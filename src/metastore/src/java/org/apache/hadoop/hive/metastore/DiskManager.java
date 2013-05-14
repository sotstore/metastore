package org.apache.hadoop.hive.metastore;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.logging.Log;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.api.InvalidObjectException;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.hive.metastore.api.Node;
import org.apache.hadoop.hive.metastore.api.SFile;
import org.apache.hadoop.hive.metastore.api.SFileLocation;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.hadoop.hive.metastore.model.MNode;
import org.apache.hadoop.util.ReflectionUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class DiskManager {
    public RawStore rs;
    public Log LOG;
    private final HiveConf hiveConf;
    public final int bsize = 64 * 1024;
    public DatagramSocket server;
    private DMThread dmt;
    private DMCleanThread dmct;
    private DMRepThread dmrt;
    public boolean safeMode = true;
    private final Timer timer = new Timer("checker");
    private final DMTimerTask dmtt = new DMTimerTask();
    public final Queue<DMRequest> cleanQ = new ConcurrentLinkedQueue<DMRequest>();
    public final Queue<DMRequest> repQ = new ConcurrentLinkedQueue<DMRequest>();

    public static class DMReply {
      public enum DMReplyType {
        DELETED, REPLICATED,
      }
      DMReplyType type;
      String args;
    }

    public static class DMRequest {
      public enum DMROperation {
        REPLICATE, RM_PHYSICAL,
      }
      SFile file;
      DMROperation op;
      public DMRequest(SFile f, DMROperation o) {
        file = f;
        op = o;
      }
    }

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
      List<SFileLocation> toDelete;
      List<JSONObject> toRep;

      public NodeInfo(List<DeviceInfo> dis) {
        this.lastRptTs = System.currentTimeMillis();
        this.dis = dis;
        this.toDelete = Collections.synchronizedList(new ArrayList<SFileLocation>());
        this.toRep = Collections.synchronizedList(new ArrayList<JSONObject>());
      }

      public String getMP(String devid) {
        for (DeviceInfo di : dis) {
          if (di.dev.equals(devid)) {
            return di.mp;
          }
        }
        return null;
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
      dmct = new DMCleanThread("DiskManagerCleanThread");
      dmrt = new DMRepThread("DiskManagerRepThread");
      timer.schedule(dmtt, 0, 20000);
    }

    // Return old devs
    public NodeInfo addToNDMap(Node node, List<DeviceInfo> ndi) {
      // flush to database
      for (DeviceInfo di : ndi) {
        try {
          synchronized (rs) {
            rs.createOrUpdateDevice(di, node);
          }
        } catch (InvalidObjectException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        } catch (MetaException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
      NodeInfo ni = ndmap.get(node.getNode_name());
      if (ni == null) {
        ni = new NodeInfo(ndi);
        ni = ndmap.put(node.getNode_name(), ni);
      }

      // check if we can leave safe mode
      try {
        synchronized (rs) {
          if (safeMode && ((double) ndmap.size() / (double) rs.countNode() > 0.99)) {
            double cn = (double) rs.countNode();

            LOG.info("Nodemap size: " + ndmap.size() + ", saved size: " + rs.countNode() + ", reach "
                +
                (double) ndmap.size() / (double) cn * 100 + "%, leave SafeMode.");
            safeMode = false;
          }
        }
      } catch (MetaException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      return ni;
    }

    public NodeInfo removeFromNDMap(Node node) {
      return ndmap.remove(node.getNode_name());
    }

    public NodeInfo removeFromNDMapWTO(String node, long cts) {
      NodeInfo ni = ndmap.get(node);

      if (ni.lastRptTs + DMTimerTask.timeout < cts) {
        if (ni.toDelete.size() == 0 && ni.toRep.size() == 0) {
          ni = ndmap.remove(node);
        }
      }
      try {
        synchronized (rs) {
          if ((double)ndmap.size() / (double)rs.countNode() <= 0.99) {
            safeMode = true;
            LOG.info("Lost too many Nodes, enter into SafeMode now.");
          }
        }
      } catch (MetaException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      return ni;
    }

    public String findBestNode() throws IOException {
      if (safeMode) {
        throw new IOException("Disk Manager is in Safe Mode, waiting for disk reports ...\n");
      }
      long largest = 0;
      String largestNode = null;

      for (Map.Entry<String, NodeInfo> entry : ndmap.entrySet()) {
        List<DeviceInfo> dis = entry.getValue().dis;
        long thisfree = 0;
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

    public class DMCleanThread implements Runnable {
      Thread runner;
      public DMCleanThread(String threadName) {
        runner = new Thread(this, threadName);
        runner.start();
      }

      public void run() {
        while (true) {
          // dequeue requests from the clean queue
          DMRequest r = cleanQ.poll();
          if (r == null) {
            try {
              synchronized (cleanQ) {
                cleanQ.wait();
              }
            } catch (InterruptedException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
            continue;
          }
          if (r.op == DMRequest.DMROperation.RM_PHYSICAL) {
            for (SFileLocation loc : r.file.getLocations()) {
              NodeInfo ni = ndmap.get(loc.getNode_name());
              if (ni == null) {
                // add back to cleanQ
                synchronized (cleanQ) {
                  cleanQ.add(r);
                }
                continue;
              }
              synchronized (ni.toDelete) {
                ni.toDelete.add(loc);
              }
            }
          }
        }
      }
    }

    public class DMRepThread implements Runnable {
      Thread runner;

      public DMRepThread(String threadName) {
        runner = new Thread(this, threadName);
        runner.start();
      }

      public void run() {
        while (true) {
          // dequeue requests from the rep queue
          DMRequest r = repQ.poll();
          if (r == null) {
            try {
              synchronized (repQ) {
                repQ.wait();
              }
            } catch (InterruptedException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
            continue;
          }
          if (r.op == DMRequest.DMROperation.REPLICATE) {
            // allocate new file locations
            for (int i = 1; i < r.file.getRep_nr(); i++) {
              try {
                String node_name = findBestNode();
                if (node_name == null) {
                  // insert back to the queue;
                  synchronized (repQ) {
                    repQ.add(r);
                    repQ.notify();
                  }
                  break;
                }
                String devid = findBestDevice(node_name);
                String location = "/data/";
                Random rand = new Random();

                if (r.file.getPlacement() > 0) {
                  synchronized (rs) {
                    Table t = rs.getTableByID(r.file.getPlacement());
                    location += t.getDbName() + "/" + t.getTableName() + "/"
                        + rand.nextInt(Integer.MAX_VALUE);
                  }
                } else {
                  location += "UNNAMED-TABLE/" + rand.nextInt(Integer.MAX_VALUE);
                }
                SFileLocation nloc = new SFileLocation(node_name, r.file.getFid(), devid, location,
                    i, System.currentTimeMillis(),
                    MetaStoreConst.MFileLocationVisitStatus.OFFLINE, "SFL_REP_DEFAULT");
                synchronized (rs) {
                  rs.createFileLocation(nloc);
                }
                r.file.addToLocations(nloc);

                // indicate file transfer
                JSONObject jo = new JSONObject();
                try {
                  JSONObject j = new JSONObject();
                  NodeInfo ni = ndmap.get(r.file.getLocations().get(0).getNode_name());

                  if (ni == null) {
                    LOG.error("Can not find Node '" + node_name + "' in nodemap now, is it offline?");
                  }
                  j.put("node_name", r.file.getLocations().get(0).getNode_name());
                  j.put("devid", r.file.getLocations().get(0).getDevid());
                  j.put("mp", ni.getMP(r.file.getLocations().get(0).getDevid()));
                  j.put("location", r.file.getLocations().get(0).getLocation());
                  jo.put("from", j);

                  j = new JSONObject();
                  ni = ndmap.get(r.file.getLocations().get(i).getNode_name());
                  if (ni == null) {
                    LOG.error("Can not find Node '" + node_name + "' in nodemap now, is it offline?");
                  }
                  j.put("node_name", r.file.getLocations().get(i).getNode_name());
                  j.put("devid", r.file.getLocations().get(i).getDevid());
                  j.put("mp", ni.getMP(r.file.getLocations().get(i).getDevid()));
                  j.put("location", r.file.getLocations().get(i).getLocation());
                  jo.put("to", j);
                } catch (JSONException e) {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
                }
                NodeInfo ni = ndmap.get(node_name);
                if (ni == null) {
                  LOG.error("Can not find Node '" + node_name + "' in nodemap now, is it offline?");
                } else {
                  synchronized (ni.toRep) {
                    ni.toRep.add(jo);
                  }
                }
              } catch (IOException e) {
                e.printStackTrace();
                // insert back to the queue;
                synchronized (repQ) {
                  repQ.add(r);
                  repQ.notify();
                }
              } catch (MetaException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
              } catch (InvalidObjectException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
              }
            }
          }
        }
      }
    }

    public class DMThread implements Runnable {
      Thread runner;
      public DMThread(String threadName) {
        runner = new Thread(this, threadName);
        runner.start();
      }

      public class DMReport {
        // Report Format:
        // +node:node_name
        // DEVMAPS
        // +CMD
        // +DEL:node,devid,location
        // +DEL:node,devid,location
        // ...
        // +REP:node,devid,location
        // +REP:node,devid,location
        // ...
        public String node = null;
        public List<DeviceInfo> dil = null;
        public List<DMReply> replies = null;
      }

      public DMReport parseReport(String recv) {
        DMReport r = new DMReport();
        String[] reports = recv.split("\\+CMD\n");

        switch (reports.length) {
        case 1:
          // only DEVMAPS
          r.node = reports[0].substring(0, reports[0].indexOf('\n')).replaceFirst("\\+node:", "");
          r.dil = parseDevices(reports[0].substring(reports[0].indexOf('\n') + 1));
          break;
        case 2:
          // contains CMDS
          r.node = reports[0].substring(0, reports[0].indexOf('\n')).replaceFirst("\\+node:", "");
          r.dil = parseDevices(reports[0].substring(reports[0].indexOf('\n') + 1));
          r.replies = parseCmds(reports[1]);
          break;
        default:
          LOG.error("parseReport '" + recv + "' error.");
          r = null;
        }
        LOG.info("----node----->" + r.node);
        for (DeviceInfo di : r.dil) {
          LOG.info("----DI------>" + di.dev + "," + di.mp + "," + di.used + "," + di.free);
        }

        return r;
      }

      List<DMReply> parseCmds(String cmdStr) {
        List<DMReply> r = new ArrayList<DMReply>();
        String[] cmds = cmdStr.split("\n");

        for (int i = 0; i < cmds.length; i++) {
          if (cmds[i].startsWith("+REP:")) {
            DMReply dmr = new DMReply();
            dmr.type = DMReply.DMReplyType.REPLICATED;
            dmr.args = cmds[i].substring(5);
            r.add(dmr);
          } else if (cmds[i].startsWith("+DEL:")) {
            DMReply dmr = new DMReply();
            dmr.type = DMReply.DMReplyType.DELETED;
            dmr.args = cmds[i].substring(5);
            r.add(dmr);
          }
        }

        return r;
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

          DMReport report = parseReport(recvStr);

          if (report == null) {
            LOG.error("Invalid report from address: " + recvPacket.getAddress().getHostAddress());
            continue;
          }
          Node reportNode = null;

          if (report.node == null) {
            try {
              synchronized (rs) {
                reportNode = rs.findNode(recvPacket.getAddress().getHostAddress());
              }
            } catch (MetaException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
          } else {
            try {
              synchronized (rs) {
                reportNode = rs.getNode(report.node);
              }
            } catch (MetaException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
          }

          String sendStr = "+OK\n";

          if (reportNode == null) {
            String errStr = "Failed to find Node: " + report.node + ", IP=" + recvPacket.getAddress().getHostAddress();
            LOG.warn(errStr);
            // try to use "+NODE:node_name" to find
            sendStr = "+FAIL\n";
            sendStr += "+COMMENT:" + errStr;
          }

          if (reportNode != null) {
            // 1. update Node status
            switch (reportNode.getStatus()) {
            default:
            case MNode.NodeStatus.ONLINE:
              break;
            case MNode.NodeStatus.SUSPECT:
              try {
                reportNode.setStatus(MNode.NodeStatus.ONLINE);
                synchronized (rs) {
                  rs.updateNode(reportNode);
                }
              } catch (MetaException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
              }
              break;
            case MNode.NodeStatus.OFFLINE:
              LOG.warn("OFFLINE node '" + reportNode.getNode_name() + "' do report!");
              break;
            }
            LOG.info("--------> 1");

            // 2. update NDMap
            if (report.dil == null) {
              // remove from the map
              removeFromNDMap(reportNode);
            } else {
              // update the map
              addToNDMap(reportNode, report.dil);
            }
            LOG.info("--------> 2");

            // 2.NA update metadata
            Set<SFile> toCheckRep = new HashSet<SFile>();
            Set<SFile> toCheckDel = new HashSet<SFile>();
            if (report.replies != null) {
              for (DMReply r : report.replies) {
                String[] args = r.args.split(",");
                switch (r.type) {
                case REPLICATED:
                  if (args.length < 3) {
                    LOG.warn("Invalid REP report: " + r.args);
                  } else {
                    try {
                      synchronized (rs) {
                        SFileLocation newsfl = rs.getSFileLocation(args[0], args[1], args[2]);
                        SFile file = rs.getSFile(newsfl.getFid());
                        toCheckRep.add(file);
                        newsfl.setVisit_status(MetaStoreConst.MFileLocationVisitStatus.ONLINE);
                        newsfl.setDigest(file.getDigest());
                        rs.updateSFileLocation(newsfl);
                      }
                    } catch (MetaException e) {
                      e.printStackTrace();
                    }
                  }
                  break;
                case DELETED:
                  if (args.length < 3) {
                    LOG.warn("Invalid DEL report: " + r.args);
                  } else {
                    try {
                      LOG.warn("Begin delete FLoc " + args[0] + "," + args[1] + "," + args[2]);
                      synchronized (rs) {
                        SFileLocation sfl = rs.getSFileLocation(args[0], args[1], args[2]);
                        SFile file = rs.getSFile(sfl.getFid());
                        toCheckDel.add(file);
                        rs.delSFileLocation(args[0], args[1], args[2]);
                      }
                    } catch (MetaException e) {
                      e.printStackTrace();
                    }
                  }
                  break;
                default:
                  LOG.warn("Invalid DMReply type: " + r.type);
                }
              }
            }
            LOG.info("--------> 3");
            if (!toCheckRep.isEmpty()) {
              for (SFile f : toCheckRep) {
                try {
                  synchronized (rs) {
                    List<SFileLocation> sfl = rs.getSFileLocations(f.getFid());
                    int repnr = 0;
                    for (SFileLocation fl : sfl) {
                      if (fl.getVisit_status() == MetaStoreConst.MFileLocationVisitStatus.ONLINE) {
                        repnr++;
                      }
                    }
                    if (f.getRep_nr() == repnr && f.getStore_status() == MetaStoreConst.MFileStoreStatus.CLOSED) {
                      f.setStore_status(MetaStoreConst.MFileStoreStatus.REPLICATED);
                      rs.updateSFile(f);
                    }
                  }
                } catch (MetaException e) {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
                }
              }
              toCheckRep.clear();
            }
            LOG.info("--------> 4");
            if (!toCheckDel.isEmpty()) {
              for (SFile f : toCheckDel) {
                try {
                  synchronized (rs) {
                    List<SFileLocation> sfl = rs.getSFileLocations(f.getFid());
                    if (sfl.size() == 0) {
                      // delete this file
                      rs.delSFile(f.getFid());
                    }
                  }
                } catch (MetaException e) {
                  e.printStackTrace();
                }
              }
              toCheckDel.clear();
            }

            // 3. append any commands
            LOG.info("--------> 5");
            NodeInfo ni = ndmap.get(reportNode.getNode_name());
            LOG.info("--------> 5.5");
            if (ni != null && ni.toDelete.size() > 0) {
              LOG.info("--------> 5.6 " + ni.toDelete.size());
              synchronized (ni.toDelete) {
                LOG.info("--------> 5.7");
                for (SFileLocation loc : ni.toDelete) {
                  sendStr += "+DEL:" + loc.getNode_name() + ":" + loc.getDevid() + ":" +
                      ndmap.get(loc.getNode_name()).getMP(loc.getDevid()) + ":" +
                      loc.getLocation() + "\n";
                  LOG.info("+DEL:" + loc.getNode_name() + ":" + loc.getDevid() + ":" + loc.getLocation());
                }
                ni.toDelete.clear();
              }
            }
            LOG.info("--------> 6");
            if (ni != null && ni.toRep.size() > 0) {
              synchronized (ni.toRep) {
                for (JSONObject jo : ni.toRep) {
                  sendStr += "+REP:" + jo.toString() + "\n";
                }
                ni.toRep.clear();
              }
            }
          }

          // send back the reply
          LOG.info("--------> 7");
          int port = recvPacket.getPort();
          byte[] sendBuf;
          sendBuf = sendStr.getBytes();
          DatagramPacket sendPacket = new DatagramPacket(sendBuf , sendBuf.length ,
              recvPacket.getAddress() , port );
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
