package org.apache.hadoop.hive.metastore;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.api.InvalidObjectException;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.hive.metastore.api.Node;
import org.apache.hadoop.hive.metastore.model.MNode;
import org.apache.hadoop.util.ReflectionUtils;

public class DiskManager {
    public RawStore rs;
    public Log LOG;
    private final HiveConf hiveConf;
    public final int bsize = 64 * 1024;
    public DatagramSocket server;
    private DMThread dmt;
    public boolean safeMode = true;
    private final Timer timer = new Timer("checker");
    private final DMTimerTask dmtt = new DMTimerTask();

    public class DeviceInfo {
      public String dev; // dev name
      public String mp; // mount point
      public long read_nr;
      public long write_nr;
      public long err_nr;
      public long used;
      public long free;
    }

    public class NodeInfo {
      public long lastRptTs;
      List<DeviceInfo> dis;

      public NodeInfo(List<DeviceInfo> dis) {
        this.lastRptTs = System.currentTimeMillis();
        this.dis = dis;
      }
    }

    // Node -> Device Map
    private final Map<String, NodeInfo> ndmap;

    public class DMTimerTask extends TimerTask {
      private int times = 0;
      public static final long timeout = 60000; //in millisecond

      @Override
      public void run() {
        times++;
        // iterate the map, and invalidate the Node entry
        List<String> toInvalidate = new ArrayList<String>();

        for (Map.Entry<String, NodeInfo> entry : ndmap.entrySet()) {
          if (entry.getValue().lastRptTs + timeout < System.currentTimeMillis()) {
            // invalid this entry
            LOG.info("TIMES[" + times + "] " + "Invalidate Entry '" + entry.getKey() + "' for timeout.");
            toInvalidate.add(entry.getKey());
          } else {
            LOG.info("TIMES[" + times + "] " + "Validate   Entry '" + entry.getKey() + "'.");
          }
        }

        for (String node : toInvalidate) {
          removeFromNDMapWTO(node, System.currentTimeMillis());
        }
      }
    }

    public DiskManager(HiveConf conf, Log LOG) throws IOException, MetaException {
      this.hiveConf = conf;
      this.LOG = LOG;
      String rawStoreClassName = hiveConf.getVar(HiveConf.ConfVars.METASTORE_RAW_STORE_IMPL);
      Class<? extends RawStore> rawStoreClass = (Class<? extends RawStore>) MetaStoreUtils.getClass(
        rawStoreClassName);
      this.rs = (RawStore) ReflectionUtils.newInstance(rawStoreClass, conf);
      ndmap = new ConcurrentHashMap<String, NodeInfo>();
      init();
    }

    public void init() throws IOException {
      int listenPort = hiveConf.getIntVar(HiveConf.ConfVars.DISKMANAGERLISTENPORT);
      LOG.info("Starting DiskManager on port " + listenPort);
      server = new DatagramSocket(listenPort);
      dmt = new DMThread("DiskManagerThread");
      timer.schedule(dmtt, 0, 20000);
    }

