package org.apache.hadoop.hive.metastore;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.apache.commons.logging.Log;
import org.apache.hadoop.hive.conf.HiveConf;

public class DiskManager {
    public Log LOG;
    private final HiveConf hiveConf;
    public final int bsize = 64 * 1024;
    public DatagramSocket server;
    private DMThread dmt;

    public DiskManager(HiveConf conf, Log LOG) throws IOException {
      hiveConf = conf;
      this.LOG = LOG;
      init();
    }

    public void init() throws IOException {
      int listenPort = hiveConf.getIntVar(HiveConf.ConfVars.DISKMANAGERLISTENPORT);
      LOG.info("Starting DiskManager on port " + listenPort);
      server = new DatagramSocket(listenPort);
      dmt = new DMThread("DiskManagerThread");
    }

    public class DMThread implements Runnable {
      Thread runner;
      public DMThread(String threadName) {
        runner = new Thread(this, threadName);
        runner.start();
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
          int port = recvPacket.getPort();
          InetAddress addr = recvPacket.getAddress();
          String sendStr = "Hello ! I'm Server";
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
