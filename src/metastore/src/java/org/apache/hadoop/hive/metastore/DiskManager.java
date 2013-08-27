package org.apache.hadoop.hive.metastore;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStore.HMSHandler;
import org.apache.hadoop.hive.metastore.api.Datacenter;
import org.apache.hadoop.hive.metastore.api.Device;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.InvalidObjectException;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.hive.metastore.api.NoSuchObjectException;
import org.apache.hadoop.hive.metastore.api.Node;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.SFile;
import org.apache.hadoop.hive.metastore.api.SFileLocation;
import org.apache.hadoop.hive.metastore.api.Subpartition;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.hadoop.hive.metastore.tools.PartitionFactory.PartitionInfo;
import org.apache.hadoop.util.ReflectionUtils;
import org.apache.thrift.TException;

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
    private final Timer bktimer = new Timer("backuper");
    private final DMTimerTask dmtt = new DMTimerTask();
    private final BackupTimerTask bktt = new BackupTimerTask();
    public final Queue<DMRequest> cleanQ = new ConcurrentLinkedQueue<DMRequest>();
    public final Queue<DMRequest> repQ = new ConcurrentLinkedQueue<DMRequest>();
    public final Queue<BackupEntry> backupQ = new ConcurrentLinkedQueue<BackupEntry>();
    public final Map<String, Long> toReRep = new ConcurrentHashMap<String, Long>();
    public final Map<String, Long> toUnspc = new ConcurrentHashMap<String, Long>();
    public final Map<String, MigrateEntry> rrmap = new ConcurrentHashMap<String, MigrateEntry>();

    // TODO: fix me: change it to 30 min
    public long backupTimeout = 1 * 60 * 1000;
    public long backupQTimeout = 5 * 3600 * 1000; // 5 hour
    public long fileSizeThreshold = 64 * 1024 * 1024;
    public Set<String> backupDevs = new TreeSet<String>();

    public static class SFLTriple implements Comparable<SFLTriple> {
      public String node;
      public String devid;
      public String location;

      public SFLTriple(String node, String devid, String location) {
        this.node = node;
        this.devid = devid;
        this.location = location;
      }

      @Override
      public int compareTo(SFLTriple b) {
        return node.compareTo(b.node) & devid.compareTo(b.devid) & location.compareTo(b.location);
      }

      @Override
      public String toString() {
        return "N:" + node + ",D:" + devid + ",L:" + location;
      }
    }

    public static class MigrateEntry {
      boolean is_part;
      String to_dc;
      public Partition part;
      public Subpartition subpart;
      public List<Long> files;
      public Map<String, Long> timap;

      public MigrateEntry(String to_dc, Partition part, List<Long> files, Map<String, Long> timap) {
        this.is_part = true;
        this.to_dc = to_dc;
        this.part = part;
        this.files = files;
        this.timap = timap;
      }

      public MigrateEntry(String to_dc, Subpartition subpart, List<Long> files, Map<String, Long> timap) {
        this.is_part = false;
        this.to_dc = to_dc;
        this.subpart = subpart;
        this.files = files;
        this.timap = timap;
      }

      @Override
      public String toString() {
        String r;
        if (is_part) {
          r = "Part    :" + part.getPartitionName() + ",files:" + files.toString();
        } else {
          r = "Subpart :" + subpart.getPartitionName() + ",files:" + files.toString();
        }
        return r;
      }
    }
    public static class BackupEntry {
      public enum FOP {
        ADD_PART, DROP_PART, ADD_SUBPART, DROP_SUBPART,
      }
      public Partition part;
      public Subpartition subpart;
      public List<SFile> files;
      public FOP op;
      public long ttl;

      public BackupEntry(Partition part, List<SFile> files, FOP op) {
        this.part = part;
        this.files = files;
        this.op = op;
        this.ttl = System.currentTimeMillis();
      }
      public BackupEntry(Subpartition subpart, List<SFile> files, FOP op) {
        this.subpart = subpart;
        this.files = files;
        this.op = op;
        this.ttl = System.currentTimeMillis();
      }
      @Override
      public String toString() {
        String r;

        switch (op) {
        case ADD_PART:
          r = "ADD  PART: " + part.getPartitionName() + ",files:" + files.toString();
          break;
        case DROP_PART:
          r = "DROP PART: " + part.getPartitionName() + ",files:" + files.toString();
          break;
        case ADD_SUBPART:
          r = "ADD  SUBPART: " + subpart.getPartitionName() + ",files:" + files.toString();
          break;
        case DROP_SUBPART:
          r = "DROP SUBPART: " + subpart.getPartitionName() + ",files:" + files.toString();
          break;
        default:
          r = "BackupEntry: INVALID OP!";
        }
        return r;
      }
    }

    public static class FileToPart {
      public boolean isPart;
      public SFile file;
      public Partition part;
      public Subpartition subpart;

      public FileToPart(SFile file, Partition part) {
        this.isPart = true;
        this.file = file;
        this.part = part;
      }
      public FileToPart(SFile file, Subpartition subpart) {
        this.isPart = false;
        this.file = file;
        this.subpart = subpart;
      }
    }

    public static class DMReply {
      public enum DMReplyType {
        DELETED, REPLICATED,
      }
      DMReplyType type;
      String args;
    }

    public static class DMRequest {
      public enum DMROperation {
        REPLICATE, RM_PHYSICAL, MIGRATE,
      }
      SFile file;
      SFile tfile; // target file, only valid when op is MIGRATE
      Map<String, String> devmap;
      DMROperation op;
      String to_dc;
      int begin_idx;

      public DMRequest(SFile f, DMROperation o, int idx) {
        file = f;
        op = o;
        begin_idx = idx;
      }

      public DMRequest(SFile source, SFile target, Map<String, String> devmap, String to_dc) {
        this.file = source;
        this.tfile = target;
        this.devmap = devmap;
        this.to_dc = to_dc;
        op = DMROperation.MIGRATE;
      }

      @Override
      public String toString() {
        String r;
        switch (op) {
        case REPLICATE:
          r = "REPLICATE: file fid " + file.getFid() + " from idx " + begin_idx;
          break;
        case RM_PHYSICAL:
          r = "DELETE   : file fid " + file.getFid();
          break;
        case MIGRATE:
          r = "MIGRATE  : file fid " + file.getFid() + " to DC " + to_dc + "fid " + tfile.getFid();
          break;
        default:
          r = "DMRequest: Invalid OP!";
        }
        return r;
      }
    }

    public static class DeviceInfo implements Comparable<DeviceInfo> {
      public String dev; // dev name
      public String mp; // mount point
      public int prop;
      public long read_nr;
      public long write_nr;
      public long err_nr;
      public long used;
      public long free;
      @Override
      public int compareTo(DeviceInfo o) {
        return this.dev.compareTo(o.dev);
      }
    }

    public class NodeInfo {
      public long lastRptTs;
      List<DeviceInfo> dis;
      Set<SFileLocation> toDelete;
      List<JSONObject> toRep;

      public NodeInfo(List<DeviceInfo> dis) {
        this.lastRptTs = System.currentTimeMillis();
        this.dis = dis;
        this.toDelete = Collections.synchronizedSet(new TreeSet<SFileLocation>());
        this.toRep = Collections.synchronizedList(new ArrayList<JSONObject>());
      }

      public String getMP(String devid) {
        synchronized (this) {
          if (dis == null) {
            return null;
          }
          for (DeviceInfo di : dis) {
            if (di.dev.equals(devid)) {
              return di.mp;
            }
          }
          return null;
        }
      }
    }

    // Node -> Device Map
    private final Map<String, NodeInfo> ndmap;

    public class BackupTimerTask extends TimerTask {
      private long last_backupTs = System.currentTimeMillis();

      public boolean generateSyncFiles(Set<Partition> parts, Set<Subpartition> subparts, Set<FileToPart> toAdd, Set<FileToPart> toDrop) {
        Date d = new Date(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
        File dir = new File(System.getProperty("user.dir") + "/backup/sync-" + sdf.format(d));
        if (!dir.mkdirs()) {
          LOG.error("Make directory " + dir.getPath() + " failed, can't write sync meta files.");
          return false;
        }
        // generate tableName.desc files
        Set<Table> tables = new TreeSet<Table>();
        Map<String, Table> partToTbl = new HashMap<String, Table>();
        for (Partition p : parts) {
          synchronized (rs) {
            Table t;
            try {
              t = rs.getTable(p.getDbName(), p.getTableName());
              tables.add(t);
              partToTbl.put(p.getPartitionName(), t);
            } catch (MetaException e) {
              LOG.error(e, e);
              return false;
            }
          }
        }
        for (Subpartition p : subparts) {
          synchronized (rs) {
            Table t;
            try {
              t = rs.getTable(p.getDbName(), p.getTableName());
              tables.add(t);
              partToTbl.put(p.getPartitionName(), t);
            } catch (MetaException e) {
              LOG.error(e, e);
              return false;
            }
          }
        }
        for (Table t : tables) {
          File f = new File(dir, t.getDbName() + ":" + t.getTableName() + ".desc");
          try {
            if (!f.exists()) {
              f.createNewFile();
            }
            String content = "[fieldInfo]\n";
            for (FieldSchema fs : t.getSd().getCols()) {
              content += fs.getName() + "\t" + fs.getType() + "\n";
            }
            content += "[partitionInfo]\n";
            List<PartitionInfo> pis = PartitionInfo.getPartitionInfo(t.getPartitionKeys());
            for (PartitionInfo pi : pis) {
              content += pi.getP_col() + "\t" + pi.getP_type().getName() + "\t" + pi.getArgs().toString() + "\n";
            }
            FileWriter fw = new FileWriter(f.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.close();
          } catch (IOException e) {
            LOG.error(e, e);
            return false;
          }
        }
        File f = new File(dir, "manifest.desc");
        if (!f.exists()) {
          try {
            f.createNewFile();
          } catch (IOException e) {
            LOG.error(e, e);
            return false;
          }
        }
        String content = "";
        for (FileToPart ftp : toAdd) {
          for (SFileLocation sfl : ftp.file.getLocations()) {
            Device device = null;
            synchronized (rs) {
              try {
                device = rs.getDevice(sfl.getDevid());
              } catch (MetaException e) {
                LOG.error(e, e);
              } catch (NoSuchObjectException e) {
                LOG.error(e, e);
              }
            }
            if (device != null && (device.getProp() == MetaStoreConst.MDeviceProp.BACKUP)) {
              content += sfl.getLocation().substring(sfl.getLocation().lastIndexOf('/') + 1);
              if (ftp.isPart) {
                content += "\tADD\t" + ftp.part.getDbName() + "\t" + ftp.part.getTableName() + "\t";
                for (int i = 0; i < ftp.part.getValuesSize(); i++) {
                  Table t = partToTbl.get(ftp.part.getPartitionName());
                  List<PartitionInfo> pis = PartitionInfo.getPartitionInfo(t.getPartitionKeys());
                  for (PartitionInfo pi : pis) {
                    if (pi.getP_level() == 1) {
                      content += pi.getP_col() + "=";
                      break;
                    }
                  }
                  content += ftp.part.getValues().get(i);
                  if (i < ftp.part.getValuesSize() - 1) {
                    content += ",";
                  }
                }
                content += "\n";
              } else {
                Table t = partToTbl.get(ftp.subpart.getPartitionName());
                List<PartitionInfo> pis = PartitionInfo.getPartitionInfo(t.getPartitionKeys());
                Partition pp;

                synchronized (rs) {
                  try {
                    pp = rs.getParentPartition(ftp.subpart.getDbName(), ftp.subpart.getTableName(), ftp.subpart.getPartitionName());
                  } catch (NoSuchObjectException e) {
                    LOG.error(e, e);
                    break;
                  } catch (MetaException e) {
                    LOG.error(e, e);
                    break;
                  }
                }
                content += "\tADD\t" + ftp.subpart.getDbName() + "\t" + ftp.subpart.getTableName() + "\t";
                for (PartitionInfo pi : pis) {
                  if (pi.getP_level() == 1) {
                    content += pi.getP_col() + "=";
                    for (int i = 0; i < pp.getValuesSize(); i++) {

                      content += pp.getValues().get(i);
                      if (i < pp.getValuesSize() - 1) {
                        content += ",";
                      }
                    }
                    content += "#";
                  }
                  if (pi.getP_level() == 2) {
                    content += pi.getP_col() + "=";
                    for (int i = 0; i < ftp.subpart.getValuesSize(); i++) {

                      content += ftp.subpart.getValues().get(i);
                      if (i < ftp.subpart.getValuesSize() - 1) {
                        content += ",";
                      }
                    }
                  }
                }
                content += "\n";
              }
              break;
            }
          }
        }
        for (FileToPart ftp : toDrop) {
          for (SFileLocation sfl : ftp.file.getLocations()) {
            Device device = null;
            synchronized (rs) {
              try {
                device = rs.getDevice(sfl.getDevid());
              } catch (MetaException e) {
                LOG.error(e, e);
              } catch (NoSuchObjectException e) {
                LOG.error(e, e);
              }
            }
            if (device != null && (device.getProp() == MetaStoreConst.MDeviceProp.BACKUP)) {
              content += sfl.getLocation() + "\tRemove\t" + ftp.part.getDbName() + "\t" + ftp.part.getTableName() + "\t";
              for (int i = 0; i < ftp.part.getValuesSize(); i++) {
                content += ftp.part.getValues().get(i);
                if (i < ftp.part.getValuesSize() - 1) {
                  content += "#";
                }
              }
              content += "\n";
              break;
            }
          }
        }

        try {
          FileWriter fw = new FileWriter(f.getAbsoluteFile());
          BufferedWriter bw = new BufferedWriter(fw);
          bw.write(content);
          bw.close();
        } catch (IOException e) {
          LOG.error(e, e);
          return false;
        }

        return true;
      }

      @Override
      public void run() {

        backupTimeout = hiveConf.getLongVar(HiveConf.ConfVars.DM_BACKUP_TIMEOUT);
        backupQTimeout = hiveConf.getLongVar(HiveConf.ConfVars.DM_BACKUPQ_TIMEOUT);
        fileSizeThreshold = hiveConf.getLongVar(HiveConf.ConfVars.DM_BACKUP_FILESIZE_THRESHOLD);

        if (last_backupTs + backupTimeout <= System.currentTimeMillis()) {
          // TODO: generate manifest.desc and tableName.desc
          Set<Partition> parts = new TreeSet<Partition>();
          Set<Subpartition> subparts = new TreeSet<Subpartition>();
          Set<FileToPart> toAdd = new HashSet<FileToPart>();
          Set<FileToPart> toDrop = new HashSet<FileToPart>();
          Queue<BackupEntry> localQ = new ConcurrentLinkedQueue<BackupEntry>();

          while (true) {
            BackupEntry be = null;

            synchronized (backupQ) {
              be = backupQ.poll();
            }
            if (be == null) {
              break;
            }
            // this is a valid entry, check if the file size is large enough
            if (be.op == BackupEntry.FOP.ADD_PART) {
              // refresh to check if the file is closed and has the proper length
              for (SFile f : be.files) {
                SFile nf;
                synchronized (rs) {
                  try {
                    nf = rs.getSFile(f.getFid());
                    if (nf != null) {
                      nf.setLocations(rs.getSFileLocations(f.getFid()));
                    } else {
                      LOG.error("Invalid SFile fid " + f.getFid() + ", not found.");
                      continue;
                    }
                  } catch (MetaException e) {
                    LOG.error(e, e);
                    // lately reinsert back to the queue
                    localQ.add(be);
                    break;
                  }
                }
                if (nf != null && nf.getStore_status() == MetaStoreConst.MFileStoreStatus.INCREATE) {
                  // this means we should wait a moment for the sfile
                  if (be.ttl + backupQTimeout >= System.currentTimeMillis()) {
                    localQ.add(be);
                  } else {
                    LOG.warn("This is a long opening file (fid " + nf.getFid() + "), might be void files.");
                  }
                  break;
                }
                if (nf != null && ((nf.getStore_status() == MetaStoreConst.MFileStoreStatus.CLOSED ||
                    nf.getStore_status() == MetaStoreConst.MFileStoreStatus.REPLICATED ||
                    nf.getStore_status() == MetaStoreConst.MFileStoreStatus.RM_PHYSICAL) &&
                    nf.getLength() >= fileSizeThreshold)) {
                  // add this file to manifest.desc
                  FileToPart ftp = new FileToPart(nf, be.part);
                  toAdd.add(ftp);
                  parts.add(be.part);
                } else {
                  LOG.warn("This file (fid " + nf.getFid() + " is ignored. (status " + nf.getStore_status() + ").");
                }
              }
            } else if (be.op == BackupEntry.FOP.DROP_PART) {
              for (SFile f : be.files) {
                // add this file to manifest.desc
                FileToPart ftp = new FileToPart(f, be.part);
                toDrop.add(ftp);
                parts.add(be.part);
              }
            } else if (be.op == BackupEntry.FOP.ADD_SUBPART) {
              // refresh to check if the file is closed and has the proper length
              for (SFile f : be.files) {
                SFile nf;
                synchronized (rs) {
                  try {
                    nf = rs.getSFile(f.getFid());
                    if (nf != null) {
                      nf.setLocations(rs.getSFileLocations(f.getFid()));
                    } else {
                      LOG.error("Invalid SFile fid " + f.getFid() + ", not found.");
                      continue;
                    }
                  } catch (MetaException e) {
                    LOG.error(e, e);
                    // lately reinsert back to the queue
                    localQ.add(be);
                    break;
                  }
                }
                if (nf != null && nf.getStore_status() == MetaStoreConst.MFileStoreStatus.INCREATE) {
                  // this means we should wait a moment for the sfile
                  if (be.ttl + backupQTimeout >= System.currentTimeMillis()) {
                    localQ.add(be);
                  } else {
                    LOG.warn("This is a long opening file (fid " + nf.getFid() + "), might be void files.");
                  }
                  break;
                }
                if (nf != null && ((nf.getStore_status() == MetaStoreConst.MFileStoreStatus.CLOSED ||
                    nf.getStore_status() == MetaStoreConst.MFileStoreStatus.REPLICATED ||
                    nf.getStore_status() == MetaStoreConst.MFileStoreStatus.RM_PHYSICAL) &&
                    nf.getLength() >= fileSizeThreshold)) {
                  // add this file to manifest.desc
                  FileToPart ftp = new FileToPart(nf, be.subpart);
                  toAdd.add(ftp);
                  subparts.add(be.subpart);
                } else {
                  LOG.warn("This file (fid " + nf.getFid() + " is ignored. (status " + nf.getStore_status() + ").");
                }
              }
            } else if (be.op == BackupEntry.FOP.DROP_SUBPART) {
              for (SFile f : be.files) {
                // add this file to manifest.desc
                FileToPart ftp = new FileToPart(f, be.subpart);
                toDrop.add(ftp);
                subparts.add(be.subpart);
              }
            }
          }
          toAdd.removeAll(toDrop);
          // generate final desc files
          if ((toAdd.size() + toDrop.size() > 0) && generateSyncFiles(parts, subparts, toAdd, toDrop)) {
            LOG.info("Generated SYNC dir around time " + System.currentTimeMillis() + ", toAdd " + toAdd.size() + ", toDrop " + toDrop.size());
          }
          last_backupTs = System.currentTimeMillis();
          synchronized (backupQ) {
            backupQ.addAll(localQ);
          }
        }
      }

    }

    public class DMTimerTask extends TimerTask {
      private RawStore trs;
      private int times = 0;
      private boolean isRunning = false;
      private final Long syncIsRunning = new Long(0);
      public static final long timeout = 60 * 1000; //in millisecond
      public static final long repDelCheck = 60 * 1000;
      public static final long voidFileCheck = 30 * 60 * 1000;
      public static final long voidFileTimeout = 12 * 3600 * 1000; // 12 hours
      public static final long repTimeout = 15 * 60 * 1000;
      public static final long delTimeout = 5 * 60 * 1000;
      public static final long rerepTimeout = 30 * 1000;

      private long last_repTs = System.currentTimeMillis();
      private long last_rerepTs = System.currentTimeMillis();
      private long last_unspcTs = System.currentTimeMillis();
      private long last_voidTs = System.currentTimeMillis();

      private boolean useVoidCheck = false;

      public void init(HiveConf conf) throws MetaException {
        String rawStoreClassName = hiveConf.getVar(HiveConf.ConfVars.METASTORE_RAW_STORE_IMPL);
        Class<? extends RawStore> rawStoreClass = (Class<? extends RawStore>) MetaStoreUtils.getClass(
            rawStoreClassName);
        this.trs = (RawStore) ReflectionUtils.newInstance(rawStoreClass, conf);
      }

      public void do_delete(SFile f, int nr) {
        int i = 0;

        if (nr <= 0) {
          return;
        }

        synchronized (ndmap) {
          for (SFileLocation loc : f.getLocations()) {
            if (i >= nr) {
              break;
            }
            NodeInfo ni = ndmap.get(loc.getNode_name());
            if (ni == null) {
              continue;
            }
            synchronized (ni.toDelete) {
              ni.toDelete.add(loc);
              i++;
              LOG.info("----> Add toDelete " + loc.getLocation() + ", qs " + cleanQ.size() + ", "
                  + f.getLocationsSize());
            }
          }
        }
      }

      public void do_replicate(SFile f, int nr) {
        int init_size = f.getLocationsSize();
        int valid_idx = 0;
        boolean no_valid_fl = true;
        FileLocatingPolicy flp, flp_backup, flp_default;
        Set<String> excludes = new TreeSet<String>();
        Set<String> excl_dev = new TreeSet<String>();
        Set<String> spec_dev = new TreeSet<String>();
        Set<String> spec_node = new TreeSet<String>();

        if (init_size <= 0) {
          LOG.error("Not valid locations for file " + f.getFid());
          return;
        }
        // find the backup devices
        findBackupDevice(spec_dev, spec_node);
        LOG.debug("Try to write to backup device firstly: N <" + spec_node.toArray().toString() +
            ">, D <" + spec_dev.toArray().toString() + ">");

        // find the valid entry
        for (int i = 0; i < init_size; i++) {
          excludes.add(f.getLocations().get(i).getNode_name());
          excl_dev.add(f.getLocations().get(i).getDevid());
          if (spec_dev.remove(f.getLocations().get(i).getDevid())) {
            // this backup device has already used
            spec_dev.clear();
          }
          if (f.getLocations().get(i).getVisit_status() == MetaStoreConst.MFileLocationVisitStatus.ONLINE) {
            valid_idx = i;
            no_valid_fl = false;
            break;
          }
        }
        if (no_valid_fl) {
          LOG.error("Async replicate SFile " + f.getFid() + ", but no valid FROM SFileLocations!");
          return;
        }

        flp = flp_default = new FileLocatingPolicy(excludes, excl_dev, FileLocatingPolicy.EXCLUDE_NODES_DEVS, false);
        flp_backup = new FileLocatingPolicy(spec_node, spec_dev, FileLocatingPolicy.SPECIFY_NODES_DEVS, true);

        for (int i = init_size; i < (init_size + nr); i++, flp = flp_default) {
          if (i == init_size) {
            if (spec_dev.size() > 0) {
              flp = flp_backup;
            }
          }
          try {
            String node_name = findBestNode(flp);
            if (node_name == null) {
              LOG.info("Could not find any best node to replicate file " + f.getFid());
              break;
            }
            excludes.add(node_name);
            String devid = findBestDevice(node_name, flp);
            if (devid == null) {
              LOG.info("Could not find any best device on node " + node_name + " to replicate file " + f.getFid());
              break;
            }
            excl_dev.add(devid);
            String location;
            Random rand = new Random();
            SFileLocation nloc;

            do {
              location = "/data/";
              if (f.getPlacement() > 0) {
                synchronized (trs) {
                  Table t = trs.getTableByID(f.getPlacement());
                  location += t.getDbName() + "/" + t.getTableName() + "/"
                      + rand.nextInt(Integer.MAX_VALUE);
                }
              } else {
                location += "UNNAMED-DB/UNNAMED-TABLE/" + rand.nextInt(Integer.MAX_VALUE);
              }
              nloc = new SFileLocation(node_name, f.getFid(), devid, location,
                  i, System.currentTimeMillis(),
                  MetaStoreConst.MFileLocationVisitStatus.OFFLINE, "SFL_REP_DEFAULT");
              synchronized (trs) {
                if (trs.createFileLocation(nloc)) {
                  break;
                }
              }
            } while (true);
            f.addToLocations(nloc);

            // indicate file transfer
            JSONObject jo = new JSONObject();
            try {
              JSONObject j = new JSONObject();
              NodeInfo ni = ndmap.get(f.getLocations().get(valid_idx).getNode_name());

              if (ni == null) {
                throw new IOException("Can not find Node '" + node_name + "' in nodemap now, is it offline?");
                                 }
              j.put("node_name", f.getLocations().get(valid_idx).getNode_name());
              j.put("devid", f.getLocations().get(valid_idx).getDevid());
              j.put("mp", ni.getMP(f.getLocations().get(valid_idx).getDevid()));
              j.put("location", f.getLocations().get(valid_idx).getLocation());
              jo.put("from", j);

              j = new JSONObject();
              ni = ndmap.get(f.getLocations().get(i).getNode_name());
              if (ni == null) {
                throw new IOException("Can not find Node '" + node_name + "' in nodemap now, is it offline?");
              }
              j.put("node_name", f.getLocations().get(i).getNode_name());
              j.put("devid", f.getLocations().get(i).getDevid());
              j.put("mp", ni.getMP(f.getLocations().get(i).getDevid()));
              j.put("location", f.getLocations().get(i).getLocation());
              jo.put("to", j);
            } catch (JSONException e) {
              LOG.error(e, e);
              continue;
            }
            synchronized (ndmap) {
              NodeInfo ni = ndmap.get(node_name);
              if (ni == null) {
                LOG.error("Can not find Node '" + node_name + "' in nodemap now, is it offline?");
              } else {
                synchronized (ni.toRep) {
                  ni.toRep.add(jo);
                  LOG.info("----> ADD " + node_name + "'s toRep " + jo);
                }
              }
            }
          } catch (IOException e) {
            LOG.error(e, e);
            break;
          } catch (MetaException e) {
            LOG.error(e, e);
          } catch (InvalidObjectException e) {
            LOG.error(e, e);
          }
        }
      }

      public void updateRunningState() {
        synchronized (syncIsRunning) {
          isRunning = false;
          LOG.debug("Timer task [" + times + "] done.");
        }
      }

      @Override
      public void run() {
        times++;
        useVoidCheck = hiveConf.getBoolVar(HiveConf.ConfVars.DM_USE_VOID_CHECK);

        // iterate the map, and invalidate the Node entry
        List<String> toInvalidate = new ArrayList<String>();

        for (Map.Entry<String, NodeInfo> entry : ndmap.entrySet()) {
          if (entry.getValue().lastRptTs + timeout < System.currentTimeMillis()) {
            // invalid this entry
            LOG.info("TIMES[" + times + "] " + "Invalidate Entry '" + entry.getKey() + "' for timeout.");
            toInvalidate.add(entry.getKey());
          }
        }

        for (String node : toInvalidate) {
          synchronized (ndmap) {
            removeFromNDMapWTO(node, System.currentTimeMillis());
          }
        }

        synchronized (syncIsRunning) {
          if (isRunning) {
            return;
          } else {
            isRunning = true;
          }
        }

        // check any under/over/linger files
        if (last_repTs + repDelCheck < System.currentTimeMillis()) {
          // get the file list
          List<SFile> under = new ArrayList<SFile>();
          List<SFile> over = new ArrayList<SFile>();
          List<SFile> linger = new ArrayList<SFile>();
          Map<SFile, Integer> munder = new TreeMap<SFile, Integer>();
          Map<SFile, Integer> mover = new TreeMap<SFile, Integer>();

          LOG.info("Check Under/Over Replicated or Lingering Files [" + times + "]");
          synchronized (trs) {
            try {
              trs.findFiles(under, over, linger);
            } catch (MetaException e) {
              LOG.error(e, e);
              updateRunningState();
              return;
            }
          }
          LOG.info("OK, get under " + under.size() + ", over " + over.size() + ", linger " + linger.size());
          // handle under replicated files
          for (SFile f : under) {
            // check whether we should issue a re-replicate command
            int nr = 0;

            LOG.info("check under replicated files for fid " + f.getFid());
            for (SFileLocation fl : f.getLocations()) {
              if (fl.getVisit_status() == MetaStoreConst.MFileLocationVisitStatus.ONLINE ||
                  (fl.getVisit_status() == MetaStoreConst.MFileLocationVisitStatus.OFFLINE &&
                  fl.getUpdate_time() + repTimeout > System.currentTimeMillis())) {
                nr++;
              }
            }
            if (nr < f.getRep_nr()) {
              munder.put(f, f.getRep_nr() - nr);
            }
          }

          for (Map.Entry<SFile, Integer> entry : munder.entrySet()) {
            do_replicate(entry.getKey(), entry.getValue().intValue());
          }

          // handle over replicated files
          for (SFile f : over) {
            // check whether we should issue a del command
            int nr = 0;

            LOG.info("check over replicated files for fid " + f.getFid());
            for (SFileLocation fl : f.getLocations()) {
              if (fl.getVisit_status() == MetaStoreConst.MFileLocationVisitStatus.ONLINE) {
                nr++;
              }
            }
            if (nr > f.getRep_nr()) {
              mover.put(f, nr - f.getRep_nr());
            }
          }
          for (Map.Entry<SFile, Integer> entry : mover.entrySet()) {
            do_delete(entry.getKey(), entry.getValue().intValue());
          }

          // handle lingering files
          Set<SFileLocation> s = new TreeSet<SFileLocation>();
          Set<SFile> sd = new TreeSet<SFile>();
          for (SFile f : linger) {
            LOG.info("check lingering files for fid " + f.getFid());
            if (f.getStore_status() == MetaStoreConst.MFileStoreStatus.RM_PHYSICAL) {
              sd.add(f);
              continue;
            }
            for (SFileLocation fl : f.getLocations()) {
              if (fl.getVisit_status() == MetaStoreConst.MFileLocationVisitStatus.OFFLINE) {
                s.add(fl);
              }
            }
          }
          for (SFileLocation fl : s) {
            synchronized (trs) {
              try {
                trs.delSFileLocation(fl.getDevid(), fl.getLocation());
              } catch (MetaException e) {
                LOG.error(e, e);
              }
            }
          }
          for (SFile f : sd) {
            do_delete(f, f.getLocationsSize());
          }
          last_repTs = System.currentTimeMillis();
          under.clear();
          over.clear();
          linger.clear();
        }

        // check invalid file locations on invalid devices
        if (last_rerepTs + repDelCheck < System.currentTimeMillis()) {
          for (Map.Entry<String, Long> entry : toReRep.entrySet()) {
            boolean ignore = false;
            boolean delete = true;

            if (entry.getValue() + rerepTimeout < System.currentTimeMillis()) {
              for (NodeInfo ni : ndmap.values()) {
                if (ni.dis != null && ni.dis.contains(entry.getKey())) {
                  // found it! ignore this device and remove it now
                  toReRep.remove(entry.getKey());
                  ignore = true;
                  break;
                }
              }
              if (!ignore) {
                List<SFileLocation> sfl;

                synchronized (trs) {
                  try {
                    sfl = trs.getSFileLocations(entry.getKey(), System.currentTimeMillis(), 0);
                  } catch (MetaException e) {
                    LOG.error(e, e);
                    continue;
                  }
                }
                for (SFileLocation fl : sfl) {
                  LOG.info("Change FileLocation " + fl.getDevid() + ":" + fl.getLocation() + " to SUSPECT state!");
                  synchronized (trs) {
                    fl.setVisit_status(MetaStoreConst.MFileLocationVisitStatus.SUSPECT);
                    try {
                      trs.updateSFileLocation(fl);
                    } catch (MetaException e) {
                      LOG.error(e, e);
                      delete = false;
                      continue;
                    }
                  }
                }
              }
              if (delete) {
                toReRep.remove(entry.getKey());
              }
            }
          }
          last_rerepTs = System.currentTimeMillis();
        }

        // check files on unspc devices, try to unspc it
        if (last_unspcTs + repDelCheck < System.currentTimeMillis()) {
          // Step 1: generate the SUSPECT file list
          List<SFileLocation> sfl;

          LOG.info("Check SUSPECT SFileLocations [" + times + "]");
          synchronized (trs) {
            try {
              sfl = trs.getSFileLocations(MetaStoreConst.MFileLocationVisitStatus.SUSPECT);
              // Step 2: TODO: try to probe the target file
              for (SFileLocation fl : sfl) {
                // check if this device is back
                if (toUnspc.containsKey(fl.getDevid())) {
                  fl.setVisit_status(MetaStoreConst.MFileLocationVisitStatus.ONLINE);
                  try {
                    trs.updateSFileLocation(fl);
                  } catch (MetaException e) {
                    LOG.error(e, e);
                    continue;
                  }
                }
              }
            } catch (MetaException e) {
              LOG.error(e, e);
            }
          }

          last_unspcTs = System.currentTimeMillis();
        }

        // check void files
        if (useVoidCheck && last_voidTs + voidFileCheck < System.currentTimeMillis()) {
          List<SFile> voidFiles = new ArrayList<SFile>();

          synchronized (trs) {
            try {
              trs.findVoidFiles(voidFiles);
            } catch (MetaException e) {
              LOG.error(e, e);
              updateRunningState();
              return;
            }
          }
          for (SFile f : voidFiles) {
            boolean isVoid = true;

            if (f.getLocationsSize() > 0) {
              // check file location's update time, if it has not update in last 12 hours, then it is void!
              for (SFileLocation fl : f.getLocations()) {
                if (fl.getUpdate_time() + voidFileTimeout > System.currentTimeMillis()) {
                  isVoid = false;
                  break;
                }
              }
            } else {
              // check file create time? there is no creation_time in sfile, thus do not mark it as void
              isVoid = false;
            }

            if (isVoid) {
              // ok, mark the file as deleted
              synchronized (trs) {
                LOG.info("Mark file (fid " + f.getFid() + ") as void file to physically delete.");
                f.setStore_status(MetaStoreConst.MFileStoreStatus.RM_PHYSICAL);
                try {
                  trs.updateSFile(f);
                } catch (MetaException e) {
                  LOG.error(e, e);
                }
              }
            }
          }
          last_voidTs = System.currentTimeMillis();
        }

        updateRunningState();
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

    public void init() throws IOException, MetaException {
      int listenPort = hiveConf.getIntVar(HiveConf.ConfVars.DISKMANAGERLISTENPORT);
      LOG.info("Starting DiskManager on port " + listenPort);
      server = new DatagramSocket(listenPort);
      dmt = new DMThread("DiskManagerThread");
      dmct = new DMCleanThread("DiskManagerCleanThread");
      dmrt = new DMRepThread("DiskManagerRepThread");
      dmtt.init(hiveConf);
      timer.schedule(dmtt, 0, 5000);
      bktimer.schedule(bktt, 0, 5000);
    }

    public String getAnyNode() {
      String r = null;

      synchronized (ndmap) {
        for (Map.Entry<String, NodeInfo> e : ndmap.entrySet()) {
          r = e.getKey();
          break;
        }
      }

      return r;
    }

    public List<String> getActiveNodes() throws MetaException {
      List<String> r = new ArrayList<String>();

      synchronized (ndmap) {
        for (Map.Entry<String, NodeInfo> e : ndmap.entrySet()) {
          r.add(e.getKey());
        }
      }

      return r;
    }

    public List<String> getActiveDevices() throws MetaException {
      List<String> r = new ArrayList<String>();

      synchronized (ndmap) {
        for (Map.Entry<String, NodeInfo> e : ndmap.entrySet()) {
          for (DeviceInfo di : e.getValue().dis) {
            r.add(di.dev);
          }
        }
      }

      return r;
    }

    public boolean markSFileLocationStatus(SFile toMark) throws MetaException {
      boolean marked = false;
      Set<String> activeDevs = new HashSet<String>();

      synchronized (ndmap) {
        for (Map.Entry<String, NodeInfo> e : ndmap.entrySet()) {
          for (DeviceInfo di : e.getValue().dis) {
            activeDevs.add(di.dev);
          }
        }
      }

      for (SFileLocation sfl : toMark.getLocations()) {
        if (!activeDevs.contains(sfl.getDevid()) && sfl.getVisit_status() == MetaStoreConst.MFileLocationVisitStatus.ONLINE) {
          sfl.setVisit_status(MetaStoreConst.MFileLocationVisitStatus.SUSPECT);
          marked = true;
        }
      }

      return marked;
    }

    public String getDMStatus() throws MetaException {
      String r = "";

      r += "MetaStore Server Disk Manager listening @ " + hiveConf.getIntVar(HiveConf.ConfVars.DISKMANAGERLISTENPORT);
      r += "\nSafeMode: " + safeMode + "\n";
      synchronized (rs) {
        r += "Total nodes " + rs.countNode() + ", active nodes " + ndmap.size() + "\n";
      }
      r += "Active Node-Device map: {\n";
      synchronized (ndmap) {
        for (Map.Entry<String, NodeInfo> e : ndmap.entrySet()) {
          r += " " + e.getKey() + " -> " + "[";
          if (e.getValue().dis != null) {
            for (DeviceInfo di : e.getValue().dis) {
              r += di.dev + ",";
            }
          }
          r += "]\n";
        }
      }
      r += "}\n";
      r += "Inactive nodes list: {\n";
      synchronized (rs) {
        List<Node> lns = rs.getAllNodes();
        for (Node n : lns) {
          if (!ndmap.containsKey(n.getNode_name())) {
            r += "\t" + n.getNode_name() + ", " + n.getIps().toString() + "\n";
          }
        }
      }
      r += "}\n";
      r += "toReRep Device list: {\n";
      synchronized (toReRep) {
        for (String dev : toReRep.keySet()) {
          r += "\t" + dev + "\n";
        }
      }
      r += "}\n";
      r += "toUnspc Device list: {\n";
      synchronized (toUnspc) {
        for (String dev : toUnspc.keySet()) {
          r += "\t" + dev + "\n";
        }
      }
      r += "}\n";

      r += "backupQ: {\n";
      synchronized (backupQ) {
        for (BackupEntry be : backupQ) {
          r += "\t" + be.toString() + "\n";
        }
      }
      r += "}\n";

      r += "repQ: {\n";
      synchronized (repQ) {
        for (DMRequest req : repQ) {
          r += "\t" + req.toString() + "\n";
        }
      }
      r += "}\n";

      r += "cleanQ: {\n";
      synchronized (cleanQ) {
        for (DMRequest req : cleanQ) {
          r += "\t" + req.toString() + "\n";
        }
      }
      r += "}\n";

      r += "RRMAP: {\n";
      synchronized (rrmap) {
        for (Map.Entry<String, MigrateEntry> e : rrmap.entrySet()) {
          r += "\t" + e.getKey() + " -> " + e.getValue().toString();
        }
      }
      r += "}\n";

      return r;
    }

    public Set<DeviceInfo> maskActiveDevice(Set<DeviceInfo> toMask) {
      Set<DeviceInfo> masked = new TreeSet<DeviceInfo>();

      for (DeviceInfo odi : toMask) {
        boolean found = false;

        for (Map.Entry<String, NodeInfo> e : ndmap.entrySet()) {
          NodeInfo ni = e.getValue();
          if (ni.dis != null && ni.dis.size() > 0) {
            for (DeviceInfo di : ni.dis) {
              if (odi.dev.equalsIgnoreCase(di.dev)) {
                // this means the device is active!
                found = true;
                break;
              }
            }
          }
        }
        if (!found) {
          masked.add(odi);
        }
      }
      return masked;
    }

    // Return old devs
    public NodeInfo addToNDMap(Node node, List<DeviceInfo> ndi) {
      // flush to database
      if (ndi != null) {
        for (DeviceInfo di : ndi) {
          try {
            synchronized (rs) {
              rs.createOrUpdateDevice(di, node);
            }
          } catch (InvalidObjectException e) {
            LOG.error(e, e);
          } catch (MetaException e) {
            LOG.error(e, e);
          }
        }
      }
      NodeInfo ni = ndmap.get(node.getNode_name());
      if (ni == null) {
        ni = new NodeInfo(ndi);
        ni = ndmap.put(node.getNode_name(), ni);
      } else {
        Set<DeviceInfo> old, cur;
        old = new TreeSet<DeviceInfo>();
        cur = new TreeSet<DeviceInfo>();

        synchronized (ni) {
          ni.lastRptTs = System.currentTimeMillis();
          if (ni.dis != null) {
            for (DeviceInfo di : ni.dis) {
              old.add(di);
            }
          }
          if (ndi != null) {
            for (DeviceInfo di : ndi) {
              cur.add(di);
            }
          }
          ni.dis = ndi;
        }
        // check if we lost some devices
        if (cur.containsAll(old)) {
          // old is subset of cur => add in some devices, it is OK.
          cur.removeAll(old);
          old.clear();
        } else if (old.containsAll(cur)) {
          // cur is subset of old => delete some devices, check if we can do some re-replicate?
          old.removeAll(cur);
          cur.clear();
        } else {
          // neither
          Set<DeviceInfo> inter = new TreeSet<DeviceInfo>();
          inter.addAll(old);
          inter.retainAll(cur);
          old.removeAll(cur);
          cur.removeAll(inter);
        }
        // fitler active device on other node, for example, the nas device.
        old = maskActiveDevice(old);

        for (DeviceInfo di : old) {
          LOG.debug("Queue Device " + di.dev + " on toReRep set.");
          synchronized (toReRep) {
            if (!toReRep.containsKey(di.dev)) {
              toReRep.put(di.dev, System.currentTimeMillis());
              toUnspc.remove(di.dev);
            }
          }
        }
        for (DeviceInfo di : cur) {
          synchronized (toReRep) {
            if (toReRep.containsKey(di.dev)) {
              LOG.debug("Devcie " + di.dev + " is back, do not make SFL SUSPECT!");
              toReRep.remove(di.dev);
            }
          }
          LOG.debug("Queue Device " + di.dev + " on toUnspc set.");
          synchronized (toUnspc) {
            if (!toUnspc.containsKey(di.dev)) {
              toUnspc.put(di.dev, System.currentTimeMillis());
            }
          }
        }
      }

      // check if we can leave safe mode
      try {
        long cn;
        synchronized (rs) {
          cn = rs.countNode();
        }
        if (safeMode && ((double) ndmap.size() / (double) cn > 0.99)) {

          LOG.info("Nodemap size: " + ndmap.size() + ", saved size: " + cn + ", reach "
              +
              (double) ndmap.size() / (double) cn * 100 + "%, leave SafeMode.");
          safeMode = false;
        }
      } catch (MetaException e) {
        LOG.error(e, e);
      }
      return ni;
    }

    public NodeInfo removeFromNDMapWTO(String node, long cts) {
      NodeInfo ni = ndmap.get(node);

      if (ni.lastRptTs + DMTimerTask.timeout < cts) {
        if (ni.toDelete.size() == 0 && ni.toRep.size() == 0) {
          ni = ndmap.remove(node);
          if (ni.toDelete.size() > 0 || ni.toRep.size() > 0) {
            LOG.error("Might miss entries here ... toDelete {" + ni.toDelete.toString() + "}, toRep {" + ni.toRep.toString() + "}");
          }
          // update Node status here
          try {
            synchronized (rs) {
              Node saved = rs.getNode(node);
              saved.setStatus(MetaStoreConst.MNodeStatus.SUSPECT);
              rs.updateNode(saved);
            }
          } catch (MetaException e) {
            LOG.error(e, e);
          }
        } else {
          LOG.warn("Inactive node " + node + " with pending operations: toDelete " + ni.toDelete.size() + ", toRep " + ni.toRep.size());
        }
      }
      try {
        synchronized (rs) {
          if ((double)ndmap.size() / (double)rs.countNode() <= 0.5) {
            safeMode = true;
            LOG.info("Lost too many Nodes, enter into SafeMode now.");
          }
        }
      } catch (MetaException e) {
        LOG.error(e, e);
      }
      return ni;
    }

    public void SafeModeStateChange() {
      try {
        synchronized (rs) {
          if ((double)ndmap.size() / (double)rs.countNode() <= 0.5) {
            safeMode = true;
            LOG.info("Lost too many Nodes, enter into SafeMode now.");
          }
        }
      } catch (MetaException e) {
        LOG.error(e,e);
      }
    }

    public List<Node> findBestNodes(int nr) throws IOException {
      if (safeMode) {
        throw new IOException("Disk Manager is in Safe Mode, waiting for disk reports ...\n");
      }
      if (nr <= 0) {
        return new ArrayList<Node>();
      }
      List<Node> r = new ArrayList<Node>(nr);
      SortedMap<Long, String> m = new TreeMap<Long, String>();

      for (Map.Entry<String, NodeInfo> entry : ndmap.entrySet()) {
        NodeInfo ni = entry.getValue();
        synchronized (ni) {
          List<DeviceInfo> dis = ni.dis;
          long thisfree = 0;

          if (dis == null) {
            continue;
          }
          for (DeviceInfo di : dis) {
            thisfree += di.free;
          }
          if (thisfree > 0) {
            m.put(thisfree, entry.getKey());
          }
        }
      }

      int i = 0;
      for (Map.Entry<Long, String> entry : m.entrySet()) {
        if (i >= nr) {
          break;
        }
        synchronized (rs) {
          try {
            Node n = rs.getNode(entry.getValue());
            if (n != null) {
              r.add(n);
              i++;
            }
          } catch (MetaException e) {
            LOG.error(e, e);
          }

        }
      }
      return r;
    }

    public List<Node> findBestNodesBySingleDev(int nr) throws IOException {
      if (safeMode) {
        throw new IOException("Disk Manager is in Safe Mode, waiting for disk reports ...\n");
      }
      if (nr <= 0) {
        return new ArrayList<Node>();
      }
      List<Node> r = new ArrayList<Node>(nr);
      SortedMap<Long, String> m = new TreeMap<Long, String>();
      HashSet<String> rset = new HashSet<String>();

      for (Map.Entry<String, NodeInfo> entry : ndmap.entrySet()) {
        NodeInfo ni = entry.getValue();
        synchronized (ni) {
          List<DeviceInfo> dis = ni.dis;

          if (dis == null) {
            continue;
          }
          for (DeviceInfo di : dis) {
            if (di.free > 0) {
              m.put(di.free, entry.getKey());
            }
          }
        }
      }

      int i = 0;
      for (Map.Entry<Long, String> entry : m.entrySet()) {
        if (i >= nr) {
          break;
        }
        synchronized (rs) {
          try {
            Node n = rs.getNode(entry.getValue());
            if (n != null && !rset.contains(n.getNode_name())) {
              r.add(n);
              rset.add(n.getNode_name());
              i++;
            }
          } catch (MetaException e) {
            LOG.error(e, e);
          }
        }
      }
      return r;
    }

    public String getMP(String node_name, String devid) throws MetaException {
      NodeInfo ni = ndmap.get(node_name);
      if (ni == null) {
        throw new MetaException("Can't find Node '" + node_name + "' in ndmap.");
      }
      String mp = ni.getMP(devid);
      if (mp == null) {
        throw new MetaException("Can't find DEV '" + devid + "' in Node '" + node_name + "'.");
      }
      return mp;
    }

    static public class FileLocatingPolicy {
      public static final int EXCLUDE_NODES_DEVS = 0;
      public static final int EXCLUDE_NODES_DEVS_SHARED = 1;
      public static final int SPECIFY_NODES = 2;
      public static final int SPECIFY_NODES_DEVS = 3;

      Set<String> nodes;
      Set<String> devs;
      int mode;
      boolean canIgnore;

      public FileLocatingPolicy(Set<String> nodes, Set<String> devs, int mode, boolean canIgnore) {
        this.nodes = nodes;
        this.devs = devs;
        this.mode = mode;
        this.canIgnore = canIgnore;
      }
    }

    private boolean canFindDevices(NodeInfo ni, Set<String> devs) {
      boolean canFind = false;

      if (devs == null || devs.size() == 0) {
        return true;
      }
      if (ni != null) {
        List<DeviceInfo> dis = ni.dis;
        if (dis != null) {
          for (DeviceInfo di : dis) {
            if (!devs.contains(di.dev)) {
              canFind = true;
              break;
            }
          }
        }
      }

      return canFind;
    }

    private Set<String> findSharedDevs(List<DeviceInfo> devs) {
      Set<String> r = new TreeSet<String>();

      for (DeviceInfo di : devs) {
        if (di.prop == MetaStoreConst.MDeviceProp.SHARED ||
            di.prop == MetaStoreConst.MDeviceProp.BACKUP) {
          r.add(di.dev);
        }
      }
      return r;
    }

    public String findBestNode(FileLocatingPolicy flp) throws IOException {
      boolean isExclude = true;

      if (flp == null) {
        return findBestNode(false);
      }
      if (safeMode) {
        throw new IOException("Disk Manager is in Safe Mode, waiting for disk reports ...\n");
      }
      switch (flp.mode) {
      case FileLocatingPolicy.EXCLUDE_NODES_DEVS:
        if (flp.nodes == null || flp.nodes.size() == 0) {
          return findBestNode(false);
        }
        break;
      case FileLocatingPolicy.EXCLUDE_NODES_DEVS_SHARED:
        if (flp.nodes == null || flp.nodes.size() == 0) {
          return findBestNode(true);
        }
        break;
      case FileLocatingPolicy.SPECIFY_NODES:
      case FileLocatingPolicy.SPECIFY_NODES_DEVS:
        if (flp.nodes == null || flp.nodes.size() == 0) {
          return null;
        }
        isExclude = false;
        break;
      }

      long largest = 0;
      String largestNode = null;

      for (Map.Entry<String, NodeInfo> entry : ndmap.entrySet()) {
        NodeInfo ni = entry.getValue();
        synchronized (ni) {
          List<DeviceInfo> dis = ni.dis;
          long thisfree = 0;
          boolean ignore = false;

          if (isExclude) {
            if (flp.nodes.contains(entry.getKey())) {
              ignore = true;
            }
            Set<String> excludeDevs = new TreeSet<String>();
            if (flp.devs != null) {
              excludeDevs.addAll(flp.devs);
            }
            if (flp.mode == FileLocatingPolicy.EXCLUDE_NODES_DEVS_SHARED) {
              excludeDevs.addAll(findSharedDevs(dis));
            }
            if (!canFindDevices(ni, excludeDevs)) {
              ignore = true;
            }
          } else {
            if (!flp.nodes.contains(entry.getKey())) {
              ignore = true;
            }
          }
          if (ignore || dis == null) {
            continue;
          }
          for (DeviceInfo di : dis) {
            thisfree += di.free;
          }
          if (thisfree > largest) {
            largestNode = entry.getKey();
            largest = thisfree;
          }
        }
      }
      if (largestNode == null && flp.canIgnore) {
        return findBestNode(false);
      }

      return largestNode;
    }

    public String findBestNode(boolean ignoreShared) throws IOException {
      if (safeMode) {
        throw new IOException("Disk Manager is in Safe Mode, waiting for disk reports ...\n");
      }
      long largest = 0;
      String largestNode = null;

      for (Map.Entry<String, NodeInfo> entry : ndmap.entrySet()) {
        NodeInfo ni = entry.getValue();
        synchronized (ni) {
          List<DeviceInfo> dis = ni.dis;
          long thisfree = 0;

          if (dis == null) {
            continue;
          }
          for (DeviceInfo di : dis) {
            if (!(ignoreShared && (di.prop == MetaStoreConst.MDeviceProp.SHARED || di.prop == MetaStoreConst.MDeviceProp.BACKUP))) {
              thisfree += di.free;
            }
          }
          if (thisfree > largest) {
            largestNode = entry.getKey();
            largest = thisfree;
          }
        }
      }

      return largestNode;
    }

    private void findBackupDevice(Set<String> dev, Set<String> node) {
      for (Map.Entry<String, NodeInfo> e : ndmap.entrySet()) {
        for (DeviceInfo di : e.getValue().dis) {
          Device d = null;
          synchronized (rs) {
            try {
              d = rs.getDevice(di.dev);
            } catch (MetaException e1) {
              LOG.error(e1, e1);
            } catch (NoSuchObjectException e1) {
              LOG.error(e1, e1);
            }
          }
          if (d != null && (d.getProp() == MetaStoreConst.MDeviceProp.BACKUP || d.getProp() == MetaStoreConst.MDeviceProp.BACKUP_ALONE)) {
            dev.add(di.dev);
            node.add(d.getNode_name());
          }
        }
      }
    }

    // filter backup device either
    private List<DeviceInfo> filterSharedDevice(String node_name, List<DeviceInfo> orig) {
      List<DeviceInfo> r = new ArrayList<DeviceInfo>();

      for (DeviceInfo di : orig) {
        if (di.prop == MetaStoreConst.MDeviceProp.ALONE) {
          r.add(di);
        }
      }

      return r;
    }

    public List<DeviceInfo> findDevices(String node) throws IOException {
      if (safeMode) {
        throw new IOException("Disk Manager is in Safe Mode, waiting for disk reports ...\n");
      }
      NodeInfo ni = ndmap.get(node);
      if (ni == null) {
        return null;
      } else {
        return filterSharedDevice(node, ni.dis);
      }
    }

    public String findBestDevice(String node, FileLocatingPolicy flp) throws IOException {
      if (safeMode) {
        throw new IOException("Disk Manager is in Safe Mode, waiting for disk reports ...\n");
      }
      NodeInfo ni = ndmap.get(node);
      if (ni == null) {
        throw new IOException("Node '" + node + "' does not exist in NDMap, are you sure node '" + node + "' belongs to this MetaStore?" + hiveConf.getVar(HiveConf.ConfVars.LOCAL_DATACENTER) + "\n");
      }
      List<DeviceInfo> dilist = ni.dis;
      if (flp.mode == FileLocatingPolicy.EXCLUDE_NODES_DEVS_SHARED) {
        synchronized (ni) {
          dilist = filterSharedDevice(node, ni.dis);
        }
      }
      String bestDev = null;
      long free = 0;

      if (dilist == null) {
        return null;
      }
      for (DeviceInfo di : dilist) {
        boolean ignore = false;

        if (flp.mode == FileLocatingPolicy.EXCLUDE_NODES_DEVS ||
            flp.mode == FileLocatingPolicy.EXCLUDE_NODES_DEVS_SHARED) {
          if (flp.devs != null && flp.devs.contains(di.dev)) {
            ignore = true;
            break;
          }
        } else if (flp.mode == FileLocatingPolicy.SPECIFY_NODES_DEVS) {
          if (flp.devs != null && !flp.devs.contains(di.dev)) {
            ignore = true;
            break;
          }
        }
        if (!ignore && di.free > free) {
          bestDev = di.dev;
          free = di.free;
        }
      }
      if (bestDev == null && flp.canIgnore) {
        for (DeviceInfo di : dilist) {
          if (di.free > free) {
            bestDev = di.dev;
          }
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
              e.printStackTrace();
            }
            continue;
          }
          if (r.op == DMRequest.DMROperation.RM_PHYSICAL) {
            synchronized (ndmap) {
              for (SFileLocation loc : r.file.getLocations()) {
                NodeInfo ni = ndmap.get(loc.getNode_name());
                if (ni == null) {
                  // add back to cleanQ
                  synchronized (cleanQ) {
                    cleanQ.add(r);
                  }
                  break;
                }
                synchronized (ni.toDelete) {
                  ni.toDelete.add(loc);
                  LOG.info("----> Add toDelete " + loc.getLocation() + ", qs " + cleanQ.size() + ", " + r.file.getLocationsSize());
                }
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
              LOG.debug(e, e);
            }
            continue;
          }
          if (r.op == DMRequest.DMROperation.REPLICATE) {
            FileLocatingPolicy flp, flp_default, flp_backup;
            Set<String> excludes = new TreeSet<String>();
            Set<String> excl_dev = new TreeSet<String>();
            Set<String> spec_dev = new TreeSet<String>();
            Set<String> spec_node = new TreeSet<String>();

            // find backup device
            findBackupDevice(spec_dev, spec_node);
            LOG.debug("Try to write to backup device firstly: N <" + spec_node.toArray().toString() +
                ">, D <" + spec_dev.toArray().toString() + ">");

            // exclude old file locations
            for (int i = 0; i < r.begin_idx; i++) {
              excludes.add(r.file.getLocations().get(i).getNode_name());
              excl_dev.add(r.file.getLocations().get(i).getDevid());
              // remove if this backup device has already used
              if (spec_dev.remove(r.file.getLocations().get(i).getDevid())) {
                spec_dev.clear();
              }
            }

            flp = flp_default = new FileLocatingPolicy(excludes, excl_dev, FileLocatingPolicy.EXCLUDE_NODES_DEVS, true);
            flp_backup = new FileLocatingPolicy(spec_node, spec_dev, FileLocatingPolicy.SPECIFY_NODES_DEVS, true);

            for (int i = r.begin_idx; i < r.file.getRep_nr(); i++, flp = flp_default) {
              if (i == r.begin_idx) {
                if (spec_dev.size() > 0) {
                  flp = flp_backup;
                }
              }
              try {
                String node_name = findBestNode(flp);
                if (node_name == null) {
                  LOG.info("Could not find any best node to replicate file " + r.file.getFid());
                  r.begin_idx = i;
                  // insert back to the queue;
                  synchronized (repQ) {
                    repQ.add(r);
                  }
                  break;
                }
                excludes.add(node_name);
                String devid = findBestDevice(node_name, flp);
                if (devid == null) {
                  LOG.info("Could not find any best device on node " + node_name + " to replicate file " + r.file.getFid());
                  r.begin_idx = i;
                  // insert back to the queue;
                  synchronized (repQ) {
                    repQ.add(r);
                  }
                  break;
                }
                excl_dev.add(devid);

                String location;
                Random rand = new Random();
                SFileLocation nloc;

                do {
                  location = "/data/";
                  if (r.file.getPlacement() > 0) {
                    synchronized (rs) {
                      Table t = rs.getTableByID(r.file.getPlacement());
                      location += t.getDbName() + "/" + t.getTableName() + "/"
                          + rand.nextInt(Integer.MAX_VALUE);
                    }
                  } else {
                    location += "UNNAMED-DB/UNNAMED-TABLE/" + rand.nextInt(Integer.MAX_VALUE);
                  }
                  nloc = new SFileLocation(node_name, r.file.getFid(), devid, location,
                      i, System.currentTimeMillis(),
                      MetaStoreConst.MFileLocationVisitStatus.OFFLINE, "SFL_REP_DEFAULT");
                  synchronized (rs) {
                    if (rs.createFileLocation(nloc)) {
                      break;
                    }
                  }
                } while (true);
                r.file.addToLocations(nloc);

                // indicate file transfer
                JSONObject jo = new JSONObject();
                try {
                  JSONObject j = new JSONObject();
                  NodeInfo ni = ndmap.get(r.file.getLocations().get(0).getNode_name());

                  if (ni == null) {
                    throw new IOException("Can not find Node '" + node_name + "' in nodemap now, is it offline?");
                  }
                  j.put("node_name", r.file.getLocations().get(0).getNode_name());
                  j.put("devid", r.file.getLocations().get(0).getDevid());
                  j.put("mp", ni.getMP(r.file.getLocations().get(0).getDevid()));
                  j.put("location", r.file.getLocations().get(0).getLocation());
                  jo.put("from", j);

                  j = new JSONObject();
                  ni = ndmap.get(r.file.getLocations().get(i).getNode_name());
                  if (ni == null) {
                    throw new IOException("Can not find Node '" + node_name + "' in nodemap now, is it offline?");
                  }
                  j.put("node_name", r.file.getLocations().get(i).getNode_name());
                  j.put("devid", r.file.getLocations().get(i).getDevid());
                  j.put("mp", ni.getMP(r.file.getLocations().get(i).getDevid()));
                  j.put("location", r.file.getLocations().get(i).getLocation());
                  jo.put("to", j);
                } catch (JSONException e) {
                  LOG.error(e, e);
                  continue;
                }
                synchronized (ndmap) {
                  NodeInfo ni = ndmap.get(node_name);
                  if (ni == null) {
                    LOG.error("Can not find Node '" + node_name + "' in nodemap now, is it offline?");
                  } else {
                    synchronized (ni.toRep) {
                      ni.toRep.add(jo);
                      LOG.info("----> ADD to Node " + node_name + "'s toRep " + jo);
                    }
                  }
                }
              } catch (IOException e) {
                LOG.error(e, e);
                r.begin_idx = i;
                // insert back to the queue;
                synchronized (repQ) {
                  repQ.add(r);
                  try {
                    Thread.sleep(100);
                  } catch (InterruptedException e1) {
                  }
                  repQ.notify();
                }
                break;
              } catch (MetaException e) {
                LOG.error(e, e);
              } catch (InvalidObjectException e) {
                LOG.error(e, e);
              }
            }
          } else if (r.op == DMRequest.DMROperation.MIGRATE) {
            SFileLocation source = null, target = null;

            // select a source node
            if (r.file == null || r.tfile == null) {
              LOG.error("Invalid DMRequest provided, NULL SFile!");
              continue;
            }
            if (r.file.getLocationsSize() > 0) {
              // select the 0th location
              source = r.file.getLocations().get(0);
            }
            // determine the target node
            if (r.tfile.getLocationsSize() > 0) {
              // select the 0th location
              target = r.tfile.getLocations().get(0);
            }
            // indicate file transfer
            JSONObject jo = new JSONObject();
            try {
              JSONObject j = new JSONObject();
              NodeInfo ni = ndmap.get(source.getNode_name());

              if (ni == null) {
                throw new IOException("Can not find Node '" + source.getNode_name() + "' in ndoemap now.");
              }
              j.put("node_name", source.getNode_name());
              j.put("devid", source.getDevid());
              j.put("mp", ni.getMP(source.getDevid()));
              j.put("location", source.getLocation());
              jo.put("from", j);

              j = new JSONObject();
              if (r.devmap.get(target.getDevid()) == null) {
                throw new IOException("Can not find DEV '" + target.getDevid() + "' in pre-generated devmap.");
              }
              j.put("node_name", target.getNode_name());
              j.put("devid", target.getDevid());
              j.put("mp", r.devmap.get(target.getDevid()));
              j.put("location", target.getLocation());
              jo.put("to", j);
            } catch (JSONException e) {
              LOG.error(e, e);
              continue;
            } catch (IOException e) {
              LOG.error(e, e);
              continue;
            }
            synchronized (ndmap) {
              NodeInfo ni = ndmap.get(source.getNode_name());
              if (ni == null) {
                LOG.error("Can not find Node '" + source.getNode_name() + "' in nodemap.");
              } else {
                synchronized (ni.toRep) {
                  ni.toRep.add(jo);
                  LOG.info("----> ADD toRep (by migrate)" + jo);
                }
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
        String infos = "----node----->" + r.node + "\n";
        if (r.dil != null) {
          for (DeviceInfo di : r.dil) {
            infos += "----DEVINFO------>" + di.dev + "," + di.mp + "," + di.used + "," + di.free + "\n";
          }
        }
        LOG.debug(infos);

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
          } else if (cmds[i].startsWith("+FAIL:")) {
            LOG.error("RECV ERR: " + cmds[i]);
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
            LOG.debug("Invalid report line: " + lines[i]);
            continue;
          }
          DeviceInfo di = new DeviceInfo();
          di.dev = kv[0];
          String stats[] = kv[1].split(",");
          if (stats == null || stats.length < 6) {
            LOG.debug("Invalid report line value: " + lines[i]);
            continue;
          }
          synchronized (rs) {
            try {
              Device d = rs.getDevice(di.dev);
              di.prop = d.getProp();
            } catch (MetaException e) {
              LOG.error(e, e);
            } catch (NoSuchObjectException e) {
              LOG.error(e, e);
            }
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
          LOG.debug("RECV: " + recvStr);

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
              LOG.error(e, e);
            }
          } else {
            try {
              synchronized (rs) {
                reportNode = rs.getNode(report.node);
              }
            } catch (MetaException e) {
              LOG.error(e, e);
            }
          }

          String sendStr = "+OK\n";

          if (reportNode == null) {
            String errStr = "Failed to find Node: " + report.node + ", IP=" + recvPacket.getAddress().getHostAddress();
            LOG.warn(errStr);
            // try to use "+NODE:node_name" to find
            sendStr = "+FAIL\n";
            sendStr += "+COMMENT:" + errStr;
          } else {
            // 1. update Node status
            switch (reportNode.getStatus()) {
            default:
            case MetaStoreConst.MNodeStatus.ONLINE:
              break;
            case MetaStoreConst.MNodeStatus.SUSPECT:
              try {
                reportNode.setStatus(MetaStoreConst.MNodeStatus.ONLINE);
                synchronized (rs) {
                  rs.updateNode(reportNode);
                }
              } catch (MetaException e) {
                LOG.error(e, e);
              }
              break;
            case MetaStoreConst.MNodeStatus.OFFLINE:
              LOG.warn("OFFLINE node '" + reportNode.getNode_name() + "' do report?!");
              break;
            }

            // 2. update NDMap
            synchronized (ndmap) {
              addToNDMap(reportNode, report.dil);
            }

            // 2.NA update metadata
            Set<SFile> toCheckRep = new HashSet<SFile>();
            Set<SFile> toCheckDel = new HashSet<SFile>();
            Set<SFLTriple> toCheckMig = new HashSet<SFLTriple>();
            if (report.replies != null) {
              for (DMReply r : report.replies) {
                String[] args = r.args.split(",");
                switch (r.type) {
                case REPLICATED:
                  if (args.length < 3) {
                    LOG.warn("Invalid REP report: " + r.args);
                  } else {
                    try {
                      SFileLocation newsfl;

                      synchronized (rs) {
                        newsfl = rs.getSFileLocation(args[1], args[2]);
                        if (newsfl == null) {
                          SFLTriple t = new SFLTriple(args[0], args[1], args[2]);
                          if (rrmap.containsKey(t.toString())) {
                            // this means REP might actually MIGRATE
                            toCheckMig.add(new SFLTriple(args[0], args[1], args[2]));
                            LOG.info("----> MIGRATE to " + args[0] + ":" + args[1] + "/" + args[2] + " DONE.");
                            break;
                          }
                          throw new MetaException("Can not find SFileLocation " + args[0] + "," + args[1] + "," + args[2]);
                        }
                        SFile file = rs.getSFile(newsfl.getFid());
                        if (file != null) {
                          toCheckRep.add(file);
                          newsfl.setVisit_status(MetaStoreConst.MFileLocationVisitStatus.ONLINE);
                          // We should check the digest here, and compare it with file.getDigest().
                          newsfl.setDigest(args[3]);
                          rs.updateSFileLocation(newsfl);
                        }
                      }
                    } catch (MetaException e) {
                      LOG.error(e, e);
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
                        SFileLocation sfl = rs.getSFileLocation(args[1], args[2]);
                        if (sfl != null) {
                          SFile file = rs.getSFile(sfl.getFid());
                          toCheckDel.add(file);
                          rs.delSFileLocation(args[1], args[2]);
                        }
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
                  LOG.error(e, e);
                }
              }
              toCheckRep.clear();
            }
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
            if (!toCheckMig.isEmpty()) {
              if (HMSHandler.topdcli == null) {
                try {
                  HiveMetaStore.connect_to_top_dc(hiveConf);
                } catch (MetaException e) {
                  LOG.error(e, e);
                }
              }
              if (HMSHandler.topdcli != null) {
                for (SFLTriple t : toCheckMig) {
                  MigrateEntry me = rrmap.get(t.toString());
                  if (me == null) {
                    LOG.error("Invalid SFLTriple-MigrateEntry map.");
                    continue;
                  } else {
                    rrmap.remove(t);
                    // connect to remote DC, and close the file
                    try {
                      Datacenter rdc = HMSHandler.topdcli.get_center(me.to_dc);
                      IMetaStoreClient rcli = new HiveMetaStoreClient(rdc.getLocationUri(),
                          HiveConf.getIntVar(hiveConf, HiveConf.ConfVars.METASTORETHRIFTCONNECTIONRETRIES),
                          hiveConf.getIntVar(HiveConf.ConfVars.METASTORE_CLIENT_CONNECT_RETRY_DELAY),
                          null);
                      SFile sf = rcli.get_file_by_name(t.node, t.devid, t.location);
                      sf.setDigest("REMOTE-DIGESTED!");
                      rcli.close_file(sf);
                      LOG.info("Close remote file: " + t.node + ":" + t.devid + ":" + t.location);
                      rcli.close();
                    } catch (NoSuchObjectException e) {
                      LOG.error(e, e);
                    } catch (TException e) {
                      LOG.error(e, e);
                    }
                    // remove the partition-file relationship from metastore
                    if (me.is_part) {
                      // is partition
                      synchronized (rs) {
                        Partition np;
                        try {
                          np = rs.getPartition(me.part.getDbName(), me.part.getTableName(), me.part.getPartitionName());
                          long fid = me.timap.get(t.toString());
                          me.timap.remove(t.toString());
                          List<Long> nfiles = new ArrayList<Long>();
                          nfiles.addAll(np.getFiles());
                          nfiles.remove(fid);
                          np.setFiles(nfiles);
                          rs.updatePartition(np);
                          LOG.info("Remove file fid " + fid + " from partition " + np.getPartitionName());
                        } catch (MetaException e) {
                          LOG.error(e, e);
                        } catch (NoSuchObjectException e) {
                          LOG.error(e, e);
                        } catch (InvalidObjectException e) {
                          LOG.error(e, e);
                        }
                      }
                    } else {
                      // subpartition
                      Subpartition np;
                      try {
                        np = rs.getSubpartition(me.subpart.getDbName(), me.subpart.getTableName(), me.subpart.getPartitionName());
                        long fid = me.timap.get(t.toString());
                        me.timap.remove(t.toString());
                        List<Long> nfiles = new ArrayList<Long>();
                        nfiles.addAll(np.getFiles());
                        nfiles.remove(fid);
                        np.setFiles(nfiles);
                        rs.updateSubpartition(np);
                        LOG.info("Remove file fid " + fid + " from subpartition " + np.getPartitionName());
                      } catch (MetaException e) {
                        LOG.error(e, e);
                      } catch (NoSuchObjectException e) {
                        LOG.error(e, e);
                      } catch (InvalidObjectException e) {
                        LOG.error(e, e);
                      }
                    }
                  }
                }
              }
            }

            // 3. append any commands
            int nr = 0;
            int nr_max = hiveConf.getIntVar(HiveConf.ConfVars.DM_APPEND_CMD_MAX);
            synchronized (ndmap) {
              NodeInfo ni = ndmap.get(reportNode.getNode_name());
              if (ni != null && ni.toDelete.size() > 0) {
                synchronized (ni.toDelete) {
                  Set<SFileLocation> ls = new TreeSet<SFileLocation>();
                  for (SFileLocation loc : ni.toDelete) {
                    if (nr >= nr_max) {
                      break;
                    }
                    sendStr += "+DEL:" + loc.getNode_name() + ":" + loc.getDevid() + ":" +
                        ndmap.get(loc.getNode_name()).getMP(loc.getDevid()) + ":" +
                        loc.getLocation() + "\n";
                    ls.add(loc);
                    nr++;
                  }
                  for (SFileLocation l : ls) {
                    ni.toDelete.remove(l);
                  }
                }
              }

              if (ni != null && ni.toRep.size() > 0) {
                synchronized (ni.toRep) {
                  List<JSONObject> jos = new ArrayList<JSONObject>();
                  for (JSONObject jo : ni.toRep) {
                    if (nr >= nr_max) {
                      break;
                    }
                    sendStr += "+REP:" + jo.toString() + "\n";
                    jos.add(jo);
                    nr++;
                  }
                  for (JSONObject j : jos) {
                    ni.toRep.remove(j);
                  }
                }
              }
            }
          }

          // send back the reply
          int port = recvPacket.getPort();
          byte[] sendBuf;
          sendBuf = sendStr.getBytes();
          DatagramPacket sendPacket = new DatagramPacket(sendBuf , sendBuf.length ,
              recvPacket.getAddress() , port );
          try {
            server.send(sendPacket);
          } catch (IOException e) {
            LOG.error(e, e);
          }
        }
      }
    }
}