    // Return old devs
    public NodeInfo addToNDMap(Node node, List<DeviceInfo> ndi) {
      // flush to database
      for (DeviceInfo di : ndi) {
        try {
          rs.createOrUpdateDevice(di, node);
        } catch (InvalidObjectException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        } catch (MetaException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
      NodeInfo ni = new NodeInfo(ndi);

      NodeInfo rni = ndmap.put(node.getNode_name(), ni);

      // check if we can leave safe mode
      try {
        if (safeMode && ((double) ndmap.size() / (double)rs.countNode() > 0.99)) {
          LOG.info(ndmap.size() + ", " + rs.countNode());
          safeMode = false;
        }
      } catch (MetaException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      return rni;
    }

    public NodeInfo removeFromNDMap(Node node) {
      return ndmap.remove(node.getNode_name());
    }

    public NodeInfo removeFromNDMapWTO(String node, long cts) {
      if (ndmap.get(node).lastRptTs + DMTimerTask.timeout < cts) {
        return ndmap.remove(node);
      }
      return null;
    }

    public String findBestNode() throws IOException {
      if (safeMode) {
        throw new IOException("Disk Manager is in Safe Mode, waiting for disk reports ...\n");
      }
      int largest = 0;
      String largestNode = null;

      for (Map.Entry<String, NodeInfo> entry : ndmap.entrySet()) {
        List<DeviceInfo> dis = entry.getValue().dis;
        int thisfree = 0;
        for (DeviceInfo di : dis) {
          thisfree += di.free;
        }
        if (thisfree > largest) {
          largestNode = entry.getKey();
          largest = thisfree;
        }
      }

      return largestNode;
    }

    public List<DeviceInfo> findDevices(String node) throws IOException {
      if (safeMode) {
        throw new IOException("Disk Manager is in Safe Mode, waiting for disk reports ...\n");
      }
      NodeInfo ni = ndmap.get(node);
      if (ni == null) {
        return null;
      } else {
        return ni.dis;
      }
    }

    public String findBestDevice(String node) throws IOException {
      if (safeMode) {
        throw new IOException("Disk Manager is in Safe Mode, waiting for disk reports ...\n");
      }
      NodeInfo ni = ndmap.get(node);
      if (ni == null) {
        throw new IOException("Node '" + node + "' does not exist in NDMap ...\n");
      }
      List<DeviceInfo> dilist = ni.dis;
      String bestDev = null;
      long free = 0;

      for (DeviceInfo di : dilist) {
        if (di.free > free) {
          bestDev = di.dev;
        }
      }

      return bestDev;
    }

    public class DMThread implements Runnable {
      Thread runner;
      public DMThread(String threadName) {
        runner = new Thread(this, threadName);
        runner.start();
      }

      // report format:
      // dev-id:mount_path,readnr,writenr,errnr,usedB,freeB\n
      public List<DeviceInfo> parseDevices(String report) {
        List<DeviceInfo> dilist = new ArrayList<DeviceInfo>();
        String lines[];

        if (report == null) {
          return null;
        }

        lines = report.split("\n");
        for (int i = 0; i < lines.length; i++) {
          String kv[] = lines[i].split(":");
          if (kv == null || kv.length < 2) {
            LOG.warn("Invalid report line: " + lines[i]);
            continue;
          }
          DeviceInfo di = new DeviceInfo();
          di.dev = kv[0];
          String stats[] = kv[1].split(",");
          if (stats == null || stats.length < 6) {
            LOG.warn("Invalid report line value: " + lines[i]);
            continue;
          }
          di.mp = stats[0];
          di.read_nr = Long.parseLong(stats[1]);
          di.write_nr = Long.parseLong(stats[2]);
          di.err_nr = Long.parseLong(stats[3]);
          di.used = Long.parseLong(stats[4]);
          di.free = Long.parseLong(stats[5]);

          dilist.add(di);
        }

        if (dilist.size() > 0) {
          return dilist;
        } else {
          return null;
        }
      }

      @Override
      public void run() {
        while (true) {
          byte[] recvBuf = new byte[bsize];
          DatagramPacket recvPacket = new DatagramPacket(recvBuf , recvBuf.length);
          try {
            server.receive(recvPacket);
          } catch (IOException e) {
            e.printStackTrace();
            continue;
          }
          String recvStr = new String(recvPacket.getData() , 0 , recvPacket.getLength());
          LOG.info("RECV: " + recvStr);

          InetAddress addr = recvPacket.getAddress();
          Node reportNode;
          try {
            reportNode = rs.findNode(addr.getHostAddress());
          } catch (MetaException e1) {
            e1.printStackTrace();
            reportNode = null;
          }
          String sendStr = "+OK";

          if (reportNode == null) {
            LOG.error("Failed to find Node: " + addr.getHostAddress());
            sendStr = "+FAIL";
          } else {
            switch (reportNode.getStatus()) {
            default:
            case MNode.NodeStatus.ONLINE:
              break;
            case MNode.NodeStatus.SUSPECT:
              try {
                reportNode.setStatus(MNode.NodeStatus.ONLINE);
                rs.updateNode(reportNode);
              } catch (MetaException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
              }
              break;
            case MNode.NodeStatus.OFFLINE:
              LOG.warn("OFFLINE node '" + reportNode.getNode_name() + "' do report!");
              break;
            }
            // parse report str
            List<DeviceInfo> dilist = parseDevices(recvStr);
            if (dilist == null) {
              // remove from the map
              removeFromNDMap(reportNode);
            } else {
              // update the map
              addToNDMap(reportNode, dilist);
            }
          }
          // send back the reply
          int port = recvPacket.getPort();
          byte[] sendBuf;
          sendBuf = sendStr.getBytes();
          DatagramPacket sendPacket
          = new DatagramPacket(sendBuf , sendBuf.length , addr , port );
          try {
            server.send(sendPacket);
          } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
      }
    }
}
