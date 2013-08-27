/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.hive.metastore;

import static org.apache.commons.lang.StringUtils.join;
import static org.apache.hadoop.hive.metastore.MetaStoreUtils.DEFAULT_DATABASE_COMMENT;
import static org.apache.hadoop.hive.metastore.MetaStoreUtils.DEFAULT_DATABASE_NAME;
import static org.apache.hadoop.hive.metastore.MetaStoreUtils.validateName;

import java.io.IOException;
import java.net.InetAddress;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.common.LogUtils;
import org.apache.hadoop.hive.common.LogUtils.LogInitializationException;
import org.apache.hadoop.hive.common.classification.InterfaceAudience;
import org.apache.hadoop.hive.common.classification.InterfaceStability;
import org.apache.hadoop.hive.common.cli.CommonCliOptions;
import org.apache.hadoop.hive.common.metrics.Metrics;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.conf.HiveConf.ConfVars;
import org.apache.hadoop.hive.metastore.DiskManager.BackupEntry;
import org.apache.hadoop.hive.metastore.DiskManager.DMRequest;
import org.apache.hadoop.hive.metastore.DiskManager.DeviceInfo;
import org.apache.hadoop.hive.metastore.DiskManager.FileLocatingPolicy;
import org.apache.hadoop.hive.metastore.DiskManager.MigrateEntry;
import org.apache.hadoop.hive.metastore.DiskManager.SFLTriple;
import org.apache.hadoop.hive.metastore.api.AlreadyExistsException;
import org.apache.hadoop.hive.metastore.api.BusiTypeColumn;
import org.apache.hadoop.hive.metastore.api.BusiTypeDatacenter;
import org.apache.hadoop.hive.metastore.api.Busitype;
import org.apache.hadoop.hive.metastore.api.ColumnStatistics;
import org.apache.hadoop.hive.metastore.api.ColumnStatisticsDesc;
import org.apache.hadoop.hive.metastore.api.ColumnStatisticsObj;
import org.apache.hadoop.hive.metastore.api.ConfigValSecurityException;
import org.apache.hadoop.hive.metastore.api.Database;
import org.apache.hadoop.hive.metastore.api.Datacenter;
import org.apache.hadoop.hive.metastore.api.Device;
import org.apache.hadoop.hive.metastore.api.EnvironmentContext;
import org.apache.hadoop.hive.metastore.api.FOFailReason;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.FileOperationException;
import org.apache.hadoop.hive.metastore.api.HiveObjectPrivilege;
import org.apache.hadoop.hive.metastore.api.HiveObjectRef;
import org.apache.hadoop.hive.metastore.api.HiveObjectType;
import org.apache.hadoop.hive.metastore.api.Index;
import org.apache.hadoop.hive.metastore.api.IndexAlreadyExistsException;
import org.apache.hadoop.hive.metastore.api.InvalidInputException;
import org.apache.hadoop.hive.metastore.api.InvalidObjectException;
import org.apache.hadoop.hive.metastore.api.InvalidOperationException;
import org.apache.hadoop.hive.metastore.api.InvalidPartitionException;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.hive.metastore.api.NoSuchObjectException;
import org.apache.hadoop.hive.metastore.api.Node;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.PartitionEventType;
import org.apache.hadoop.hive.metastore.api.PrincipalPrivilegeSet;
import org.apache.hadoop.hive.metastore.api.PrincipalType;
import org.apache.hadoop.hive.metastore.api.PrivilegeBag;
import org.apache.hadoop.hive.metastore.api.PrivilegeGrantInfo;
import org.apache.hadoop.hive.metastore.api.Role;
import org.apache.hadoop.hive.metastore.api.SFile;
import org.apache.hadoop.hive.metastore.api.SFileLocation;
import org.apache.hadoop.hive.metastore.api.SFileRef;
import org.apache.hadoop.hive.metastore.api.Subpartition;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.hadoop.hive.metastore.api.ThriftHiveMetastore;
import org.apache.hadoop.hive.metastore.api.Type;
import org.apache.hadoop.hive.metastore.api.UnknownDBException;
import org.apache.hadoop.hive.metastore.api.UnknownPartitionException;
import org.apache.hadoop.hive.metastore.api.UnknownTableException;
import org.apache.hadoop.hive.metastore.api.hive_metastoreConstants;
import org.apache.hadoop.hive.metastore.events.AddPartitionEvent;
import org.apache.hadoop.hive.metastore.events.AlterPartitionEvent;
import org.apache.hadoop.hive.metastore.events.AlterTableEvent;
import org.apache.hadoop.hive.metastore.events.CreateDatabaseEvent;
import org.apache.hadoop.hive.metastore.events.CreateTableEvent;
import org.apache.hadoop.hive.metastore.events.DropDatabaseEvent;
import org.apache.hadoop.hive.metastore.events.DropPartitionEvent;
import org.apache.hadoop.hive.metastore.events.DropTableEvent;
import org.apache.hadoop.hive.metastore.events.EventCleanerTask;
import org.apache.hadoop.hive.metastore.events.LoadPartitionDoneEvent;
import org.apache.hadoop.hive.metastore.events.PreAddPartitionEvent;
import org.apache.hadoop.hive.metastore.events.PreAlterPartitionEvent;
import org.apache.hadoop.hive.metastore.events.PreAlterTableEvent;
import org.apache.hadoop.hive.metastore.events.PreCreateDatabaseEvent;
import org.apache.hadoop.hive.metastore.events.PreCreateTableEvent;
import org.apache.hadoop.hive.metastore.events.PreDropDatabaseEvent;
import org.apache.hadoop.hive.metastore.events.PreDropPartitionEvent;
import org.apache.hadoop.hive.metastore.events.PreDropTableEvent;
import org.apache.hadoop.hive.metastore.events.PreEventContext;
import org.apache.hadoop.hive.metastore.events.PreLoadPartitionDoneEvent;
import org.apache.hadoop.hive.metastore.model.MDBPrivilege;
import org.apache.hadoop.hive.metastore.model.MGlobalPrivilege;
import org.apache.hadoop.hive.metastore.model.MPartitionColumnPrivilege;
import org.apache.hadoop.hive.metastore.model.MPartitionPrivilege;
import org.apache.hadoop.hive.metastore.model.MRole;
import org.apache.hadoop.hive.metastore.model.MRoleMap;
import org.apache.hadoop.hive.metastore.model.MTableColumnPrivilege;
import org.apache.hadoop.hive.metastore.model.MTablePrivilege;
import org.apache.hadoop.hive.metastore.msg.MSGFactory;
import org.apache.hadoop.hive.metastore.msg.MSGType;
import org.apache.hadoop.hive.metastore.msg.MetaMsgServer;
import org.apache.hadoop.hive.metastore.tools.PartitionFactory;
import org.apache.hadoop.hive.metastore.tools.PartitionFactory.PartitionDefinition;
import org.apache.hadoop.hive.metastore.tools.PartitionFactory.PartitionInfo;
import org.apache.hadoop.hive.metastore.tools.PartitionFactory.PartitionType;
import org.apache.hadoop.hive.serde2.Deserializer;
import org.apache.hadoop.hive.serde2.SerDeException;
import org.apache.hadoop.hive.serde2.SerDeUtils;
import org.apache.hadoop.hive.shims.ShimLoader;
import org.apache.hadoop.hive.thrift.HadoopThriftAuthBridge;
import org.apache.hadoop.hive.thrift.TUGIContainingTransport;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.util.ReflectionUtils;
import org.apache.hadoop.util.StringUtils;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportFactory;

import com.facebook.fb303.FacebookBase;
import com.facebook.fb303.fb_status;

/**
 * TODO:pc remove application logic to a separate interface.
 */
public class HiveMetaStore extends ThriftHiveMetastore {
  public static final Log LOG = LogFactory.getLog(
      HiveMetaStore.class);

  /**
   * default port on which to start the Hive server
   */
  private static final int DEFAULT_HIVE_METASTORE_PORT = 9083;

  private static HadoopThriftAuthBridge.Server saslServer;
  private static boolean useSasl;

  private static final class ChainedTTransportFactory extends TTransportFactory {
    private final TTransportFactory parentTransFactory;
    private final TTransportFactory childTransFactory;

    private ChainedTTransportFactory(
        TTransportFactory parentTransFactory,
        TTransportFactory childTransFactory) {
      this.parentTransFactory = parentTransFactory;
      this.childTransFactory = childTransFactory;
    }

    @Override
    public TTransport getTransport(TTransport trans) {
      return childTransFactory.getTransport(parentTransFactory.getTransport(trans));
    }
  }

  public static DiskManager dm = null;
  public static Random rand = new Random();

  public static class HMSHandler extends FacebookBase implements
      IHMSHandler {
    public static IMetaStoreClient topdcli = null;
    public static String msUri = null;
    public static final Log LOG = HiveMetaStore.LOG;
    private static boolean createDefaultDB = false;
    private String rawStoreClassName;
    private final HiveConf hiveConf; // stores datastore (jpox) properties,
                                     // right now they come from jpox.properties

    private Warehouse wh; // hdfs warehouse
    private final ThreadLocal<RawStore> threadLocalMS =
        new ThreadLocal<RawStore>() {
          @Override
          protected synchronized RawStore initialValue() {
            return null;
          }
        };

    // Thread local configuration is needed as many threads could make changes
    // to the conf using the connection hook
    private final ThreadLocal<Configuration> threadLocalConf =
        new ThreadLocal<Configuration>() {
          @Override
          protected synchronized Configuration initialValue() {
            return null;
          }
        };

    public static final String AUDIT_FORMAT =
        "ugi=%s\t" + // ugi
            "ip=%s\t" + // remote IP
            "cmd=%s\t"; // command
    public static final Log auditLog = LogFactory.getLog(
        HiveMetaStore.class.getName() + ".audit");
    private static final ThreadLocal<Formatter> auditFormatter =
        new ThreadLocal<Formatter>() {
          @Override
          protected Formatter initialValue() {
            return new Formatter(new StringBuilder(AUDIT_FORMAT.length() * 4));
          }
        };

    private final void logAuditEvent(String cmd) {
      if (cmd == null) {
        return;
      }

      UserGroupInformation ugi;
      try {
        ugi = ShimLoader.getHadoopShims().getUGIForConf(getConf());
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
      final Formatter fmt = auditFormatter.get();
      ((StringBuilder) fmt.out()).setLength(0);

      String address;
      if (useSasl) {
        address = saslServer.getRemoteAddress().toString();
      } else {
        address = getIpAddress();
      }
      if (address == null) {
        address = "unknown-ip-addr";
      }

      auditLog.info(fmt.format(AUDIT_FORMAT, ugi.getUserName(),
          address, cmd).toString());
    }

    // The next serial number to be assigned
    private boolean checkForDefaultDb;
    private static int nextSerialNum = 0;
    private static ThreadLocal<Integer> threadLocalId = new ThreadLocal<Integer>() {
      @Override
      protected synchronized Integer initialValue() {
        return new Integer(nextSerialNum++);
      }
    };

    // This will only be set if the metastore is being accessed from a metastore Thrift server,
    // not if it is from the CLI. Also, only if the TTransport being used to connect is an
    // instance of TSocket.
    private static ThreadLocal<String> threadLocalIpAddress = new ThreadLocal<String>() {
      @Override
      protected synchronized String initialValue() {
        return null;
      }
    };

    public static void setIpAddress(String ipAddress) {
      threadLocalIpAddress.set(ipAddress);
    }

    // This will return null if the metastore is not being accessed from a metastore Thrift server,
    // or if the TTransport being used to connect is not an instance of TSocket.
    public static String getIpAddress() {
      return threadLocalIpAddress.get();
    }

    public static Integer get() {
      return threadLocalId.get();
    }

    public HMSHandler(String name) throws MetaException {
      super(name);
      hiveConf = new HiveConf(this.getClass());
      init();
    }

    public HMSHandler(String name, HiveConf conf) throws MetaException {
      super(name);
      hiveConf = conf;
      init();
    }

    public HiveConf getHiveConf() {
      return hiveConf;
    }

    private ClassLoader classLoader;
    private AlterHandler alterHandler;
    private List<MetaStorePreEventListener> preListeners;
    private List<MetaStoreEventListener> listeners;
    private List<MetaStoreEndFunctionListener> endFunctionListeners;

    {
      classLoader = Thread.currentThread().getContextClassLoader();
      if (classLoader == null) {
        classLoader = Configuration.class.getClassLoader();
      }
    }

    private boolean init() throws MetaException {
      rawStoreClassName = hiveConf.getVar(HiveConf.ConfVars.METASTORE_RAW_STORE_IMPL);
      checkForDefaultDb = hiveConf.getBoolean(
          "hive.metastore.checkForDefaultDb", true);
      String alterHandlerName = hiveConf.get("hive.metastore.alter.impl",
          HiveAlterHandler.class.getName());
      alterHandler = (AlterHandler) ReflectionUtils.newInstance(MetaStoreUtils.getClass(
          alterHandlerName), hiveConf);
      wh = new Warehouse(hiveConf);

      createDefaultDB();

      if (hiveConf.getBoolean("hive.metastore.metrics.enabled", false)) {
        try {
          Metrics.init();
        } catch (Exception e) {
          // log exception, but ignore inability to start
          LOG.error("error in Metrics init: " + e.getClass().getName() + " "
              + e.getMessage());
          MetaStoreUtils.printStackTrace(e);

        }
      }

      preListeners = MetaStoreUtils.getMetaStoreListeners(MetaStorePreEventListener.class,
          hiveConf,
          hiveConf.getVar(HiveConf.ConfVars.METASTORE_PRE_EVENT_LISTENERS));
      listeners = MetaStoreUtils.getMetaStoreListeners(MetaStoreEventListener.class, hiveConf,
          hiveConf.getVar(HiveConf.ConfVars.METASTORE_EVENT_LISTENERS));
      endFunctionListeners = MetaStoreUtils.getMetaStoreListeners(
          MetaStoreEndFunctionListener.class, hiveConf,
          hiveConf.getVar(HiveConf.ConfVars.METASTORE_END_FUNCTION_LISTENERS));

      long cleanFreq = hiveConf.getLongVar(ConfVars.METASTORE_EVENT_CLEAN_FREQ) * 1000L;
      if (cleanFreq > 0) {
        // In default config, there is no timer.
        Timer cleaner = new Timer("Metastore Events Cleaner Thread", true);
        cleaner.schedule(new EventCleanerTask(this), cleanFreq, cleanFreq);
      }
      return true;
    }

    private String addPrefix(String s) {
      return threadLocalId.get() + ": " + s;
    }

    public void setConf(Configuration conf) {
      threadLocalConf.set(conf);
      RawStore ms = threadLocalMS.get();
      if (ms != null) {
        ms.setConf(conf);
      }
    }

    private Configuration getConf() {
      Configuration conf = threadLocalConf.get();
      if (conf == null) {
        conf = new Configuration(hiveConf);
        threadLocalConf.set(conf);
      }
      return conf;
    }

    public Warehouse getWh() {
      return wh;
    }

    /**
     * Get a cached RawStore.
     *
     * @return the cached RawStore
     * @throws MetaException
     */
    @InterfaceAudience.LimitedPrivate({"HCATALOG"})
    @InterfaceStability.Evolving
    public RawStore getMS() throws MetaException {
      RawStore ms = threadLocalMS.get();
      if (ms == null) {
        ms = newRawStore();
        threadLocalMS.set(ms);
        ms = threadLocalMS.get();
      }
      return ms;
    }

    private RawStore newRawStore() throws MetaException {
      LOG.info(addPrefix("Opening raw store with implemenation class:"
          + rawStoreClassName));
      Configuration conf = getConf();

      return RetryingRawStore.getProxy(hiveConf, conf, rawStoreClassName, threadLocalId.get());
    }

    private void createDefaultDB_core(RawStore ms) throws MetaException, InvalidObjectException {
      Datacenter ldc = null, mdc = null;

      if (hiveConf.getVar(HiveConf.ConfVars.LOCAL_DATACENTER) == null) {
        throw new MetaException("Please set 'hive.datacenter.local' as the local DC name");
      }
      try {
        ldc = ms.getDatacenter(hiveConf.getVar(HiveConf.ConfVars.LOCAL_DATACENTER));
      } catch (NoSuchObjectException e) {
        ldc = new Datacenter(hiveConf.getVar(HiveConf.ConfVars.LOCAL_DATACENTER), null, HMSHandler.msUri == null ? "DEFAULT_URI" : HMSHandler.msUri, null);
        ms.createDatacenter(ldc);
      }
      if (!hiveConf.getBoolVar(HiveConf.ConfVars.IS_TOP_DATACENTER) && HMSHandler.topdcli != null) {
        try {
          LOG.info(HMSHandler.topdcli + ", " + hiveConf.getVar(HiveConf.ConfVars.LOCAL_DATACENTER));
          mdc = HMSHandler.topdcli.get_center(hiveConf.getVar(HiveConf.ConfVars.LOCAL_DATACENTER));
        } catch (NoSuchObjectException e) {
          mdc = new Datacenter(hiveConf.getVar(HiveConf.ConfVars.LOCAL_DATACENTER), null, HMSHandler.msUri == null ? "DEFAULT_URI" : HMSHandler.msUri, null);
          try {
            HMSHandler.topdcli.create_datacenter(mdc);
          } catch (AlreadyExistsException e1) {
            LOG.error(e, e);
          } catch (TException e1) {
            LOG.error(e, e);
            throw new MetaException("Try to create dc to top-level datacenter failed!");
          }
        } catch (TException e) {
          LOG.error(e, e);
          throw new MetaException("Try to get dc from top-level datacenter failed!");
        }
      }

      if (HMSHandler.msUri != null && !ldc.getLocationUri().equals(HMSHandler.msUri)) {
        // update the msUri now
        ldc.setLocationUri(HMSHandler.msUri);
        try {
          ms.updateDatacenter(ldc);
        } catch (NoSuchObjectException e) {
          LOG.error(e, e);
          throw new MetaException("Try to update datacenter's locationUri failed!");
        }
      }
      if (mdc != null && HMSHandler.msUri != null && !mdc.getLocationUri().equals(HMSHandler.msUri) && HMSHandler.topdcli != null) {
        // update the msUri now
        mdc.setLocationUri(HMSHandler.msUri);
        try {
          HMSHandler.topdcli.update_center(mdc);
        } catch (NoSuchObjectException e) {
          LOG.error(e, e);
          throw new MetaException("Try to update datacenter's locationUri failed!");
        } catch (TException e) {
          LOG.error(e, e);
          throw new MetaException("Try to update datacenter's locationUri failed!");
        }
      }
      ms.setThisDC(ldc.getName());

      try {
        ms.getDatabase(DEFAULT_DATABASE_NAME);
      } catch (NoSuchObjectException e) {
        Database db = new Database(DEFAULT_DATABASE_NAME, DEFAULT_DATABASE_COMMENT,
                wh.getDefaultDatabasePath(DEFAULT_DATABASE_NAME).toString(), null);
        db.setDatacenter(ldc);
        ms.createDatabase(db);
      }
      HMSHandler.createDefaultDB = true;
    }

    /**
     * create default database if it doesn't exist
     *
     * @throws MetaException
     */
    private void createDefaultDB() throws MetaException {
      synchronized (HMSHandler.class) {
        if (HMSHandler.createDefaultDB || !checkForDefaultDb) {
          return;
        }

        try {
          createDefaultDB_core(getMS());
        } catch (InvalidObjectException e) {
          throw new MetaException(e.getMessage());
        } catch (MetaException e) {
          throw e;
        } catch (Exception e) {
          assert (e instanceof RuntimeException);
          throw (RuntimeException) e;
        }
      }
    }

    private void logInfo(String m) {
      LOG.info(threadLocalId.get().toString() + ": " + m);
      logAuditEvent(m);
    }

    public String startFunction(String function, String extraLogInfo) {
      incrementCounter(function);
      logInfo((getIpAddress() == null ? "" : "source:" + getIpAddress() + " ") +
          function + extraLogInfo);
      try {
        Metrics.startScope(function);
      } catch (IOException e) {
        LOG.debug("Exception when starting metrics scope"
            + e.getClass().getName() + " " + e.getMessage());
        MetaStoreUtils.printStackTrace(e);
      }
      return function;
    }

    public String startFunction(String function) {
      return startFunction(function, "");
    }

    public String startTableFunction(String function, String db, String tbl) {
      return startFunction(function, " : db=" + db + " tbl=" + tbl);
    }

    public String startMultiTableFunction(String function, String db, List<String> tbls) {
      String tableNames = join(tbls, ",");
      return startFunction(function, " : db=" + db + " tbls=" + tableNames);
    }

    public String startPartitionFunction(String function, String db, String tbl,
        List<String> partVals) {
      return startFunction(function, " : db=" + db + " tbl=" + tbl
          + "[" + join(partVals, ",") + "]");
    }

    public String startPartitionFunction(String function, String db, String tbl,
        Map<String, String> partName) {
      return startFunction(function, " : db=" + db + " tbl=" + tbl + "partition=" + partName);
    }

    public void endFunction(String function, boolean successful, Exception e) {
      endFunction(function, new MetaStoreEndFunctionContext(successful, e));
    }

    public void endFunction(String function, MetaStoreEndFunctionContext context) {
      try {
        Metrics.endScope(function);
      } catch (IOException e) {
        LOG.debug("Exception when closing metrics scope" + e);
      }

      for (MetaStoreEndFunctionListener listener : endFunctionListeners) {
        listener.onEndFunction(function, context);
      }
    }

    @Override
    public fb_status getStatus() {
      return fb_status.ALIVE;
    }

    @Override
    public void shutdown() {
      logInfo("Shutting down the object store...");
      RawStore ms = threadLocalMS.get();
      if (ms != null) {
        ms.shutdown();
        threadLocalMS.remove();
      }
      logInfo("Metastore shutdown complete.");
    }

    @Override
    public AbstractMap<String, Long> getCounters() {
      AbstractMap<String, Long> counters = super.getCounters();

      // Allow endFunctionListeners to add any counters they have collected
      if (endFunctionListeners != null) {
        for (MetaStoreEndFunctionListener listener : endFunctionListeners) {
          listener.exportCounters(counters);
        }
      }

      return counters;
    }

    private void create_database_core(RawStore ms, final Database db)
        throws AlreadyExistsException, InvalidObjectException, MetaException {
      if (!validateName(db.getName())) {
        throw new InvalidObjectException(db.getName() + " is not a valid database name");
      }
      if (null == db.getLocationUri()) {
        db.setLocationUri(wh.getDefaultDatabasePath(db.getName()).toString());
      } else {
        db.setLocationUri(wh.getDnsPath(new Path(db.getLocationUri())).toString());
      }
      Path dbPath = new Path(db.getLocationUri());
      boolean success = false;
      boolean madeDir = false;

      try {

        firePreEvent(new PreCreateDatabaseEvent(db, this));

        if (!wh.isDir(dbPath)) {
          if (!wh.mkdirs(dbPath)) {
            throw new MetaException("Unable to create database path " + dbPath +
                ", failed to create database " + db.getName());
          }
          madeDir = true;
        }

        ms.openTransaction();
        ms.createDatabase(db);
        success = ms.commitTransaction();
      } finally {
        if (!success) {
          ms.rollbackTransaction();
          if (madeDir) {
            wh.deleteDir(dbPath, true);
          }
        }
        for (MetaStoreEventListener listener : listeners) {
          listener.onCreateDatabase(new CreateDatabaseEvent(db, success, this));
        }
      }
    }

    public void create_database(final Database db)
        throws AlreadyExistsException, InvalidObjectException, MetaException {
      startFunction("create_database", ": " + db.toString());
      boolean success = false;
      Exception ex = null;
      try {
        try {
          if (null != get_database(db.getName())) {
            throw new AlreadyExistsException("Database " + db.getName() + " already exists");
          }
        } catch (NoSuchObjectException e) {
          // expected
        }

        create_database_core(getMS(), db);
        success = true;
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof InvalidObjectException) {
          throw (InvalidObjectException) e;
        } else if (e instanceof AlreadyExistsException) {
          throw (AlreadyExistsException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("create_database", success, ex);
      }
    }

    public Database get_database(final String name) throws NoSuchObjectException,
        MetaException {
      startFunction("get_database", ": " + name);
      Database db = null;
      Exception ex = null;
      try {
        db = getMS().getDatabase(name);
      } catch (MetaException e) {
        ex = e;
        throw e;
      } catch (NoSuchObjectException e) {
        ex = e;
        throw e;
      } catch (Exception e) {
        ex = e;
        assert (e instanceof RuntimeException);
        throw (RuntimeException) e;
      } finally {
        endFunction("get_database", db != null, ex);
      }
      return db;
    }

    public void alter_database(final String dbName, final Database db)
        throws NoSuchObjectException, TException, MetaException {
      startFunction("alter_database" + dbName);
      boolean success = false;
      Exception ex = null;
      try {
        getMS().alterDatabase(dbName, db);
        success = true;
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof NoSuchObjectException) {
          throw (NoSuchObjectException) e;
        } else if (e instanceof TException) {
          throw (TException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("alter_database", success, ex);
      }
    }

    private void drop_database_core(RawStore ms,
        final String name, final boolean deleteData, final boolean cascade)
        throws NoSuchObjectException, InvalidOperationException, MetaException,
        IOException, InvalidObjectException, InvalidInputException {
      boolean success = false;
      Database db = null;
      List<Path> tablePaths = new ArrayList<Path>();
      List<Path> partitionPaths = new ArrayList<Path>();
      try {
        ms.openTransaction();
        db = ms.getDatabase(name);

        firePreEvent(new PreDropDatabaseEvent(db, this));

        List<String> allTables = get_all_tables(db.getName());
        if (!cascade && !allTables.isEmpty()) {
          throw new InvalidOperationException("Database " + db.getName() + " is not empty");
        }
        Path path = new Path(db.getLocationUri()).getParent();
        if (!wh.isWritable(path)) {
          throw new MetaException("Database not dropped since " +
              path + " is not writable by " +
              hiveConf.getUser());
        }

        Path databasePath = wh.getDnsPath(wh.getDatabasePath(db));

        // first drop tables
        int tableBatchSize = HiveConf.getIntVar(hiveConf,
            ConfVars.METASTORE_BATCH_RETRIEVE_MAX);

        int startIndex = 0;
        int endIndex = -1;
        // retrieve the tables from the metastore in batches to alleviate memory constraints
        while (endIndex < allTables.size() - 1) {
          startIndex = endIndex + 1;
          endIndex = endIndex + tableBatchSize;
          if (endIndex >= allTables.size()) {
            endIndex = allTables.size() - 1;
          }

          List<Table> tables = null;
          try {
            tables = ms.getTableObjectsByName(name, allTables.subList(startIndex, endIndex));
          } catch (UnknownDBException e) {
            throw new MetaException(e.getMessage());
          }

          if (tables != null && !tables.isEmpty()) {
            for (Table table : tables) {

              // If the table is not external and it might not be in a subdirectory of the database
              // add it's locations to the list of paths to delete
              Path tablePath = null;
              if (table.getSd().getLocation() != null && !isExternal(table)) {
                tablePath = wh.getDnsPath(new Path(table.getSd().getLocation()));
                if (!wh.isWritable(tablePath.getParent())) {
                  throw new MetaException("Database metadata not deleted since table: " +
                      table.getTableName() + " has a parent location " + tablePath.getParent() +
                      " which is not writable by " + hiveConf.getUser());
                }

                if (!isSubdirectory(databasePath, tablePath)) {
                  tablePaths.add(tablePath);
                }
              }

              // For each partition in each table, drop the partitions and get a list of
              // partitions' locations which might need to be deleted
              partitionPaths = dropPartitionsAndGetLocations(ms, name, table.getTableName(),
                  tablePath, table.getPartitionKeys(), deleteData && !isExternal(table));

              // Drop the table but not its data
              drop_table(name, table.getTableName(), false);
            }
          }
        }

        if (ms.dropDatabase(name)) {
          success = ms.commitTransaction();
        }
      } finally {
        if (!success) {
          ms.rollbackTransaction();
        } else if (deleteData) {
          // Delete the data in the partitions which have other locations
          deletePartitionData(partitionPaths);
          // Delete the data in the tables which have other locations
          for (Path tablePath : tablePaths) {
            deleteTableData(tablePath);
          }
          // Delete the data in the database
          try {
            wh.deleteDir(new Path(db.getLocationUri()), true);
          } catch (Exception e) {
            LOG.error("Failed to delete database directory: " + db.getLocationUri() +
                " " + e.getMessage());
          }
          // it is not a terrible thing even if the data is not deleted
        }
        for (MetaStoreEventListener listener : listeners) {
          listener.onDropDatabase(new DropDatabaseEvent(db, success, this));
        }
      }
    }

    /**
     * Returns a BEST GUESS as to whether or not other is a subdirectory of parent. It does not
     * take into account any intricacies of the underlying file system, which is assumed to be
     * HDFS. This should not return any false positives, but may return false negatives.
     *
     * @param parent
     * @param other
     * @return
     */
    private boolean isSubdirectory(Path parent, Path other) {
      return other.toString().startsWith(parent.toString().endsWith(Path.SEPARATOR) ?
          parent.toString() : parent.toString() + Path.SEPARATOR);
    }

    public void drop_database(final String dbName, final boolean deleteData, final boolean cascade)
        throws NoSuchObjectException, InvalidOperationException, MetaException {

      startFunction("drop_database", ": " + dbName);
      if (DEFAULT_DATABASE_NAME.equalsIgnoreCase(dbName)) {
        endFunction("drop_database", false, null);
        throw new MetaException("Can not drop default database");
      }

      boolean success = false;
      Exception ex = null;
      try {
        drop_database_core(getMS(), dbName, deleteData, cascade);
        success = true;
      } catch (IOException e) {
        ex = e;
        throw new MetaException(e.getMessage());
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof InvalidOperationException) {
          throw (InvalidOperationException) e;
        } else if (e instanceof NoSuchObjectException) {
          throw (NoSuchObjectException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("drop_database", success, ex);
      }
    }

    public List<String> get_databases(final String pattern) throws MetaException {
      startFunction("get_databases", ": " + pattern);

      List<String> ret = null;
      Exception ex = null;
      try {
        ret = getMS().getDatabases(pattern);
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("get_databases", ret != null, ex);
      }
      return ret;
    }

    public List<String> get_all_databases() throws MetaException {
      startFunction("get_all_databases");

      List<String> ret = null;
      Exception ex = null;
      try {
        ret = getMS().getAllDatabases();
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("get_all_databases", ret != null, ex);
      }
      return ret;
    }

    private void create_type_core(final RawStore ms, final Type type)
        throws AlreadyExistsException, MetaException, InvalidObjectException {
      if (!MetaStoreUtils.validateName(type.getName())) {
        throw new InvalidObjectException("Invalid type name");
      }

      boolean success = false;
      try {
        ms.openTransaction();
        if (is_type_exists(ms, type.getName())) {
          throw new AlreadyExistsException("Type " + type.getName() + " already exists");
        }
        ms.createType(type);
        success = ms.commitTransaction();
      } finally {
        if (!success) {
          ms.rollbackTransaction();
        }
      }
    }

    public boolean create_type(final Type type) throws AlreadyExistsException,
        MetaException, InvalidObjectException {
      startFunction("create_type", ": " + type.toString());
      boolean success = false;
      Exception ex = null;
      try {
        create_type_core(getMS(), type);
        success = true;
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof InvalidObjectException) {
          throw (InvalidObjectException) e;
        } else if (e instanceof AlreadyExistsException) {
          throw (AlreadyExistsException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("create_type", success, ex);
      }

      return success;
    }

    public Type get_type(final String name) throws MetaException, NoSuchObjectException {
      startFunction("get_type", ": " + name);

      Type ret = null;
      Exception ex = null;
      try {
        ret = getMS().getType(name);
        if (null == ret) {
          throw new NoSuchObjectException("Type \"" + name + "\" not found.");
        }
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof NoSuchObjectException) {
          throw (NoSuchObjectException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("get_type", ret != null, ex);
      }
      return ret;
    }

    private boolean is_type_exists(RawStore ms, String typeName)
        throws MetaException {
      return (ms.getType(typeName) != null);
    }

    private void drop_type_core(final RawStore ms, String typeName)
        throws NoSuchObjectException, MetaException {
      boolean success = false;
      try {
        ms.openTransaction();
        // drop any partitions
        if (!is_type_exists(ms, typeName)) {
          throw new NoSuchObjectException(typeName + " doesn't exist");
        }
        if (!ms.dropType(typeName)) {
          throw new MetaException("Unable to drop type " + typeName);
        }
        success = ms.commitTransaction();
      } finally {
        if (!success) {
          ms.rollbackTransaction();
        }
      }
    }

    public boolean drop_type(final String name) throws MetaException, NoSuchObjectException {
      startFunction("drop_type", ": " + name);

      boolean success = false;
      Exception ex = null;
      try {
        // TODO:pc validate that there are no types that refer to this
        success = getMS().dropType(name);
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof NoSuchObjectException) {
          throw (NoSuchObjectException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("drop_type", success, ex);
      }
      return success;
    }

    public Map<String, Type> get_type_all(String name) throws MetaException {
      // TODO Auto-generated method stub
      startFunction("get_type_all", ": " + name);
      endFunction("get_type_all", false, null);
      throw new MetaException("Not yet implemented");
    }

    private void create_table_core(final RawStore ms, final Table tbl,
        final EnvironmentContext envContext)
        throws AlreadyExistsException, MetaException,
        InvalidObjectException, NoSuchObjectException {

      if (!MetaStoreUtils.validateName(tbl.getTableName())
          || !MetaStoreUtils.validateColNames(tbl.getSd().getCols())
          || (tbl.getPartitionKeys() != null && !MetaStoreUtils
              .validateColNames(tbl.getPartitionKeys()))
          || !MetaStoreUtils.validateSkewedColNames(
              (null == tbl.getSd().getSkewedInfo()) ?
                  null : tbl.getSd().getSkewedInfo().getSkewedColNames())
          || !MetaStoreUtils.validateSkewedColNamesSubsetCol(
              (null == tbl.getSd().getSkewedInfo()) ?
                  null : tbl.getSd().getSkewedInfo().getSkewedColNames(),
              tbl.getSd().getCols())) {
        throw new InvalidObjectException(tbl.getTableName()
            + " is not a valid object name");
      }

      Path tblPath = null;
      boolean success = false, madeDir = false;
      try {
        firePreEvent(new PreCreateTableEvent(tbl, this));

        ms.openTransaction();

        if (ms.getDatabase(tbl.getDbName()) == null) {
          throw new NoSuchObjectException("The database " + tbl.getDbName() + " does not exist");
        }

        // get_table checks whether database exists, it should be moved here
        if (is_table_exists(ms, tbl.getDbName(), tbl.getTableName())) {
          throw new AlreadyExistsException("Table " + tbl.getTableName()
              + " already exists");
        }

        if (!TableType.VIRTUAL_VIEW.toString().equals(tbl.getTableType())) {
          if (tbl.getSd().getLocation() == null
              || tbl.getSd().getLocation().isEmpty()) {
            tblPath = wh.getTablePath(
                ms.getDatabase(tbl.getDbName()), tbl.getTableName());
          } else {
            if (!isExternal(tbl) && !MetaStoreUtils.isNonNativeTable(tbl)) {
              LOG.warn("Location: " + tbl.getSd().getLocation()
                  + " specified for non-external table:" + tbl.getTableName());
            }
            tblPath = wh.getDnsPath(new Path(tbl.getSd().getLocation()));
          }
          tbl.getSd().setLocation(tblPath.toString());
        }

        if (tblPath != null) {
          if (!wh.isDir(tblPath)) {
            if (!wh.mkdirs(tblPath)) {
              throw new MetaException(tblPath
                  + " is not a directory or unable to create one");
            }
            madeDir = true;
          }
        }

        // set create time
        long time = System.currentTimeMillis() / 1000;
        tbl.setCreateTime((int) time);
        if (tbl.getParameters() == null ||
            tbl.getParameters().get(hive_metastoreConstants.DDL_TIME) == null) {
          tbl.putToParameters(hive_metastoreConstants.DDL_TIME, Long.toString(time));
        }
        ms.createTable(tbl);
        success = ms.commitTransaction();

        this.createBusiTypeDC(ms, tbl);

      } finally {
        if (!success) {
          ms.rollbackTransaction();
          if (madeDir) {
            wh.deleteDir(tblPath, true);
          }
        }
        for (MetaStoreEventListener listener : listeners) {
          CreateTableEvent createTableEvent =
              new CreateTableEvent(tbl, success, this);
          createTableEvent.setEnvironmentContext(envContext);
          listener.onCreateTable(createTableEvent);
        }
      }
    }

    private void createBusiTypeDC(final RawStore ms,Table tbl) throws MetaException{
      for(FieldSchema f : tbl.getSd().getCols()){
        String cmet = f.getComment();
        if(cmet != null && cmet.indexOf(MetaStoreUtils.BUSI_TYPES_PREFIX)>=0){
          int pos = cmet.indexOf(MetaStoreUtils.BUSI_TYPES_PREFIX);// ip/tel/time/content
          for(String type : MetaStoreUtils.BUSI_TYPES){
            if( cmet.length() - pos >= type.length()
                && type.equals(cmet.substring(pos,type.length()).toLowerCase())){
              try {
                BusiTypeDatacenter busiTypeDatacenter = new BusiTypeDatacenter(type,ms.getDatabase(tbl.getDbName()).getDatacenter(),tbl.getDbName());
                if(!isTopDc()){
                  if(topdcli != null){
                    topdcli.append_busi_type_datacenter(busiTypeDatacenter);
                  }else{
                    throw new MetaException("Top datacenter is not reachable!");
                  }
                }else{
                  append_busi_type_datacenter(busiTypeDatacenter);
                }
              } catch (Exception e) {
                LOG.error(e,e);
              }
            }
          }
        }
      }
    }

    @Override
    public void create_table(final Table tbl) throws AlreadyExistsException,
        MetaException, InvalidObjectException {
      create_table(tbl, null);
    }

    @Override
    public void create_table_with_environment_context(final Table table,
        final EnvironmentContext envContext)
        throws AlreadyExistsException, MetaException, InvalidObjectException {
      create_table(table, envContext);
    }

    private void create_table(final Table tbl,
        final EnvironmentContext envContext) throws AlreadyExistsException,
        MetaException, InvalidObjectException {
      startFunction("create_table", ": " + tbl.toString());

      LOG.warn("----zjw--creating table");
      boolean success = false;
      Exception ex = null;
      try {
        create_table_core(getMS(), tbl, envContext);
        success = true;
      } catch (NoSuchObjectException e) {
        ex = e;
        throw new InvalidObjectException(e.getMessage());
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof InvalidObjectException) {
          throw (InvalidObjectException) e;
        } else if (e instanceof AlreadyExistsException) {
          throw (AlreadyExistsException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("create_table", success, ex);
      }
    }

    private boolean is_table_exists(RawStore ms, String dbname, String name)
        throws MetaException {
      return (ms.getTable(dbname, name) != null);
    }

    private void drop_table_core(final RawStore ms, final String dbname, final String name,
      final boolean deleteData) throws NoSuchObjectException, MetaException, IOException,
      InvalidObjectException, InvalidInputException {
      boolean success = false;
      boolean isExternal = false;
      Path tblPath = null;
      List<Path> partPaths = null;
      Table tbl = null;
      isExternal = false;
      boolean isIndexTable = false;
      try {
        ms.openTransaction();
        // drop any partitions
        tbl = get_table(dbname, name);
        if (tbl == null) {
          throw new NoSuchObjectException(name + " doesn't exist");
        }
        if (tbl.getSd() == null) {
          throw new MetaException("Table metadata is corrupted");
        }

        firePreEvent(new PreDropTableEvent(tbl, this));

        isIndexTable = isIndexTable(tbl);
        if (isIndexTable) {
          throw new RuntimeException(
              "The table " + name + " is an index table. Please do drop index instead.");
        }

        if (!isIndexTable) {
          try {
            List<Index> indexes = ms.getIndexes(dbname, name, Short.MAX_VALUE);
            while (indexes != null && indexes.size() > 0) {
              for (Index idx : indexes) {
                this.drop_index_by_name(dbname, name, idx.getIndexName(), true);
              }
              indexes = ms.getIndexes(dbname, name, Short.MAX_VALUE);
            }
          } catch (TException e) {
            throw new MetaException(e.getMessage());
          }
        }
        isExternal = isExternal(tbl);
        if (tbl.getSd().getLocation() != null) {
          tblPath = new Path(tbl.getSd().getLocation());
          if (!wh.isWritable(tblPath.getParent())) {
            throw new MetaException("Table metadata not deleted since " +
                tblPath.getParent() + " is not writable by " +
                hiveConf.getUser());
          }
        }

        // Drop the partitions and get a list of locations which need to be deleted
        partPaths = dropPartitionsAndGetLocations(ms, dbname, name, tblPath,
            tbl.getPartitionKeys(), deleteData && !isExternal);

        if (!ms.dropTable(dbname, name)) {
          throw new MetaException("Unable to drop table");
        }
        success = ms.commitTransaction();
      } finally {
        if (!success) {
          ms.rollbackTransaction();
        } else if (deleteData && !isExternal) {
          // Delete the data in the partitions which have other locations
          deletePartitionData(partPaths);
          // Delete the data in the table
          deleteTableData(tblPath);
          // ok even if the data is not deleted
        }
        for (MetaStoreEventListener listener : listeners) {
          listener.onDropTable(new DropTableEvent(tbl, success, this));
        }
      }
    }

    /**
     * Deletes the data in a table's location, if it fails logs an error
     *
     * @param tablePath
     */
    private void deleteTableData(Path tablePath) {
      if (tablePath != null) {
        try {
          wh.deleteDir(tablePath, true);
        } catch (Exception e) {
          LOG.error("Failed to delete table directory: " + tablePath +
              " " + e.getMessage());
        }
      }
    }

    /**
     * Give a list of partitions' locations, tries to delete each one
     * and for each that fails logs an error.
     *
     * @param partPaths
     */
    private void deletePartitionData(List<Path> partPaths) {
      if (partPaths != null && !partPaths.isEmpty()) {
        for (Path partPath : partPaths) {
          try {
            wh.deleteDir(partPath, true);
          } catch (Exception e) {
            LOG.error("Failed to delete partition directory: " + partPath +
                " " + e.getMessage());
          }
        }
      }
    }

    /**
     * Retrieves the partitions specified by partitionKeys. If checkLocation, for locations of
     * partitions which may not be subdirectories of tablePath checks to make the locations are
     * writable.
     *
     * Drops the metadata for each partition.
     *
     * Provides a list of locations of partitions which may not be subdirectories of tablePath.
     *
     * @param ms
     * @param dbName
     * @param tableName
     * @param tablePath
     * @param partitionKeys
     * @param checkLocation
     * @return
     * @throws MetaException
     * @throws IOException
     * @throws InvalidInputException
     * @throws InvalidObjectException
     * @throws NoSuchObjectException
     */
    private List<Path> dropPartitionsAndGetLocations(RawStore ms, String dbName,
      String tableName, Path tablePath, List<FieldSchema> partitionKeys, boolean checkLocation)
      throws MetaException, IOException, NoSuchObjectException, InvalidObjectException,
      InvalidInputException {
      int partitionBatchSize = HiveConf.getIntVar(hiveConf,
          ConfVars.METASTORE_BATCH_RETRIEVE_MAX);
      Path tableDnsPath = null;
      if (tablePath != null) {
        tableDnsPath = wh.getDnsPath(tablePath);
      }
      List<Path> partPaths = new ArrayList<Path>();

      // call dropPartition on each of the table's partitions to follow the
      // procedure for cleanly dropping partitions.
      while (true) {
        List<Partition> partsToDelete = ms.getPartitions(dbName, tableName, partitionBatchSize);
        if (partsToDelete == null || partsToDelete.isEmpty()) {
          break;
        }
        for (Partition part : partsToDelete) {
          if (checkLocation && part.getSd() != null &&
              part.getSd().getLocation() != null) {

            Path partPath = wh.getDnsPath(new Path(part.getSd().getLocation()));
            if (tableDnsPath == null ||
                (partPath != null && !isSubdirectory(tableDnsPath, partPath))) {
              if (!wh.isWritable(partPath.getParent())) {
                throw new MetaException("Table metadata not deleted since the partition " +
                    Warehouse.makePartName(partitionKeys, part.getValues()) +
                    " has parent location " + partPath.getParent() + " which is not writable " +
                    "by " + hiveConf.getUser());
              }
              partPaths.add(partPath);
            }
          }
          ms.dropPartition(dbName, tableName, part.getValues());
        }
      }

      return partPaths;
    }

    public void drop_table(final String dbname, final String name, final boolean deleteData)
        throws NoSuchObjectException, MetaException {
      startTableFunction("drop_table", dbname, name);

      boolean success = false;
      Exception ex = null;
      try {
        drop_table_core(getMS(), dbname, name, deleteData);
        success = true;
      } catch (IOException e) {
        ex = e;
        throw new MetaException(e.getMessage());
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof NoSuchObjectException) {
          throw (NoSuchObjectException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("drop_table", success, ex);
      }

    }

    /**
     * Is this an external table?
     *
     * @param table
     *          Check if this table is external.
     * @return True if the table is external, otherwise false.
     */
    private boolean isExternal(Table table) {
      return MetaStoreUtils.isExternalTable(table);
    }

    private boolean isIndexTable(Table table) {
      return MetaStoreUtils.isIndexTable(table);
    }

    public Table get_table(final String dbname, final String name) throws MetaException,
        NoSuchObjectException {
      Table t = null;
      startTableFunction("get_table", dbname, name);
      Exception ex = null;
      try {
        t = getMS().getTable(dbname, name);
        if (t == null) {
          throw new NoSuchObjectException(dbname + "." + name
              + " table not found");
        }
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof NoSuchObjectException) {
          throw (NoSuchObjectException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("get_table", t != null, ex);
      }
      return t;
    }

    /**
     * Gets multiple tables from the hive metastore.
     *
     * @param dbname
     *          The name of the database in which the tables reside
     * @param names
     *          The names of the tables to get.
     *
     * @return A list of tables whose names are in the the list "names" and
     *         are retrievable from the database specified by "dbnames."
     *         There is no guarantee of the order of the returned tables.
     *         If there are duplicate names, only one instance of the table will be returned.
     * @throws MetaException
     * @throws InvalidOperationException
     * @throws UnknownDBException
     */
    public List<Table> get_table_objects_by_name(final String dbname, final List<String> names)
        throws MetaException, InvalidOperationException, UnknownDBException {
      List<Table> tables = null;
      startMultiTableFunction("get_multi_table", dbname, names);
      Exception ex = null;
      try {

        if (dbname == null || dbname.isEmpty()) {
          throw new UnknownDBException("DB name is null or empty");
        }
        if (names == null)
        {
          throw new InvalidOperationException(dbname + " cannot find null tables");
        }
        tables = getMS().getTableObjectsByName(dbname, names);
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof InvalidOperationException) {
          throw (InvalidOperationException) e;
        } else if (e instanceof UnknownDBException) {
          throw (UnknownDBException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("get_multi_table", tables != null, ex);
      }
      return tables;
    }

    @Override
    public List<String> get_table_names_by_filter(
        final String dbName, final String filter, final short maxTables)
        throws MetaException, InvalidOperationException, UnknownDBException {
      List<String> tables = null;
      startFunction("get_table_names_by_filter", ": db = " + dbName + ", filter = " + filter);
      Exception ex = null;
      try {
        if (dbName == null || dbName.isEmpty()) {
          throw new UnknownDBException("DB name is null or empty");
        }
        if (filter == null) {
          throw new InvalidOperationException(filter + " cannot apply null filter");
        }
        tables = getMS().listTableNamesByFilter(dbName, filter, maxTables);
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof InvalidOperationException) {
          throw (InvalidOperationException) e;
        } else if (e instanceof UnknownDBException) {
          throw (UnknownDBException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("get_table_names_by_filter", tables != null, ex);
      }
      return tables;
    }

    public boolean set_table_parameters(String dbname, String name,
        Map<String, String> params) throws NoSuchObjectException, MetaException {
      endFunction(startTableFunction("set_table_parameters", dbname, name), false, null);
      // TODO Auto-generated method stub
      return false;
    }

    private Partition append_partition_common(RawStore ms, String dbName, String tableName,
        List<String> part_vals) throws InvalidObjectException, AlreadyExistsException,
        MetaException {

      Partition part = new Partition();
      boolean success = false, madeDir = false;
      Path partLocation = null;
      Table tbl = null;
      try {
        ms.openTransaction();
        part.setDbName(dbName);
        part.setTableName(tableName);
        part.setValues(part_vals);

        PreAddPartitionEvent event = new PreAddPartitionEvent(part, this);
        firePreEvent(event);

        tbl = ms.getTable(part.getDbName(), part.getTableName());
        if (tbl == null) {
          throw new InvalidObjectException(
              "Unable to add partition because table or database do not exist");
        }
        if (tbl.getSd().getLocation() == null) {
          throw new MetaException(
              "Cannot append a partition to a view");
        }

        part.setSd(tbl.getSd());
        partLocation = new Path(tbl.getSd().getLocation(), Warehouse
            .makePartName(tbl.getPartitionKeys(), part_vals));
        part.getSd().setLocation(partLocation.toString());

        Partition old_part = null;
        try {
          // TODO: fix it
          old_part = ms.getPartition(part.getDbName(), part
              .getTableName(), part.getValues());
        } catch (NoSuchObjectException e) {
          // this means there is no existing partition
          old_part = null;
        }
        if (old_part != null) {
          throw new AlreadyExistsException("Partition already exists:" + part);
        }

        if (!wh.isDir(partLocation)) {
          if (!wh.mkdirs(partLocation)) {
            throw new MetaException(partLocation
                + " is not a directory or unable to create one");
          }
          madeDir = true;
        }

        // set create time
        long time = System.currentTimeMillis() / 1000;
        part.setCreateTime((int) time);
        part.putToParameters(hive_metastoreConstants.DDL_TIME, Long.toString(time));

        success = ms.addPartition(part);
        if (success) {
          success = ms.commitTransaction();
        }
      } finally {
        if (!success) {
          ms.rollbackTransaction();
          if (madeDir) {
            wh.deleteDir(partLocation, true);
          }
        }

        for (MetaStoreEventListener listener : listeners) {
          AddPartitionEvent addPartitionEvent =
              new AddPartitionEvent(tbl, part, success, this);
          listener.onAddPartition(addPartitionEvent);
        }
      }
      return part;
    }

    private void firePreEvent(PreEventContext event) throws MetaException {
      for (MetaStorePreEventListener listener : preListeners) {
        try {
          listener.onEvent(event);
        } catch (NoSuchObjectException e) {
          throw new MetaException(e.getMessage());
        } catch (InvalidOperationException e) {
          throw new MetaException(e.getMessage());
        }
      }
    }

    public Partition append_partition(final String dbName, final String tableName,
        final List<String> part_vals) throws InvalidObjectException,
        AlreadyExistsException, MetaException {
      startPartitionFunction("append_partition", dbName, tableName, part_vals);
      if (LOG.isDebugEnabled()) {
        for (String part : part_vals) {
          LOG.debug(part);
        }
      }

      Partition ret = null;
      Exception ex = null;
      try {
        ret = append_partition_common(getMS(), dbName, tableName, part_vals);
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof InvalidObjectException) {
          throw (InvalidObjectException) e;
        } else if (e instanceof AlreadyExistsException) {
          throw (AlreadyExistsException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("append_partition", ret != null, ex);
      }
      return ret;
    }

    private int add_partitions_core(final RawStore ms, final List<Partition> parts)
        throws MetaException, InvalidObjectException, AlreadyExistsException {
      String db = parts.get(0).getDbName();
      String tbl = parts.get(0).getTableName();
      logInfo("add_partitions : db=" + db + " tbl=" + tbl);

      boolean success = false;
      Map<Partition, Boolean> addedPartitions = new HashMap<Partition, Boolean>();
      try {
        ms.openTransaction();
        for (Partition part : parts) {
          // No environment context is passed.
          Entry<Partition, Boolean> e = add_partition_core_notxn(ms, part, null);
          addedPartitions.put(e.getKey(), e.getValue());
        }
        success = true;
        success = ms.commitTransaction();
      } finally {
        if (!success) {
          ms.rollbackTransaction();
          for (Entry<Partition, Boolean> e : addedPartitions.entrySet()) {
            if (e.getValue()) {
              wh.deleteDir(new Path(e.getKey().getSd().getLocation()), true);
              // we just created this directory - it's not a case of pre-creation, so we nuke
            }
          }
        }
      }
      return parts.size();
    }

    public int add_partitions(final List<Partition> parts) throws MetaException,
        InvalidObjectException, AlreadyExistsException {
      startFunction("add_partition");
      if (parts.size() == 0) {
        return 0;
      }

      Integer ret = null;
      Exception ex = null;
      try {
        ret = add_partitions_core(getMS(), parts);
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof InvalidObjectException) {
          throw (InvalidObjectException) e;
        } else if (e instanceof AlreadyExistsException) {
          throw (AlreadyExistsException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("add_partition", ret != null, ex);
      }
      return ret;
    }


    private PartitionDefinition createSubpartitions(Table tab,Partition part, String part_name) throws MetaException{
      List<PartitionInfo> pis = PartitionInfo.getPartitionInfo(tab.getPartitionKeys());
      PartitionDefinition global_sub_pd = null;
      if( pis != null && pis.size() > 0){
        if(pis.get(0).getP_type() != PartitionType.interval){
          throw new MetaException("Add partition operation only supprots interval partition.");
        }
        else if(pis.size() == 2 ){
          if(part.getValues().size() ==1){
            try{
              long f_value = Long.parseLong(part.getValues().get(0));
              Double d = Double.parseDouble(pis.get(0).getArgs().get(1));
              long interval = f_value + PartitionFactory.getIntervalSeconds(pis.get(0).getArgs().get(0),d)-1;
              part.getValues().add(""+interval);
            }catch(Exception e){
              LOG.error(e,e);
            }
          }
          global_sub_pd = new PartitionDefinition();
          global_sub_pd.setPi(pis.get(1));
          global_sub_pd.setDbName(tab.getDbName());
          global_sub_pd.setTableName(tab.getTableName());
          global_sub_pd.getPi().setP_level(2);//设为2级分区
          PartitionFactory.createSubPartition(tab.getPartitionKeys(),global_sub_pd,false,part_name);
        }
      }
      return global_sub_pd;
    }

    /**
     * An implementation of add_partition_core that does not commit
     * transaction or rollback transaction as part of its operation
     * - it is assumed that will be tended to from outside this call
     *
     * @param ms
     * @param part
     * @param envContext
     *          parameters passed by the client
     * @return
     * @throws InvalidObjectException
     * @throws AlreadyExistsException
     * @throws MetaException
     */
    private Entry<Partition, Boolean> add_partition_core_notxn(
        final RawStore ms, final Partition part,
        final EnvironmentContext envContext)
        throws InvalidObjectException, AlreadyExistsException, MetaException {
      boolean success = false, madeDir = false;
      Path partLocation = null;
      Table tbl = null;
      try {
        firePreEvent(new PreAddPartitionEvent(part, this));

        Partition old_part = null;
        try {
          old_part = ms.getPartition(part.getDbName(), part
              .getTableName(), part.getPartitionName());
        } catch (NoSuchObjectException e) {
          // this means there is no existing partition
          old_part = null;
        }
        if (old_part != null) {
          throw new AlreadyExistsException("Partition already exists:" + part);
        }
        if(part.getPartitionName() == null || part.getPartitionName().isEmpty()){
          throw new AlreadyExistsException("Partition name not identified!");
        }else{
          String pn = part.getPartitionName();
          for(int i=0 ; i < pn.length(); i++){
            if(pn.charAt(i) == '\t' || pn.charAt(i) == '\n' || pn.charAt(i) == ' ') {
              throw new AlreadyExistsException("Partition name "+part.getPartitionName()+" not valid,contains space/tab/\n character!");
            }
          }
        }
        tbl = ms.getTable(part.getDbName(), part.getTableName());
        if (tbl == null) {
          throw new InvalidObjectException(
              "Unable to add partition because table or database do not exist");
        }
        LOG.warn("---zjw-- int add_partition_core_notxn.");
        PartitionDefinition global_sub_pd = this.createSubpartitions(tbl,part,part.getPartitionName());
        if(global_sub_pd != null){
          List<Subpartition> subpartitions = global_sub_pd.toSubpartitionList();
          LOG.warn("---zjw-- subpartitions size:"+subpartitions.size());
          part.setSubpartitions(subpartitions);
        }

        String partLocationStr = null;
        if (part.getSd() != null) {
          partLocationStr = part.getSd().getLocation();
        }

        if (partLocationStr == null || partLocationStr.isEmpty()) {
          // set default location if not specified and this is
          // a physical table partition (not a view)
          if (tbl.getSd().getLocation() != null) {
            partLocation = new Path(tbl.getSd().getLocation(), Warehouse
                .makePartName(tbl.getPartitionKeys(), part.getValues()));
          }

        } else {
          if (tbl.getSd().getLocation() == null) {
            throw new MetaException(
                "Cannot specify location for a view partition");
          }
          partLocation = wh.getDnsPath(new Path(partLocationStr));
        }
        LOG.info("---zjw-- before partLocation.");
        if (partLocation != null && part.getSd() != null) {
          part.getSd().setLocation(partLocation.toString());


          // Check to see if the directory already exists before calling
          // mkdirs() because if the file system is read-only, mkdirs will
          // throw an exception even if the directory already exists.
          if (!wh.isDir(partLocation)) {
            if (!wh.mkdirs(partLocation)) {
              throw new MetaException(partLocation
                  + " is not a directory or unable to create one");
            }
            madeDir = true;
          }
        }

        // set create time
        long time = System.currentTimeMillis() / 1000;
        part.setCreateTime((int) time);
        if (part.getParameters() == null ||
            part.getParameters().get(hive_metastoreConstants.DDL_TIME) == null) {
          part.putToParameters(hive_metastoreConstants.DDL_TIME, Long.toString(time));
        }

        LOG.info("---zjw-- before getParameters.");

        // Inherit table properties into partition properties.
        Map<String, String> tblParams = tbl.getParameters();
        String inheritProps = hiveConf.getVar(ConfVars.METASTORE_PART_INHERIT_TBL_PROPS).trim();
        // Default value is empty string in which case no properties will be inherited.
        // * implies all properties needs to be inherited
        Set<String> inheritKeys = new HashSet<String>(Arrays.asList(inheritProps.split(",")));
        if (inheritKeys.contains("*")) {
          inheritKeys = tblParams.keySet();
        }

        for (String key : inheritKeys) {
          String paramVal = tblParams.get(key);
          if (null != paramVal) { // add the property only if it exists in table properties
            part.putToParameters(key, paramVal);
          }
        }
        LOG.info("---zjw-- before addPartition.");
        success = ms.addPartition(part);

      } finally {
        if (!success) {
          if (madeDir) {
            wh.deleteDir(partLocation, true);
          }
        }
        for (MetaStoreEventListener listener : listeners) {
          AddPartitionEvent addPartitionEvent =
              new AddPartitionEvent(tbl, part, success, this);
          addPartitionEvent.setEnvironmentContext(envContext);
          listener.onAddPartition(addPartitionEvent);
        }
      }
      Map<Partition, Boolean> returnVal = new HashMap<Partition, Boolean>();
      returnVal.put(part, madeDir);
      return returnVal.entrySet().iterator().next();
    }

    private Partition add_partition_core(final RawStore ms,
        final Partition part, final EnvironmentContext envContext)
        throws InvalidObjectException, AlreadyExistsException, MetaException {
      boolean success = false;
      Partition retPtn = null;
      try {
        ms.openTransaction();
        retPtn = add_partition_core_notxn(ms, part, envContext).getKey();
        // we proceed only if we'd actually succeeded anyway, otherwise,
        // we'd have thrown an exception
        success = ms.commitTransaction();
      } finally {
        if (!success) {
          ms.rollbackTransaction();
        }
      }
      return retPtn;
    }

    @Override
    public Partition add_partition(final Partition part)
        throws InvalidObjectException, AlreadyExistsException, MetaException {
      return add_partition(part, null);
    }

    @Override
    public Partition add_partition_with_environment_context(
        final Partition part, EnvironmentContext envContext)
        throws InvalidObjectException, AlreadyExistsException,
        MetaException {
      return add_partition(part, envContext);
    }

    private Partition add_partition(final Partition part,
        final EnvironmentContext envContext) throws InvalidObjectException,
        AlreadyExistsException, MetaException {
      startTableFunction("add_partition",
          part.getDbName(), part.getTableName());
      Partition ret = null;
      Exception ex = null;
      try {
        ret = add_partition_core(getMS(), part, envContext);
      } catch (Exception e) {
        LOG.error(e,e);
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof InvalidObjectException) {
          throw (InvalidObjectException) e;
        } else if (e instanceof AlreadyExistsException) {
          throw (AlreadyExistsException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("add_partition", ret != null, ex);
      }
      return ret;
    }

    private boolean drop_partition_common_by_name(RawStore ms, String db_name, String tbl_name,
        String part_name, final boolean deleteData)
        throws MetaException, NoSuchObjectException, IOException, InvalidObjectException,
        InvalidInputException {
        boolean success = false;
        Path partPath = null;
        Table tbl = null;
        Partition part = null;
        boolean isArchived = false;
        Path archiveParentDir = null;

        try {
          LOG.debug("--zjw--before drop_partition_common_by_name open ");
//          ms.openTransaction();
          LOG.debug("--zjw--after drop_partition_common_by_name open ");
          // TODO: fix it
          part = ms.getPartition(db_name, tbl_name, part_name);

          firePreEvent(new PreDropPartitionEvent(part, this));

          if (part == null) {
            throw new NoSuchObjectException("Partition doesn't exist. "
                + part_name);
          }

          isArchived = MetaStoreUtils.isArchived(part);
          if (isArchived) {
            archiveParentDir = MetaStoreUtils.getOriginalLocation(part);
            if (!wh.isWritable(archiveParentDir.getParent())) {
              throw new MetaException("Table partition not deleted since " +
                  archiveParentDir.getParent() + " is not writable by " +
                  hiveConf.getUser());
            }
          }
          if (!ms.dropPartition(db_name, tbl_name, part_name)) {
            throw new MetaException("Unable to drop partition");
          }

          LOG.debug("--zjw--before drop_partition_common_by_name cmt open=");
//          success = ms.commitTransaction();
          LOG.debug("--zjw--after drop_partition_common_by_name cmt ");
          if ((part.getSd() != null) && (part.getSd().getLocation() != null)) {
            partPath = new Path(part.getSd().getLocation());
            if (!wh.isWritable(partPath.getParent())) {
              throw new MetaException("Table partition not deleted since " +
                  partPath.getParent() + " is not writable by " +
                  hiveConf.getUser());
            }
          }
          tbl = get_table(db_name, tbl_name);
        } finally {
          if (!success) {
            ms.rollbackTransaction();
          } else if (deleteData && ((partPath != null) || (archiveParentDir != null))) {
            if (tbl != null && !isExternal(tbl)) {
              // Archived partitions have har:/to_har_file as their location.
              // The original directory was saved in params
              if (isArchived) {
                assert (archiveParentDir != null);
                wh.deleteDir(archiveParentDir, true);
              } else {
                assert (partPath != null);
                wh.deleteDir(partPath, true);
              }
              // ok even if the data is not deleted
            }
            // FIXME: delete SFiles
          }
          for (MetaStoreEventListener listener : listeners) {
            listener.onDropPartition(new DropPartitionEvent(tbl, part, success, this));
          }
        }
        return true;
      }

    private boolean drop_partition_common(RawStore ms, String db_name, String tbl_name,
      List<String> part_vals, final boolean deleteData)
      throws MetaException, NoSuchObjectException, IOException, InvalidObjectException,
      InvalidInputException {
      boolean success = false;
      Path partPath = null;
      Table tbl = null;
      Partition part = null;
      boolean isArchived = false;
      Path archiveParentDir = null;

      try {
        ms.openTransaction();
        // TODO: fix it
        part = ms.getPartition(db_name, tbl_name, part_vals);

        firePreEvent(new PreDropPartitionEvent(part, this));

        if (part == null) {
          throw new NoSuchObjectException("Partition doesn't exist. "
              + part_vals);
        }

        isArchived = MetaStoreUtils.isArchived(part);
        if (isArchived) {
          archiveParentDir = MetaStoreUtils.getOriginalLocation(part);
          if (!wh.isWritable(archiveParentDir.getParent())) {
            throw new MetaException("Table partition not deleted since " +
                archiveParentDir.getParent() + " is not writable by " +
                hiveConf.getUser());
          }
        }
        if (!ms.dropPartition(db_name, tbl_name, part_vals)) {
          throw new MetaException("Unable to drop partition");
        }
        success = ms.commitTransaction();
        if ((part.getSd() != null) && (part.getSd().getLocation() != null)) {
          partPath = new Path(part.getSd().getLocation());
          if (!wh.isWritable(partPath.getParent())) {
            throw new MetaException("Table partition not deleted since " +
                partPath.getParent() + " is not writable by " +
                hiveConf.getUser());
          }
        }
        tbl = get_table(db_name, tbl_name);
      } finally {
        if (!success) {
          ms.rollbackTransaction();
        } else if (deleteData && ((partPath != null) || (archiveParentDir != null))) {
          if (tbl != null && !isExternal(tbl)) {
            // Archived partitions have har:/to_har_file as their location.
            // The original directory was saved in params
            if (isArchived) {
              assert (archiveParentDir != null);
              wh.deleteDir(archiveParentDir, true);
            } else {
              assert (partPath != null);
              wh.deleteDir(partPath, true);
            }
            // ok even if the data is not deleted
          }
        }
        for (MetaStoreEventListener listener : listeners) {
          listener.onDropPartition(new DropPartitionEvent(tbl, part, success, this));
        }
      }
      return true;
    }

    public boolean drop_partition(final String db_name, final String tbl_name,
        final List<String> part_vals, final boolean deleteData)
        throws NoSuchObjectException, MetaException, TException {
      startPartitionFunction("drop_partition", db_name, tbl_name, part_vals);
      LOG.info("Partition values:" + part_vals);

      boolean ret = false;
      Exception ex = null;
      try {
        ret = drop_partition_common(getMS(), db_name, tbl_name, part_vals, deleteData);
      } catch (IOException e) {
        ex = e;
        throw new MetaException(e.getMessage());
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof NoSuchObjectException) {
          throw (NoSuchObjectException) e;
        } else if (e instanceof TException) {
          throw (TException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("drop_partition", ret, ex);
      }
      return ret;

    }

    public Partition get_partition(final String db_name, final String tbl_name,
        final List<String> part_vals) throws MetaException, NoSuchObjectException {
      startPartitionFunction("get_partition", db_name, tbl_name, part_vals);

      Partition ret = null;
      Exception ex = null;
      try {
        // TODO: fix it
        ret = getMS().getPartition(db_name, tbl_name, part_vals);
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof NoSuchObjectException) {
          throw (NoSuchObjectException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("get_partition", ret != null, ex);
      }
      return ret;
    }

    @Override
    public Partition get_partition_with_auth(final String db_name,
        final String tbl_name, final List<String> part_vals,
        final String user_name, final List<String> group_names)
        throws MetaException, NoSuchObjectException, TException {
      startPartitionFunction("get_partition_with_auth", db_name, tbl_name,
          part_vals);

      Partition ret = null;
      Exception ex = null;
      try {
        ret = getMS().getPartitionWithAuth(db_name, tbl_name, part_vals,
            user_name, group_names);
      } catch (InvalidObjectException e) {
        ex = e;
        throw new NoSuchObjectException(e.getMessage());
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof NoSuchObjectException) {
          throw (NoSuchObjectException) e;
        } else if (e instanceof TException) {
          throw (TException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("get_partition_with_auth", ret != null, ex);
      }
      return ret;
    }

    public List<Partition> get_partitions(final String db_name, final String tbl_name,
        final short max_parts) throws NoSuchObjectException, MetaException {
      startTableFunction("get_partitions", db_name, tbl_name);

      List<Partition> ret = null;
      Exception ex = null;
      try {
        ret = getMS().getPartitions(db_name, tbl_name, max_parts);
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof NoSuchObjectException) {
          throw (NoSuchObjectException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("get_partitions", ret != null, ex);
      }
      return ret;

    }

    @Override
    public List<Partition> get_partitions_with_auth(final String dbName,
        final String tblName, final short maxParts, final String userName,
        final List<String> groupNames) throws NoSuchObjectException,
        MetaException, TException {
      startTableFunction("get_partitions_with_auth", dbName, tblName);

      List<Partition> ret = null;
      Exception ex = null;
      try {
        ret = getMS().getPartitionsWithAuth(dbName, tblName, maxParts,
            userName, groupNames);
      } catch (InvalidObjectException e) {
        ex = e;
        throw new NoSuchObjectException(e.getMessage());
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof NoSuchObjectException) {
          throw (NoSuchObjectException) e;
        } else if (e instanceof TException) {
          throw (TException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("get_partitions_with_auth", ret != null, ex);
      }
      return ret;

    }

    public List<String> get_partition_names(final String db_name, final String tbl_name,
        final short max_parts) throws MetaException {
      startTableFunction("get_partition_names", db_name, tbl_name);

      List<String> ret = null;
      Exception ex = null;
      try {
        ret = getMS().listPartitionNames(db_name, tbl_name, max_parts);
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("get_partition_names", ret != null, ex);
      }
      return ret;
    }

    @Override
    public void alter_partition(final String db_name, final String tbl_name,
        final Partition new_part)
        throws InvalidOperationException, MetaException,
        TException {
      rename_partition(db_name, tbl_name, null, new_part);
    }

    @Override
    public void alter_partition_with_environment_context(final String dbName,
        final String tableName, final Partition newPartition,
        final EnvironmentContext envContext)
        throws InvalidOperationException, MetaException, TException {
      rename_partition(dbName, tableName, null,
          newPartition, envContext);
    }

    @Override
    public void rename_partition(final String db_name, final String tbl_name,
        final List<String> part_vals, final Partition new_part)
        throws InvalidOperationException, MetaException, TException {
      // Call rename_partition without an environment context.
      rename_partition(db_name, tbl_name, part_vals, new_part, null);
    }

    private void rename_partition(final String db_name, final String tbl_name,
        final List<String> part_vals, final Partition new_part,
        final EnvironmentContext envContext)
        throws InvalidOperationException, MetaException,
        TException {
      startTableFunction("alter_partition", db_name, tbl_name);

      if (LOG.isInfoEnabled()) {
        LOG.info("New partition values:" + new_part.getValues());
        if (part_vals != null && part_vals.size() > 0) {
          LOG.info("Old Partition values:" + part_vals);
        }
      }

      Partition oldPart = null;
      Exception ex = null;
      try {
        firePreEvent(new PreAlterPartitionEvent(db_name, tbl_name, part_vals, new_part, this));

        Table table = getMS().getTable(db_name, tbl_name);
        String partName = Warehouse
              .makePartName(table.getPartitionKeys(), part_vals);

        oldPart = alterHandler.alterPartition(getMS(), wh, db_name, tbl_name, partName, part_vals, new_part);

        for (MetaStoreEventListener listener : listeners) {
          AlterPartitionEvent alterPartitionEvent =
              new AlterPartitionEvent(oldPart, new_part, true, this);
          alterPartitionEvent.setEnvironmentContext(envContext);
          listener.onAlterPartition(alterPartitionEvent);
        }
      } catch (InvalidObjectException e) {
        ex = e;
        throw new InvalidOperationException(e.getMessage());
      } catch (AlreadyExistsException e) {
        ex = e;
        throw new InvalidOperationException(e.getMessage());
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof InvalidOperationException) {
          throw (InvalidOperationException) e;
        } else if (e instanceof TException) {
          throw (TException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("alter_partition", oldPart != null, ex);
      }
      return;
    }

    @Override
    public void alter_partitions(final String db_name, final String tbl_name,
        final List<Partition> new_parts)
        throws InvalidOperationException, MetaException,
        TException {

      startTableFunction("alter_partitions", db_name, tbl_name);

      if (LOG.isInfoEnabled()) {
        for (Partition tmpPart : new_parts) {
          LOG.info("New partition values:" + tmpPart.getValues());
        }
      }
      // all partitions are altered atomically
      // all prehooks are fired together followed by all post hooks
      List<Partition> oldParts = null;
      Exception ex = null;
      try {
        for (Partition tmpPart : new_parts) {
          try {
            for (MetaStorePreEventListener listener : preListeners) {
              listener.onEvent(
                  new PreAlterPartitionEvent(db_name, tbl_name, null, tmpPart, this));
            }
          } catch (NoSuchObjectException e) {
            throw new MetaException(e.getMessage());
          }
        }
        oldParts = alterHandler.alterPartitions(getMS(), wh, db_name, tbl_name, new_parts);

        Iterator<Partition> olditr = oldParts.iterator();
        for (Partition tmpPart : new_parts) {
          Partition oldTmpPart = null;
          if (olditr.hasNext()) {
            oldTmpPart = (Partition) olditr.next();
          }
          else {
            throw new InvalidOperationException("failed to alterpartitions");
          }
          for (MetaStoreEventListener listener : listeners) {
            AlterPartitionEvent alterPartitionEvent =
                new AlterPartitionEvent(oldTmpPart, tmpPart, true, this);
            listener.onAlterPartition(alterPartitionEvent);
          }
        }
      } catch (InvalidObjectException e) {
        ex = e;
        throw new InvalidOperationException(e.getMessage());
      } catch (AlreadyExistsException e) {
        ex = e;
        throw new InvalidOperationException(e.getMessage());
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof InvalidOperationException) {
          throw (InvalidOperationException) e;
        } else if (e instanceof TException) {
          throw (TException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("alter_partition", oldParts != null, ex);
      }
      return;
    }

    public boolean create_index(Index index_def)
        throws IndexAlreadyExistsException, MetaException {
      endFunction(startFunction("create_index"), false, null);
      // TODO Auto-generated method stub
      throw new MetaException("Not yet implemented");
    }

    public void alter_index(final String dbname, final String base_table_name,
        final String index_name, final Index newIndex)
        throws InvalidOperationException, MetaException {
      startFunction("alter_index", ": db=" + dbname + " base_tbl=" + base_table_name
          + " idx=" + index_name + " newidx=" + newIndex.getIndexName());
      newIndex.putToParameters(hive_metastoreConstants.DDL_TIME, Long.toString(System
          .currentTimeMillis() / 1000));

      boolean success = false;
      Exception ex = null;
      try {
        getMS().alterIndex(dbname, base_table_name, index_name, newIndex);
        success = true;
      } catch (InvalidObjectException e) {
        ex = e;
        throw new InvalidOperationException(e.getMessage());
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof InvalidOperationException) {
          throw (InvalidOperationException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("alter_index", success, ex);
      }
      return;
    }

    public String getVersion() throws TException {
      endFunction(startFunction("getVersion"), true, null);
      return "3.0";
    }

    @Override
    public void alter_table(final String dbname, final String name,
        final Table newTable)
        throws InvalidOperationException, MetaException {
      // Do not set an environment context.
      alter_table(dbname, name, newTable, null);
    }

    @Override
    public void alter_table_with_environment_context(final String dbname,
        final String name, final Table newTable,
        final EnvironmentContext envContext)
        throws InvalidOperationException, MetaException {
      alter_table(dbname, name, newTable, envContext);
    }

    private void alter_table(final String dbname, final String name,
        final Table newTable, final EnvironmentContext envContext)
        throws InvalidOperationException, MetaException {
      startFunction("alter_table", ": db=" + dbname + " tbl=" + name
          + " newtbl=" + newTable.getTableName());

      // Update the time if it hasn't been specified.
      if (newTable.getParameters() == null ||
          newTable.getParameters().get(hive_metastoreConstants.DDL_TIME) == null) {
        newTable.putToParameters(hive_metastoreConstants.DDL_TIME, Long.toString(System
            .currentTimeMillis() / 1000));
      }
      boolean success = false;
      Exception ex = null;
      try {
        Table oldt = get_table(dbname, name);
        firePreEvent(new PreAlterTableEvent(oldt, newTable, this));
        alterHandler.alterTable(getMS(), wh, dbname, name, newTable);
        success = true;

        for (MetaStoreEventListener listener : listeners) {
          AlterTableEvent alterTableEvent =
              new AlterTableEvent(oldt, newTable, success, this);
          alterTableEvent.setEnvironmentContext(envContext);
          listener.onAlterTable(alterTableEvent);
        }
      } catch (NoSuchObjectException e) {
        // thrown when the table to be altered does not exist
        ex = e;
        throw new InvalidOperationException(e.getMessage());
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof InvalidOperationException) {
          throw (InvalidOperationException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("alter_table", success, ex);
      }
    }

    public List<String> get_tables(final String dbname, final String pattern)
        throws MetaException {
      startFunction("get_tables", ": db=" + dbname + " pat=" + pattern);

      LOG.warn("-----zjw--haha ");

      List<String> ret = null;
      Exception ex = null;
      try {
        ret = getMS().getTables(dbname, pattern);
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("get_tables", ret != null, ex);
      }
      return ret;
    }

    public List<String> get_all_tables(final String dbname) throws MetaException {
      startFunction("get_all_tables", ": db=" + dbname);

      List<String> ret = null;
      Exception ex = null;
      try {
        ret = getMS().getAllTables(dbname);
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("get_all_tables", ret != null, ex);
      }
      return ret;
    }

    public List<FieldSchema> get_fields(String db, String tableName)
        throws MetaException, UnknownTableException, UnknownDBException {
      startFunction("get_fields", ": db=" + db + "tbl=" + tableName);
      String[] names = tableName.split("\\.");
      String base_table_name = names[0];

      Table tbl;
      List<FieldSchema> ret = null;
      Exception ex = null;
      try {
        try {
          tbl = get_table(db, base_table_name);
        } catch (NoSuchObjectException e) {
          throw new UnknownTableException(e.getMessage());
        }
        boolean getColsFromSerDe = SerDeUtils.shouldGetColsFromSerDe(
            tbl.getSd().getSerdeInfo().getSerializationLib());
        if (!getColsFromSerDe) {
          ret = tbl.getSd().getCols();
        } else {
          try {
            Deserializer s = MetaStoreUtils.getDeserializer(hiveConf, tbl);
            ret = MetaStoreUtils.getFieldsFromDeserializer(tableName, s);
          } catch (SerDeException e) {
            StringUtils.stringifyException(e);
            throw new MetaException(e.getMessage());
          }
        }
      } catch (Exception e) {
        ex = e;
        if (e instanceof UnknownDBException) {
          throw (UnknownDBException) e;
        } else if (e instanceof UnknownTableException) {
          throw (UnknownTableException) e;
        } else if (e instanceof MetaException) {
          throw (MetaException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("get_fields", ret != null, ex);
      }

      return ret;
    }

    /**
     * Return the schema of the table. This function includes partition columns
     * in addition to the regular columns.
     *
     * @param db
     *          Name of the database
     * @param tableName
     *          Name of the table
     * @return List of columns, each column is a FieldSchema structure
     * @throws MetaException
     * @throws UnknownTableException
     * @throws UnknownDBException
     */
    public List<FieldSchema> get_schema(String db, String tableName)
        throws MetaException, UnknownTableException, UnknownDBException {
      startFunction("get_schema", ": db=" + db + "tbl=" + tableName);
      boolean success = false;
      Exception ex = null;
      try {
        String[] names = tableName.split("\\.");
        String base_table_name = names[0];

        Table tbl;
        try {
          tbl = get_table(db, base_table_name);
        } catch (NoSuchObjectException e) {
          throw new UnknownTableException(e.getMessage());
        }
        List<FieldSchema> fieldSchemas = get_fields(db, base_table_name);

        if (tbl == null || fieldSchemas == null) {
          throw new UnknownTableException(tableName + " doesn't exist");
        }

        if (tbl.getPartitionKeys() != null) {
          // Combine the column field schemas and the partition keys to create the
          // whole schema
          fieldSchemas.addAll(tbl.getPartitionKeys());
        }
        success = true;
        return fieldSchemas;
      } catch (Exception e) {
        ex = e;
        if (e instanceof UnknownDBException) {
          throw (UnknownDBException) e;
        } else if (e instanceof UnknownTableException) {
          throw (UnknownTableException) e;
        } else if (e instanceof MetaException) {
          throw (MetaException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("get_schema", success, ex);
      }
    }

    public String getCpuProfile(int profileDurationInSec) throws TException {
      return "";
    }

    /**
     * Returns the value of the given configuration variable name. If the
     * configuration variable with the given name doesn't exist, or if there
     * were an exception thrown while retrieving the variable, or if name is
     * null, defaultValue is returned.
     */
    public String get_config_value(String name, String defaultValue)
        throws TException, ConfigValSecurityException {
      startFunction("get_config_value", ": name=" + name + " defaultValue="
          + defaultValue);
      boolean success = false;
      Exception ex = null;
      try {
        if (name == null) {
          success = true;
          return defaultValue;
        }
        // Allow only keys that start with hive.*, hdfs.*, mapred.* for security
        // i.e. don't allow access to db password
        if (!Pattern.matches("(hive|hdfs|mapred).*", name)) {
          throw new ConfigValSecurityException("For security reasons, the "
              + "config key " + name + " cannot be accessed");
        }

        String toReturn = defaultValue;
        try {
          toReturn = hiveConf.get(name, defaultValue);
        } catch (RuntimeException e) {
          LOG.error(threadLocalId.get().toString() + ": "
              + "RuntimeException thrown in get_config_value - msg: "
              + e.getMessage() + " cause: " + e.getCause());
        }
        success = true;
        return toReturn;
      } catch (Exception e) {
        ex = e;
        if (e instanceof ConfigValSecurityException) {
          throw (ConfigValSecurityException) e;
        } else if (e instanceof TException) {
          throw (TException) e;
        } else {
          TException te = new TException(e.toString());
          te.initCause(e);
          throw te;
        }
      } finally {
        endFunction("get_config_value", success, ex);
      }
    }

    private List<String> getPartValsFromName(RawStore ms, String dbName, String tblName,
        String partName) throws MetaException, InvalidObjectException {
      // Unescape the partition name
      LinkedHashMap<String, String> hm = Warehouse.makeSpecFromName(partName);

      // getPartition expects partition values in a list. use info from the
      // table to put the partition column values in order
      Table t = ms.getTable(dbName, tblName);
      if (t == null) {
        throw new InvalidObjectException(dbName + "." + tblName
            + " table not found");
      }

      List<String> partVals = new ArrayList<String>();
      for (FieldSchema field : t.getPartitionKeys()) {
        String key = field.getName();
        String val = hm.get(key);
        if (val == null) {
          throw new InvalidObjectException("incomplete partition name - missing " + key);
        }
        partVals.add(val);
      }
      return partVals;
    }

    private Partition get_partition_by_name_core(final RawStore ms, final String db_name,
        final String tbl_name, final String part_name)
        throws MetaException, NoSuchObjectException, TException {
      Partition p = ms.getPartition(db_name, tbl_name, part_name);

      if (p == null) {
        throw new NoSuchObjectException(db_name + "." + tbl_name
            + " partition (" + part_name + ") not found");
      }
      return p;
    }

    public Partition get_partition_by_name(final String db_name, final String tbl_name,
        final String part_name) throws MetaException, NoSuchObjectException, TException {

      startFunction("get_partition_by_name", ": db=" + db_name + " tbl="
          + tbl_name + " part=" + part_name);

      Partition ret = null;
      Exception ex = null;
      try {
        ret = get_partition_by_name_core(getMS(), db_name, tbl_name, part_name);
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof NoSuchObjectException) {
          throw (NoSuchObjectException) e;
        } else if (e instanceof TException) {
          throw (TException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("get_partition_by_name", ret != null, ex);
      }
      return ret;
    }

    public Partition append_partition_by_name(final String db_name, final String tbl_name,
        final String part_name) throws InvalidObjectException,
        AlreadyExistsException, MetaException, TException {
      startFunction("append_partition_by_name", ": db=" + db_name + " tbl="
          + tbl_name + " part=" + part_name);

      Partition ret = null;
      Exception ex = null;
      try {
        RawStore ms = getMS();
        List<String> partVals = getPartValsFromName(ms, db_name, tbl_name, part_name);
        ret = append_partition_common(ms, db_name, tbl_name, partVals);
      } catch (Exception e) {
        ex = e;
        if (e instanceof InvalidObjectException) {
          throw (InvalidObjectException) e;
        } else if (e instanceof AlreadyExistsException) {
          throw (AlreadyExistsException) e;
        } else if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof TException) {
          throw (TException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("append_partition_by_name", ret != null, ex);
      }
      return ret;
    }

    private boolean drop_partition_by_name_core(final RawStore ms,
        final String db_name, final String tbl_name, final String part_name,
        final boolean deleteData) throws NoSuchObjectException,
        MetaException, TException, IOException, InvalidObjectException, InvalidInputException {

//      List<String> partVals = null;
//      try {
//        partVals = getPartValsFromName(ms, db_name, tbl_name, part_name);
//      } catch (InvalidObjectException e) {
//        throw new NoSuchObjectException(e.getMessage());
//      }
//
//      return drop_partition_common(ms, db_name, tbl_name, partVals, deleteData);
      return drop_partition_common_by_name(ms, db_name, tbl_name, part_name, deleteData);
    }

    @Override
    public boolean drop_partition_by_name(final String db_name, final String tbl_name,
        final String part_name, final boolean deleteData) throws NoSuchObjectException,
        MetaException, TException {
      startFunction("drop_partition_by_name", ": db=" + db_name + " tbl="
          + tbl_name + " part=" + part_name);

      boolean ret = false;
      Exception ex = null;
      try {
        ret = drop_partition_by_name_core(getMS(), db_name, tbl_name,
            part_name, deleteData);
      } catch (IOException e) {
        ex = e;
        throw new MetaException(e.getMessage());
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof NoSuchObjectException) {
          throw (NoSuchObjectException) e;
        } else if (e instanceof TException) {
          throw (TException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("drop_partition_by_name", ret, ex);
      }

      return ret;
    }

    @Override
    public List<Partition> get_partitions_ps(final String db_name,
        final String tbl_name, final List<String> part_vals,
        final short max_parts) throws MetaException, TException, NoSuchObjectException {
      startPartitionFunction("get_partitions_ps", db_name, tbl_name, part_vals);

      List<Partition> ret = null;
      Exception ex = null;
      try {
        ret = get_partitions_ps_with_auth(db_name, tbl_name, part_vals,
            max_parts, null, null);
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof NoSuchObjectException) {
          throw (NoSuchObjectException) e;
        } else if (e instanceof TException) {
          throw (TException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("get_partitions_ps", ret != null, ex);
      }

      return ret;
    }

    @Override
    public List<Partition> get_partitions_ps_with_auth(final String db_name,
        final String tbl_name, final List<String> part_vals,
        final short max_parts, final String userName,
        final List<String> groupNames) throws MetaException, TException, NoSuchObjectException {
      startPartitionFunction("get_partitions_ps_with_auth", db_name, tbl_name,
          part_vals);
      List<Partition> ret = null;
      Exception ex = null;
      try {
        ret = getMS().listPartitionsPsWithAuth(db_name, tbl_name, part_vals, max_parts,
            userName, groupNames);
      } catch (InvalidObjectException e) {
        ex = e;
        throw new MetaException(e.getMessage());
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof NoSuchObjectException) {
          throw (NoSuchObjectException) e;
        } else if (e instanceof TException) {
          throw (TException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("get_partitions_ps_with_auth", ret != null, ex);
      }
      return ret;
    }

    @Override
    public List<String> get_partition_names_ps(final String db_name,
        final String tbl_name, final List<String> part_vals, final short max_parts)
        throws MetaException, TException, NoSuchObjectException {
      startPartitionFunction("get_partitions_names_ps", db_name, tbl_name, part_vals);
      List<String> ret = null;
      Exception ex = null;
      try {
        ret = getMS().listPartitionNamesPs(db_name, tbl_name, part_vals, max_parts);
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof NoSuchObjectException) {
          throw (NoSuchObjectException) e;
        } else if (e instanceof TException) {
          throw (TException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("get_partitions_names_ps", ret != null, ex);
      }
      return ret;
    }

    @Override
    public List<String> partition_name_to_vals(String part_name)
        throws MetaException, TException {
      if (part_name.length() == 0) {
        return new ArrayList<String>();
      }
      LinkedHashMap<String, String> map = Warehouse.makeSpecFromName(part_name);
      List<String> part_vals = new ArrayList<String>();
      part_vals.addAll(map.values());
      return part_vals;
    }

    @Override
    public Map<String, String> partition_name_to_spec(String part_name) throws MetaException,
        TException {
      if (part_name.length() == 0) {
        return new HashMap<String, String>();
      }
      return Warehouse.makeSpecFromName(part_name);
    }

    @Override
    public Index add_index(final Index newIndex, final Table indexTable)
        throws InvalidObjectException, AlreadyExistsException, MetaException, TException {
      startFunction("add_index", ": " + newIndex.toString() + " " );
      Index ret = null;
      Exception ex = null;
      try {
        ret = add_index_core(getMS(), newIndex, indexTable);
      } catch (Exception e) {
        ex = e;
        if (e instanceof InvalidObjectException) {
          throw (InvalidObjectException) e;
        } else if (e instanceof AlreadyExistsException) {
          throw (AlreadyExistsException) e;
        } else if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof TException) {
          throw (TException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("add_index", ret != null, ex);
      }
      return ret;
    }

    private Index add_index_core(final RawStore ms, final Index index, final Table indexTable)
        throws InvalidObjectException, AlreadyExistsException, MetaException {

      boolean success = false, indexTableCreated = false;

      try {
        ms.openTransaction();
        Index old_index = null;
        try {
          old_index = get_index_by_name(index.getDbName(), index
              .getOrigTableName(), index.getIndexName());
        } catch (Exception e) {
        }
        if (old_index != null) {
          throw new AlreadyExistsException("Index already exists:" + index);
        }
        Table origTbl = ms.getTable(index.getDbName(), index.getOrigTableName());
        if (origTbl == null) {
          throw new InvalidObjectException(
              "Unable to add index because database or the orginal table do not exist");
        }

        // set create time
        long time = System.currentTimeMillis() / 1000;

        //removed by zjw

//        Table indexTbl = indexTable;
//        if (indexTbl != null) {
//          try {
//            indexTbl = ms.getTable(index.getDbName(), index.getIndexTableName());
//          } catch (Exception e) {
//          }
//          if (indexTbl != null) {
//            throw new InvalidObjectException(
//                "Unable to add index because index table already exists");
//          }
//          this.create_table(indexTable);
//          indexTableCreated = true;
//        }

        LOG.warn("---zjw-- creating index"+index.getIndexName());

        index.setCreateTime((int) time);
        index.putToParameters(hive_metastoreConstants.DDL_TIME, Long.toString(time));

        ms.addIndex(index);
        success = ms.commitTransaction();
        return index;
      } finally {
        if (!success) {
          if (indexTableCreated) {
            try {
              this.drop_table(index.getDbName(), index.getIndexTableName(), false);
            } catch (Exception e) {
            }
          }
          ms.rollbackTransaction();
        }
      }
    }

    @Override
    public boolean drop_index_by_name(final String dbName, final String tblName,
        final String indexName, final boolean deleteData) throws NoSuchObjectException,
        MetaException, TException {
      startFunction("drop_index_by_name", ": db=" + dbName + " tbl="
          + tblName + " index=" + indexName);

      boolean ret = false;
      Exception ex = null;
      try {
        ret = drop_index_by_name_core(getMS(), dbName, tblName,
            indexName, deleteData);
      } catch (IOException e) {
        ex = e;
        throw new MetaException(e.getMessage());
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof NoSuchObjectException) {
          throw (NoSuchObjectException) e;
        } else if (e instanceof TException) {
          throw (TException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("drop_index_by_name", ret, ex);
      }

      return ret;
    }

    private boolean drop_index_by_name_core(final RawStore ms,
        final String dbName, final String tblName,
        final String indexName, final boolean deleteData) throws NoSuchObjectException,
        MetaException, TException, IOException, InvalidObjectException, InvalidInputException {

      boolean success = false;
      Path tblPath = null;
      List<Path> partPaths = null;
      try {
        ms.openTransaction();

        // drop the underlying index table
        Index index = get_index_by_name(dbName, tblName, indexName);
        if (index == null) {
          throw new NoSuchObjectException(indexName + " doesn't exist");
        }
        ms.dropIndex(dbName, tblName, indexName);

        String idxTblName = index.getIndexTableName();
        if (idxTblName != null) {
          Table tbl = null;
          tbl = this.get_table(dbName, idxTblName);
          if (tbl.getSd() == null) {
            throw new MetaException("Table metadata is corrupted");
          }

          if (tbl.getSd().getLocation() != null) {
            tblPath = new Path(tbl.getSd().getLocation());
            if (!wh.isWritable(tblPath.getParent())) {
              throw new MetaException("Index table metadata not deleted since " +
                  tblPath.getParent() + " is not writable by " +
                  hiveConf.getUser());
            }
          }

          // Drop the partitions and get a list of partition locations which need to be deleted
          partPaths = dropPartitionsAndGetLocations(ms, dbName, idxTblName, tblPath,
              tbl.getPartitionKeys(), deleteData);

          if (!ms.dropTable(dbName, idxTblName)) {
            throw new MetaException("Unable to drop underlying data table "
                + idxTblName + " for index " + idxTblName);
          }
        }
        success = ms.commitTransaction();
      } finally {
        if (!success) {
          ms.rollbackTransaction();
          return false;
        } else if (deleteData && tblPath != null) {
          deletePartitionData(partPaths);
          deleteTableData(tblPath);
          // ok even if the data is not deleted
        }
      }
      return true;
    }

    @Override
    public Index get_index_by_name(final String dbName, final String tblName,
        final String indexName) throws MetaException, NoSuchObjectException,
        TException {

      startFunction("get_index_by_name", ": db=" + dbName + " tbl="
          + tblName + " index=" + indexName);

      Index ret = null;
      Exception ex = null;
      try {
        ret = get_index_by_name_core(getMS(), dbName, tblName, indexName);
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof NoSuchObjectException) {
          throw (NoSuchObjectException) e;
        } else if (e instanceof TException) {
          throw (TException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("drop_index_by_name", ret != null, ex);
      }
      return ret;
    }

    private Index get_index_by_name_core(final RawStore ms, final String db_name,
        final String tbl_name, final String index_name)
        throws MetaException, NoSuchObjectException, TException {
      Index index = ms.getIndex(db_name, tbl_name, index_name);

      if (index == null) {
        throw new NoSuchObjectException(db_name + "." + tbl_name
            + " index=" + index_name + " not found");
      }
      return index;
    }

    @Override
    public List<String> get_index_names(final String dbName, final String tblName,
        final short maxIndexes) throws MetaException, TException {
      startTableFunction("get_index_names", dbName, tblName);

      List<String> ret = null;
      Exception ex = null;
      try {
        ret = getMS().listIndexNames(dbName, tblName, maxIndexes);
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof TException) {
          throw (TException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("get_index_names", ret != null, ex);
      }
      return ret;
    }

    @Override
    public List<Index> get_indexes(final String dbName, final String tblName,
        final short maxIndexes) throws NoSuchObjectException, MetaException,
        TException {
      startTableFunction("get_indexes", dbName, tblName);

      List<Index> ret = null;
      Exception ex = null;
      try {
        ret = getMS().getIndexes(dbName, tblName, maxIndexes);
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof NoSuchObjectException) {
          throw (NoSuchObjectException) e;
        } else if (e instanceof TException) {
          throw (TException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("get_indexes", ret != null, ex);
      }
      return ret;
    }

    private String lowerCaseConvertPartName(String partName) throws MetaException {
      boolean isFirst = true;
      Map<String, String> partSpec = Warehouse.makeEscSpecFromName(partName);
      String convertedPartName = new String();

      for (Map.Entry<String, String> entry : partSpec.entrySet()) {
        String partColName = entry.getKey();
        String partColVal = entry.getValue();

        if (!isFirst) {
          convertedPartName += "/";
        } else {
          isFirst = false;
        }
        convertedPartName += partColName.toLowerCase() + "=" + partColVal;
      }
      return convertedPartName;
    }

    public ColumnStatistics get_table_column_statistics(String dbName, String tableName,
      String colName) throws NoSuchObjectException, MetaException, TException,
      InvalidInputException, InvalidObjectException
    {
      dbName = dbName.toLowerCase();
      tableName = tableName.toLowerCase();
      colName = colName.toLowerCase();
      startFunction("get_column_statistics_by_table: db=" + dbName + " table=" + tableName +
                    " column=" + colName);
      ColumnStatistics statsObj = null;
      try {
        statsObj = getMS().getTableColumnStatistics(dbName, tableName, colName);
      } finally {
        endFunction("get_column_statistics_by_table: ", statsObj != null, null);
      }
      return statsObj;
    }

    public ColumnStatistics get_partition_column_statistics(String dbName, String tableName,
      String partName, String colName) throws NoSuchObjectException, MetaException,
      InvalidInputException, TException,InvalidObjectException
    {
      dbName = dbName.toLowerCase();
      tableName = tableName.toLowerCase();
      colName = colName.toLowerCase();
      String convertedPartName = lowerCaseConvertPartName(partName);
      startFunction("get_column_statistics_by_partition: db=" + dbName + " table=" + tableName +
          " partition=" + convertedPartName + " column=" + colName);
      ColumnStatistics statsObj = null;

      try {
        List<String> partVals = getPartValsFromName(getMS(), dbName, tableName, partName);
        statsObj = getMS().getPartitionColumnStatistics(dbName, tableName, convertedPartName,
                                                            partVals, colName);
      } finally {
        endFunction("get_column_statistics_by_partition: ", statsObj != null, null);
      }
      return statsObj;
   }

    public boolean update_table_column_statistics(ColumnStatistics colStats)
      throws NoSuchObjectException,InvalidObjectException,MetaException,TException,
      InvalidInputException
    {
      String dbName = null;
      String tableName = null;
      String colName = null;
      ColumnStatisticsDesc statsDesc = colStats.getStatsDesc();
      dbName = statsDesc.getDbName().toLowerCase();
      tableName = statsDesc.getTableName().toLowerCase();

      statsDesc.setDbName(dbName);
      statsDesc.setTableName(tableName);
      long time = System.currentTimeMillis() / 1000;
      statsDesc.setLastAnalyzed(time);

      List<ColumnStatisticsObj> statsObjs =  colStats.getStatsObj();

      for (ColumnStatisticsObj statsObj:statsObjs) {
        colName = statsObj.getColName().toLowerCase();
        statsObj.setColName(colName);
        startFunction("write_column_statistics:  db=" + dbName + " table=" + tableName +
          " column=" + colName);
      }

     colStats.setStatsDesc(statsDesc);
     colStats.setStatsObj(statsObjs);

     boolean ret = false;

      try {
        ret = getMS().updateTableColumnStatistics(colStats);
        return ret;
      } finally {
        endFunction("write_column_statistics: ", ret != false, null);
      }
    }

    public boolean update_partition_column_statistics(ColumnStatistics colStats)
      throws NoSuchObjectException,InvalidObjectException,MetaException,TException,
      InvalidInputException
    {

      String dbName = null;
      String tableName = null;
      String partName = null;
      String colName = null;

      ColumnStatisticsDesc statsDesc = colStats.getStatsDesc();
      dbName = statsDesc.getDbName().toLowerCase();
      tableName = statsDesc.getTableName().toLowerCase();
      partName = lowerCaseConvertPartName(statsDesc.getPartName());

      statsDesc.setDbName(dbName);
      statsDesc.setTableName(tableName);
      statsDesc.setPartName(partName);

      long time = System.currentTimeMillis() / 1000;
      statsDesc.setLastAnalyzed(time);

      List<ColumnStatisticsObj> statsObjs =  colStats.getStatsObj();

      for (ColumnStatisticsObj statsObj:statsObjs) {
        colName = statsObj.getColName().toLowerCase();
        statsObj.setColName(colName);
        startFunction("write_partition_column_statistics:  db=" + dbName + " table=" + tableName +
          " part=" + partName + "column=" + colName);
      }

      colStats.setStatsDesc(statsDesc);
      colStats.setStatsObj(statsObjs);

      boolean ret = false;

      try {
        List<String> partVals = getPartValsFromName(getMS(), dbName,
            tableName, partName);
        ret = getMS().updatePartitionColumnStatistics(colStats, partVals);
        return ret;
      } finally {
        endFunction("write_partition_column_statistics: ", ret != false, null);
      }
    }

    public boolean delete_partition_column_statistics(String dbName, String tableName,
      String partName, String colName) throws NoSuchObjectException, MetaException,
      InvalidObjectException, TException, InvalidInputException
    {
      dbName = dbName.toLowerCase();
      tableName = tableName.toLowerCase();
      if (colName != null) {
        colName = colName.toLowerCase();
      }
      String convertedPartName = lowerCaseConvertPartName(partName);
      startFunction("delete_column_statistics_by_partition: db=" + dbName + " table=" + tableName +
                    " partition=" + convertedPartName + " column=" + colName);
      boolean ret = false;

      try {
        List<String> partVals = getPartValsFromName(getMS(), dbName, tableName, convertedPartName);
        ret = getMS().deletePartitionColumnStatistics(dbName, tableName,
                                                      convertedPartName, partVals, colName);
      } finally {
        endFunction("delete_column_statistics_by_partition: ", ret != false, null);
      }
      return ret;
    }

    public boolean delete_table_column_statistics(String dbName, String tableName, String colName)
      throws NoSuchObjectException, MetaException, InvalidObjectException, TException,
      InvalidInputException
   {
      dbName = dbName.toLowerCase();
      tableName = tableName.toLowerCase();

      if (colName != null) {
        colName = colName.toLowerCase();
      }
      startFunction("delete_column_statistics_by_table: db=" + dbName + " table=" + tableName +
                    " column=" + colName);

      boolean ret = false;
      try {
        ret = getMS().deleteTableColumnStatistics(dbName, tableName, colName);
      } finally {
        endFunction("delete_column_statistics_by_table: ", ret != false, null);
      }
      return ret;
   }

    @Override
    public List<Partition> get_partitions_by_filter(final String dbName,
        final String tblName, final String filter, final short maxParts)
        throws MetaException, NoSuchObjectException, TException {
      startTableFunction("get_partitions_by_filter", dbName, tblName);

      List<Partition> ret = null;
      Exception ex = null;
      try {
        ret = getMS().getPartitionsByFilter(dbName, tblName, filter, maxParts);
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof NoSuchObjectException) {
          throw (NoSuchObjectException) e;
        } else if (e instanceof TException) {
          throw (TException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("get_partitions_by_filter", ret != null, ex);
      }
      return ret;
    }

    @Override
    public List<Partition> get_partitions_by_names(final String dbName,
        final String tblName, final List<String> partNames)
        throws MetaException, NoSuchObjectException, TException {

      startTableFunction("get_partitions_by_names", dbName, tblName);

      List<Partition> ret = null;
      Exception ex = null;
      try {
        ret = getMS().getPartitionsByNames(dbName, tblName, partNames);
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof NoSuchObjectException) {
          throw (NoSuchObjectException) e;
        } else if (e instanceof TException) {
          throw (TException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("get_partitions_by_names", ret != null, ex);
      }
      return ret;
    }

    @Override
    public PrincipalPrivilegeSet get_privilege_set(HiveObjectRef hiveObject,
        String userName, List<String> groupNames) throws MetaException,
        TException {
      if (hiveObject.getObjectType() == HiveObjectType.COLUMN) {
        String partName = getPartName(hiveObject);
        return this.get_column_privilege_set(hiveObject.getDbName(), hiveObject
            .getObjectName(), partName, hiveObject.getColumnName(), userName,
            groupNames);
      } else if (hiveObject.getObjectType() == HiveObjectType.PARTITION) {
        String partName = getPartName(hiveObject);
        return this.get_partition_privilege_set(hiveObject.getDbName(),
            hiveObject.getObjectName(), partName, userName, groupNames);
      } else if (hiveObject.getObjectType() == HiveObjectType.DATABASE) {
        return this.get_db_privilege_set(hiveObject.getDbName(), userName,
            groupNames);
      } else if (hiveObject.getObjectType() == HiveObjectType.TABLE) {
        return this.get_table_privilege_set(hiveObject.getDbName(), hiveObject
            .getObjectName(), userName, groupNames);
      } else if (hiveObject.getObjectType() == HiveObjectType.GLOBAL) {
        return this.get_user_privilege_set(userName, groupNames);
      }
      return null;
    }

    private String getPartName(HiveObjectRef hiveObject) throws MetaException {
      String partName = null;
      List<String> partValue = hiveObject.getPartValues();
      if (partValue != null && partValue.size() > 0) {
        try {
          Table table = get_table(hiveObject.getDbName(), hiveObject
              .getObjectName());
          partName = Warehouse
              .makePartName(table.getPartitionKeys(), partValue);
        } catch (NoSuchObjectException e) {
          throw new MetaException(e.getMessage());
        }
      }
      return partName;
    }

    public PrincipalPrivilegeSet get_column_privilege_set(final String dbName,
        final String tableName, final String partName, final String columnName,
        final String userName, final List<String> groupNames) throws MetaException,
        TException {
      incrementCounter("get_column_privilege_set");

      PrincipalPrivilegeSet ret = null;
      try {
        ret = getMS().getColumnPrivilegeSet(
            dbName, tableName, partName, columnName, userName, groupNames);
      } catch (MetaException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      return ret;
    }

    public PrincipalPrivilegeSet get_db_privilege_set(final String dbName,
        final String userName, final List<String> groupNames) throws MetaException,
        TException {
      incrementCounter("get_db_privilege_set");

      PrincipalPrivilegeSet ret = null;
      try {
        ret = getMS().getDBPrivilegeSet(dbName, userName, groupNames);
      } catch (MetaException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      return ret;
    }

    public PrincipalPrivilegeSet get_partition_privilege_set(
        final String dbName, final String tableName, final String partName,
        final String userName, final List<String> groupNames)
        throws MetaException, TException {
      incrementCounter("get_partition_privilege_set");

      PrincipalPrivilegeSet ret = null;
      try {
        ret = getMS().getPartitionPrivilegeSet(dbName, tableName, partName,
            userName, groupNames);
      } catch (MetaException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      return ret;
    }

    public PrincipalPrivilegeSet get_table_privilege_set(final String dbName,
        final String tableName, final String userName,
        final List<String> groupNames) throws MetaException, TException {
      incrementCounter("get_table_privilege_set");

      PrincipalPrivilegeSet ret = null;
      try {
        ret = getMS().getTablePrivilegeSet(dbName, tableName, userName,
            groupNames);
      } catch (MetaException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      return ret;
    }

    @Override
    public boolean grant_role(final String roleName,
        final String userName, final PrincipalType principalType,
        final String grantor, final PrincipalType grantorType, final boolean grantOption)
        throws MetaException, TException {
      incrementCounter("add_role_member");

      Boolean ret = null;
      try {
        RawStore ms = getMS();
        Role role = ms.getRole(roleName);
        ret = ms.grantRole(role, userName, principalType, grantor, grantorType, grantOption);
      } catch (MetaException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      return ret;
    }

    public List<Role> list_roles(final String principalName,
        final PrincipalType principalType) throws MetaException, TException {
      incrementCounter("list_roles");

      List<Role> ret = null;
      try {

        List<Role> result = new ArrayList<Role>();
        List<MRoleMap> roleMap = getMS().listRoles(principalName, principalType);
        if (roleMap != null) {
          for (MRoleMap role : roleMap) {
            MRole r = role.getRole();
            result.add(new Role(r.getRoleName(), r
                .getCreateTime(), r.getOwnerName()));
          }
        }
        ret = result;
      } catch (MetaException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      return ret;
    }

    @Override
    public boolean create_role(final Role role)
        throws MetaException, TException {
      incrementCounter("create_role");

      Boolean ret = null;
      try {
        ret = getMS().addRole(role.getRoleName(), role.getOwnerName());
      } catch (MetaException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      return ret;
    }

    @Override
    public boolean drop_role(final String roleName)
        throws MetaException, TException {
      incrementCounter("drop_role");

      Boolean ret = null;
      try {
        ret = getMS().removeRole(roleName);
      } catch (MetaException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      return ret;
    }

    @Override
    public List<String> get_role_names() throws MetaException, TException {
      incrementCounter("get_role_names");

      List<String> ret = null;
      try {
        ret = getMS().listRoleNames();
      } catch (MetaException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      return ret;
    }

    @Override
    public boolean grant_privileges(final PrivilegeBag privileges) throws MetaException,
        TException {
      incrementCounter("grant_privileges");

      Boolean ret = null;
      try {
        ret = getMS().grantPrivileges(privileges);
      } catch (MetaException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      return ret;
    }

    @Override
    public boolean revoke_role(final String roleName, final String userName,
        final PrincipalType principalType) throws MetaException, TException {
      incrementCounter("remove_role_member");

      Boolean ret = null;
      try {
        RawStore ms = getMS();
        Role mRole = ms.getRole(roleName);
        ret = ms.revokeRole(mRole, userName, principalType);
      } catch (MetaException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      return ret;
    }

    @Override
    public boolean revoke_privileges(final PrivilegeBag privileges)
        throws MetaException, TException {
      incrementCounter("revoke_privileges");

      Boolean ret = null;
      try {
        ret = getMS().revokePrivileges(privileges);
      } catch (MetaException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      return ret;
    }

    public PrincipalPrivilegeSet get_user_privilege_set(final String userName,
        final List<String> groupNames) throws MetaException, TException {
      incrementCounter("get_user_privilege_set");

      PrincipalPrivilegeSet ret = null;
      try {
        ret = getMS().getUserPrivilegeSet(userName, groupNames);
      } catch (MetaException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      return ret;
    }

    public PrincipalType getPrincipalType(String principalType) {
      return PrincipalType.valueOf(principalType);
    }

    @Override
    public List<HiveObjectPrivilege> list_privileges(String principalName,
        PrincipalType principalType, HiveObjectRef hiveObject)
        throws MetaException, TException {
      if (hiveObject.getObjectType() == HiveObjectType.GLOBAL) {
        return this.list_global_privileges(principalName, principalType);
      } else if (hiveObject.getObjectType() == HiveObjectType.DATABASE) {
        return this.list_db_privileges(principalName, principalType, hiveObject
            .getDbName());
      } else if (hiveObject.getObjectType() == HiveObjectType.TABLE) {
        return this.list_table_privileges(principalName, principalType,
            hiveObject.getDbName(), hiveObject.getObjectName());
      } else if (hiveObject.getObjectType() == HiveObjectType.PARTITION) {
        return this.list_partition_privileges(principalName, principalType,
            hiveObject.getDbName(), hiveObject.getObjectName(), hiveObject
                .getPartValues());
      } else if (hiveObject.getObjectType() == HiveObjectType.COLUMN) {
        return this.list_column_privileges(principalName, principalType,
            hiveObject.getDbName(), hiveObject.getObjectName(), hiveObject
                .getPartValues(), hiveObject.getColumnName());
      }
      return null;
    }

    public List<HiveObjectPrivilege> list_column_privileges(
        final String principalName, final PrincipalType principalType,
        final String dbName, final String tableName, final List<String> partValues,
        final String columnName) throws MetaException, TException {
      incrementCounter("list_security_column_grant");

      List<HiveObjectPrivilege> ret = null;
      try {
        RawStore ms = getMS();
        String partName = null;
        if (partValues != null && partValues.size() > 0) {
          Table tbl = get_table(dbName, tableName);
          partName = Warehouse.makePartName(tbl.getPartitionKeys(), partValues);
        }

        List<HiveObjectPrivilege> result = Collections.<HiveObjectPrivilege> emptyList();

        if (partName != null) {
          Partition part = null;
          part = get_partition_by_name(dbName, tableName, partName);
          List<MPartitionColumnPrivilege> mPartitionCols = ms.listPrincipalPartitionColumnGrants(
              principalName,
              principalType, dbName, tableName, partName, columnName);
          if (mPartitionCols.size() > 0) {
            result = new ArrayList<HiveObjectPrivilege>();
            for (int i = 0; i < mPartitionCols.size(); i++) {
              MPartitionColumnPrivilege sCol = mPartitionCols.get(i);
              HiveObjectRef objectRef = new HiveObjectRef(
                  HiveObjectType.COLUMN, dbName, tableName,
                  part == null ? null : part.getValues(), sCol
                      .getColumnName());
              HiveObjectPrivilege secObj = new HiveObjectPrivilege(objectRef,
                  sCol.getPrincipalName(), principalType,
                  new PrivilegeGrantInfo(sCol.getPrivilege(), sCol
                      .getCreateTime(), sCol.getGrantor(), PrincipalType
                      .valueOf(sCol.getGrantorType()), sCol.getGrantOption()));
              result.add(secObj);
            }
          }
        } else {
          List<MTableColumnPrivilege> mTableCols = ms
              .listPrincipalTableColumnGrants(principalName, principalType,
                  dbName, tableName, columnName);
          if (mTableCols.size() > 0) {
            result = new ArrayList<HiveObjectPrivilege>();
            for (int i = 0; i < mTableCols.size(); i++) {
              MTableColumnPrivilege sCol = mTableCols.get(i);
              HiveObjectRef objectRef = new HiveObjectRef(
                  HiveObjectType.COLUMN, dbName, tableName, null, sCol
                      .getColumnName());
              HiveObjectPrivilege secObj = new HiveObjectPrivilege(
                  objectRef, sCol.getPrincipalName(), principalType,
                  new PrivilegeGrantInfo(sCol.getPrivilege(), sCol
                      .getCreateTime(), sCol.getGrantor(), PrincipalType
                      .valueOf(sCol.getGrantorType()), sCol
                      .getGrantOption()));
              result.add(secObj);
            }
          }
        }

        ret = result;
      } catch (MetaException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      return ret;
    }

    public List<HiveObjectPrivilege> list_db_privileges(final String principalName,
        final PrincipalType principalType, final String dbName)
        throws MetaException, TException {
      incrementCounter("list_security_db_grant");

      try {
        RawStore ms = getMS();
        List<MDBPrivilege> mDbs = ms.listPrincipalDBGrants(
            principalName, principalType, dbName);
        if (mDbs.size() > 0) {
          List<HiveObjectPrivilege> result = new ArrayList<HiveObjectPrivilege>();
          for (int i = 0; i < mDbs.size(); i++) {
            MDBPrivilege sDB = mDbs.get(i);
            HiveObjectRef objectRef = new HiveObjectRef(
                HiveObjectType.DATABASE, dbName, null, null, null);
            HiveObjectPrivilege secObj = new HiveObjectPrivilege(objectRef,
                sDB.getPrincipalName(), principalType,
                new PrivilegeGrantInfo(sDB.getPrivilege(), sDB
                    .getCreateTime(), sDB.getGrantor(), PrincipalType
                    .valueOf(sDB.getGrantorType()), sDB.getGrantOption()));
            result.add(secObj);
          }
          return result;
        }
        return Collections.<HiveObjectPrivilege> emptyList();
      } catch (MetaException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    public List<HiveObjectPrivilege> list_partition_privileges(
        final String principalName, final PrincipalType principalType,
        final String dbName, final String tableName, final List<String> partValues)
        throws MetaException, TException {
      incrementCounter("list_security_partition_grant");

      try {
        RawStore ms = getMS();
        Table tbl = get_table(dbName, tableName);
        String partName = Warehouse.makePartName(tbl.getPartitionKeys(), partValues);
        List<MPartitionPrivilege> mParts = ms.listPrincipalPartitionGrants(
            principalName, principalType, dbName, tableName, partName);
        if (mParts.size() > 0) {
          List<HiveObjectPrivilege> result = new ArrayList<HiveObjectPrivilege>();
          for (int i = 0; i < mParts.size(); i++) {
            MPartitionPrivilege sPart = mParts.get(i);
            HiveObjectRef objectRef = new HiveObjectRef(
                HiveObjectType.PARTITION, dbName, tableName, partValues,
                null);
            HiveObjectPrivilege secObj = new HiveObjectPrivilege(objectRef,
                sPart.getPrincipalName(), principalType,
                new PrivilegeGrantInfo(sPart.getPrivilege(), sPart
                    .getCreateTime(), sPart.getGrantor(), PrincipalType
                    .valueOf(sPart.getGrantorType()), sPart
                    .getGrantOption()));

            result.add(secObj);
          }
          return result;
        }
        return Collections.<HiveObjectPrivilege> emptyList();
      } catch (MetaException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    public List<HiveObjectPrivilege> list_table_privileges(
        final String principalName, final PrincipalType principalType,
        final String dbName, final String tableName) throws MetaException,
        TException {
      incrementCounter("list_security_table_grant");

      try {
        List<MTablePrivilege> mTbls = getMS()
            .listAllTableGrants(principalName, principalType, dbName, tableName);
        if (mTbls.size() > 0) {
          List<HiveObjectPrivilege> result = new ArrayList<HiveObjectPrivilege>();
          for (int i = 0; i < mTbls.size(); i++) {
            MTablePrivilege sTbl = mTbls.get(i);
            HiveObjectRef objectRef = new HiveObjectRef(
                HiveObjectType.TABLE, dbName, tableName, null, null);
            HiveObjectPrivilege secObj = new HiveObjectPrivilege(objectRef,
                sTbl.getPrincipalName(), principalType,
                new PrivilegeGrantInfo(sTbl.getPrivilege(), sTbl.getCreateTime(), sTbl
                    .getGrantor(), PrincipalType.valueOf(sTbl
                    .getGrantorType()), sTbl.getGrantOption()));
            result.add(secObj);
          }
          return result;
        }
        return Collections.<HiveObjectPrivilege> emptyList();
      } catch (MetaException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    public List<HiveObjectPrivilege> list_global_privileges(
        final String principalName, final PrincipalType principalType)
        throws MetaException, TException {
      incrementCounter("list_security_user_grant");

      try {
        List<MGlobalPrivilege> mUsers = getMS().listPrincipalGlobalGrants(
            principalName, principalType);
        if (mUsers.size() > 0) {
          List<HiveObjectPrivilege> result = new ArrayList<HiveObjectPrivilege>();
          for (int i = 0; i < mUsers.size(); i++) {
            MGlobalPrivilege sUsr = mUsers.get(i);
            HiveObjectRef objectRef = new HiveObjectRef(
                HiveObjectType.GLOBAL, null, null, null, null);
            HiveObjectPrivilege secUser = new HiveObjectPrivilege(
                objectRef, sUsr.getPrincipalName(), principalType,
                new PrivilegeGrantInfo(sUsr.getPrivilege(), sUsr
                    .getCreateTime(), sUsr.getGrantor(), PrincipalType
                    .valueOf(sUsr.getGrantorType()), sUsr.getGrantOption()));
            result.add(secUser);
          }
          return result;
        }
        return Collections.<HiveObjectPrivilege> emptyList();
      } catch (MetaException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void cancel_delegation_token(String token_str_form)
        throws MetaException, TException {
      startFunction("cancel_delegation_token");
      boolean success = false;
      Exception ex = null;
      try {
        HiveMetaStore.cancelDelegationToken(token_str_form);
        success = true;
      } catch (IOException e) {
        ex = e;
        throw new MetaException(e.getMessage());
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof TException) {
          throw (TException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("cancel_delegation_token", success, ex);
      }
    }

    @Override
    public long renew_delegation_token(String token_str_form)
        throws MetaException, TException {
      startFunction("renew_delegation_token");
      Long ret = null;
      Exception ex = null;
      try {
        ret = HiveMetaStore.renewDelegationToken(token_str_form);
      } catch (IOException e) {
        ex = e;
        throw new MetaException(e.getMessage());
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof TException) {
          throw (TException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("renew_delegation_token", ret != null, ex);
      }
      return ret;
    }

    @Override
    public String get_delegation_token(String token_owner,
        String renewer_kerberos_principal_name)
        throws MetaException, TException {
      startFunction("get_delegation_token");
      String ret = null;
      Exception ex = null;
      try {
        ret =
            HiveMetaStore.getDelegationToken(token_owner,
                renewer_kerberos_principal_name);
      } catch (IOException e) {
        ex = e;
        throw new MetaException(e.getMessage());
      } catch (InterruptedException e) {
        ex = e;
        throw new MetaException(e.getMessage());
      } catch (Exception e) {
        ex = e;
        if (e instanceof MetaException) {
          throw (MetaException) e;
        } else if (e instanceof TException) {
          throw (TException) e;
        } else {
          MetaException me = new MetaException(e.toString());
          me.initCause(e);
          throw me;
        }
      } finally {
        endFunction("get_delegation_token", ret != null, ex);
      }
      return ret;
    }

    @Override
    public void markPartitionForEvent(final String db_name, final String tbl_name,
        final Map<String, String> partName, final PartitionEventType evtType) throws
        MetaException, TException, NoSuchObjectException, UnknownDBException,
        UnknownTableException,
        InvalidPartitionException, UnknownPartitionException {

      Table tbl = null;
      Exception ex = null;
      try {
        startPartitionFunction("markPartitionForEvent", db_name, tbl_name, partName);
        firePreEvent(new PreLoadPartitionDoneEvent(db_name, tbl_name, partName, this));
        tbl = getMS().markPartitionForEvent(db_name, tbl_name, partName, evtType);
        if (null == tbl) {
          throw new UnknownTableException("Table: " + tbl_name + " not found.");
        } else {
          for (MetaStoreEventListener listener : listeners) {
            listener.onLoadPartitionDone(new LoadPartitionDoneEvent(true, tbl, partName, this));
          }
        }
      } catch (Exception original) {
        ex = original;
        LOG.error(original);
        if (original instanceof NoSuchObjectException) {
          throw (NoSuchObjectException) original;
        } else if (original instanceof UnknownTableException) {
          throw (UnknownTableException) original;
        } else if (original instanceof UnknownDBException) {
          throw (UnknownDBException) original;
        } else if (original instanceof UnknownPartitionException) {
          throw (UnknownPartitionException) original;
        } else if (original instanceof InvalidPartitionException) {
          throw (InvalidPartitionException) original;
        } else if (original instanceof MetaException) {
          throw (MetaException) original;
        } else {
          MetaException me = new MetaException(original.toString());
          me.initCause(original);
          throw me;
        }
      } finally {
                endFunction("markPartitionForEvent", tbl != null, ex);
      }
    }

    @Override
    public boolean isPartitionMarkedForEvent(final String db_name, final String tbl_name,
        final Map<String, String> partName, final PartitionEventType evtType) throws
        MetaException, NoSuchObjectException, UnknownDBException, UnknownTableException,
        TException, UnknownPartitionException, InvalidPartitionException {

      startPartitionFunction("isPartitionMarkedForEvent", db_name, tbl_name, partName);
      Boolean ret = null;
      Exception ex = null;
      try {
        ret = getMS().isPartitionMarkedForEvent(db_name, tbl_name, partName, evtType);
      } catch (Exception original) {
        LOG.error(original);
        ex = original;
        if (original instanceof NoSuchObjectException) {
          throw (NoSuchObjectException) original;
        } else if (original instanceof UnknownTableException) {
          throw (UnknownTableException) original;
        } else if (original instanceof UnknownDBException) {
          throw (UnknownDBException) original;
        } else if (original instanceof UnknownPartitionException) {
          throw (UnknownPartitionException) original;
        } else if (original instanceof InvalidPartitionException) {
          throw (InvalidPartitionException) original;
        } else if (original instanceof MetaException) {
          throw (MetaException) original;
        } else {
          MetaException me = new MetaException(original.toString());
          me.initCause(original);
          throw me;
        }
      } finally {
                endFunction("isPartitionMarkedForEvent", ret != null, ex);
      }

      return ret;
    }

    @Override
    public List<String> set_ugi(String username, List<String> groupNames) throws MetaException,
        TException {
      Collections.addAll(groupNames, username);
      return groupNames;
    }

    public Device create_device(String devid, int prop, String node_name) throws MetaException, TException {
      DeviceInfo di = new DeviceInfo();
      Node node = null;

      di.dev = devid;
      di.prop = prop;
      node = getMS().getNode(node_name);
      getMS().createOrUpdateDevice(di, node);

      Device d = getMS().getDevice(devid);
      return d;
    }

    public boolean del_device(String devid) throws MetaException, TException {
      return getMS().delDevice(devid);
    }

    @Override
    public SFile create_file(String node_name, int repnr, String db_name, String table_name)
        throws FileOperationException, TException {
      // TODO: if repnr less than 1, we should increase it to replicate to BACKUP-STORE
      if (repnr <= 1) {
        repnr++;
      }
      // do not select the backup/shared device for the first entry
      FileLocatingPolicy flp = new FileLocatingPolicy(null, dm.backupDevs, FileLocatingPolicy.EXCLUDE_NODES_DEVS_SHARED, false);
      return create_file(flp, node_name, repnr, db_name, table_name);
    }

    private SFile create_file_wo_location(int repnr, String dbName, String tableName)
      throws FileOperationException, TException {
      long table_id = -1;

      // try to parse table_name
      if (dbName != null && tableName != null) {
        Table tbl;
        try {
          tbl = getMS().getTable(dbName, tableName);
          table_id = getMS().getTableOID(dbName, tableName);
        } catch (MetaException me) {
          throw new FileOperationException("Invalid Table name:" + dbName + "/" + tableName, FOFailReason.INVALID_TABLE);
        }
        tableName = tbl.getDbName() + "/" + tbl.getTableName();
      }
      SFile cfile = new SFile(0, table_id, MetaStoreConst.MFileStoreStatus.INCREATE, repnr,
          "SFILE_DEFAULT_X", 0, 0, null, 0);
      getMS().createFile(cfile);
      cfile = getMS().getSFile(cfile.getFid());
      if (cfile == null) {
          throw new FileOperationException("Creating file with internal error, metadata inconsistent?", FOFailReason.INVALID_FILE);
      }

      return cfile;
    }

    private SFile create_file(FileLocatingPolicy flp, String node_name, int repnr, String db_name, String table_name)
        throws FileOperationException, TException {

      if (node_name == null) {
        // this means we should select Best Available Node and Best Available Device;
        try {
          node_name = dm.findBestNode(flp);
          if (node_name == null) {
            throw new IOException("Folloing the FLP, we can't find any available node now.");
          }
        } catch (IOException e) {
          LOG.error(e, e);
          throw new FileOperationException("Can not find any Best Available Node now, please retry", FOFailReason.SAFEMODE);
        }
      }

      SFile cfile = null;

      // Step 1: find best device to put a file
      if (dm == null) {
        return null;
      }
      try {
        if (flp == null) {
          flp = new FileLocatingPolicy(null, null, FileLocatingPolicy.EXCLUDE_NODES_DEVS_SHARED, true);
        }
        String devid = dm.findBestDevice(node_name, flp);
        long table_id = -1;

        if (devid == null) {
          throw new FileOperationException("Can not find any available device on node '" + node_name + "' now", FOFailReason.NOTEXIST);
        }
        // try to parse table_name
        if (db_name != null && table_name != null) {
          Table tbl;
          try {
            tbl = getMS().getTable(db_name, table_name);
            table_id = getMS().getTableOID(db_name, table_name);
          } catch (MetaException me) {
            throw new FileOperationException("Invalid Table name:" + db_name + "/" + table_name, FOFailReason.INVALID_TABLE);
          }
          table_name = tbl.getDbName() + "/" + tbl.getTableName();
        }

        // how to convert table_name to tbl_id?
        cfile = new SFile(0, table_id, MetaStoreConst.MFileStoreStatus.INCREATE, repnr,
            "SFILE_DEFALUT", 0, 0, null, 0);
        getMS().createFile(cfile);
        cfile = getMS().getSFile(cfile.getFid());
        if (cfile == null) {
          throw new FileOperationException("Creating file with internal error, metadata inconsistent?", FOFailReason.INVALID_FILE);
        }

        do {
          String location = "/data/";

          if (table_name == null) {
            location += "UNNAMED-DB/UNNAMED-TABLE/" + rand.nextInt(Integer.MAX_VALUE);
          } else {
            location += table_name + "/" + rand.nextInt(Integer.MAX_VALUE);
          }
          SFileLocation sfloc = new SFileLocation(node_name, cfile.getFid(), devid, location, 0, System.currentTimeMillis(),
              MetaStoreConst.MFileLocationVisitStatus.OFFLINE, "SFL_DEFAULT");
          if (!getMS().createFileLocation(sfloc)) {
            continue;
          }
          List<SFileLocation> sfloclist = new ArrayList<SFileLocation>();
          sfloclist.add(sfloc);
          cfile.setLocations(sfloclist);
          break;
        } while (true);
      } catch (IOException e) {
        throw new FileOperationException("System might in Safe Mode, please wait ... {" + e + "}", FOFailReason.SAFEMODE);
      }

      return cfile;
    }

    @Override
    public int close_file(SFile file) throws FileOperationException, MetaException, TException {
      SFile saved = getMS().getSFile(file.getFid());
      if (saved == null) {
        throw new FileOperationException("Can not find SFile by FID" + file.getFid(), FOFailReason.INVALID_FILE);
      }

      if (saved.getStore_status() != MetaStoreConst.MFileStoreStatus.INCREATE) {
        throw new FileOperationException("File StoreStatus is not in INCREATE (vs " + saved.getStore_status() + ").",
            FOFailReason.INVALID_STATE);
      }

      file.setStore_status(MetaStoreConst.MFileStoreStatus.CLOSED);
      // keep repnr unchanged
      file.setRep_nr(saved.getRep_nr());
      getMS().updateSFile(file);

      SFileLocation sfl = file.getLocations().get(0);
      assert sfl != null;
      sfl.setVisit_status(MetaStoreConst.MFileLocationVisitStatus.ONLINE);
      sfl.setDigest(file.getDigest());
      getMS().updateSFileLocation(sfl);

      synchronized (dm.repQ) {
        dm.repQ.add(new DMRequest(file, DMRequest.DMROperation.REPLICATE, 1));
        dm.repQ.notify();
      }
      return 0;
    }

    public void identifySharedDevice(List<SFileLocation> lsfl) throws MetaException, NoSuchObjectException {
      if (lsfl == null) {
        return;
      }
      for (SFileLocation sfl : lsfl) {
        Device d = getMS().getDevice(sfl.getDevid());
        if (d.getProp() == MetaStoreConst.MDeviceProp.SHARED ||
            d.getProp() == MetaStoreConst.MDeviceProp.BACKUP) {
          sfl.setNode_name("");
        }
      }
    }

    @Override
    public SFile get_file_by_id(long fid) throws FileOperationException, MetaException, TException {
      SFile r = getMS().getSFile(fid);
      if (r == null) {
        throw new FileOperationException("Can not find SFile by FID " + fid, FOFailReason.INVALID_FILE);
      }

      switch (r.getStore_status()) {
      case MetaStoreConst.MFileStoreStatus.RM_LOGICAL:
      case MetaStoreConst.MFileStoreStatus.RM_PHYSICAL:
        break;
      default:
        r.setLocations(getMS().getSFileLocations(fid));
      }
      identifySharedDevice(r.getLocations());

      return r;
    }

    @Override
    public int rm_file_logical(SFile file) throws FileOperationException, MetaException, TException {
      SFile saved = getMS().getSFile(file.getFid());
      if (saved == null) {
        throw new FileOperationException("Can not find SFile by FID" + file.getFid(), FOFailReason.INVALID_FILE);
      }

      // only in REPLICATED state can step into RM_LOGICAL
      if (saved.getStore_status() != MetaStoreConst.MFileStoreStatus.REPLICATED) {
        throw new FileOperationException("File StoreStatus is not in REPLICATED.", FOFailReason.INVALID_STATE);
      }
      saved.setStore_status(MetaStoreConst.MFileStoreStatus.RM_LOGICAL);
      getMS().updateSFile(saved);
      return 0;
    }

    @Override
    public int restore_file(SFile file) throws FileOperationException, MetaException, TException {
      SFile saved = getMS().getSFile(file.getFid());
      if (saved == null) {
        throw new FileOperationException("Can not find SFile by FID" + file.getFid(), FOFailReason.INVALID_FILE);
      }

      if (saved.getStore_status() != MetaStoreConst.MFileStoreStatus.RM_LOGICAL) {
        throw new FileOperationException("File StoreStatus is not in RM_LOGICAL.", FOFailReason.INVALID_STATE);
      }
      saved.setStore_status(MetaStoreConst.MFileStoreStatus.REPLICATED);
      getMS().updateSFile(saved);
      return 0;
    }

    @Override
    public int rm_file_physical(SFile file) throws FileOperationException, MetaException,
        TException {
      SFile saved = getMS().getSFile(file.getFid());
      if (saved == null) {
        throw new FileOperationException("Can not find SFile by FID" + file.getFid(), FOFailReason.INVALID_FILE);
      }

      if (!(saved.getStore_status() == MetaStoreConst.MFileStoreStatus.INCREATE ||
          saved.getStore_status() == MetaStoreConst.MFileStoreStatus.REPLICATED ||
          saved.getStore_status() == MetaStoreConst.MFileStoreStatus.RM_LOGICAL)) {
        throw new FileOperationException("File StoreStatus is not in INCREATE/REPLICATED/RM_LOGICAL.", FOFailReason.INVALID_STATE);
      }
      saved.setStore_status(MetaStoreConst.MFileStoreStatus.RM_PHYSICAL);
      file = getMS().updateSFile(saved);
      file.setLocations(getMS().getSFileLocations(file.getFid()));
      synchronized (dm.cleanQ) {
        dm.cleanQ.add(new DMRequest(file, DMRequest.DMROperation.RM_PHYSICAL, 0));
        dm.cleanQ.notify();
      }
      return 0;
    }

    @Override
    public Node add_node(String node_name, List<String> ipl) throws MetaException, TException {
      Node node = new Node(node_name, ipl, MetaStoreConst.MNodeStatus.ONLINE);
      getMS().createNode(node);
      if (dm != null) {
        dm.SafeModeStateChange();
      }
      return node;
    }

    @Override
    public int del_node(String node_name) throws MetaException, TException {
      if (getMS().delNode(node_name)) {
        if (dm != null) {
          dm.SafeModeStateChange();
        }
        return 1;
      } else {
        return 0;
      }
    }

    @Override
    public Node alter_node(String node_name, List<String> ipl, int status) throws MetaException,
        TException {
      Node node = new Node(node_name, ipl, status);
      if (getMS().updateNode(node)) {
        return node;
      } else {
        throw new MetaException("Alter node " + node_name + " failed.");
      }
    }

    @Override
    public Node get_node(String node_name) throws MetaException, TException {
      Node n = getMS().getNode(node_name);
      if (n == null) {
        throw new MetaException("Can not find Node " + node_name);
      }
      return n;
    }

    @Override
    public List<Node> find_best_nodes(int nr) throws MetaException, TException {
      if (nr > 0) {
        try {
          return dm.findBestNodes(nr);
        } catch (IOException e) {
          throw new MetaException(e.getMessage());
        }
      } else if (nr < 0) {
        // call findBestNodesBySingleDev
        try {
          return dm.findBestNodesBySingleDev(-nr);
        } catch (IOException e) {
          throw new MetaException(e.getMessage());
        }
      }

      return new ArrayList<Node>();
    }

    @Override
    public List<Node> get_all_nodes() throws MetaException, TException {
      return getMS().getAllNodes();
    }

    @Override
    public void create_datacenter(Datacenter datacenter) throws AlreadyExistsException,
        InvalidObjectException, MetaException, TException {
      getMS().createDatacenter(datacenter);
    }

    @Override
    public Datacenter get_center(String name) throws NoSuchObjectException, MetaException,
        TException {
      if (hiveConf.getBoolVar(HiveConf.ConfVars.IS_TOP_DATACENTER)) {
        return getMS().getDatacenter(name);
      } else {
        if (HMSHandler.topdcli == null) {
          connect_to_top_dc(hiveConf);
        }
        return HMSHandler.topdcli.get_center(name);
      }
    }

    @Override
    public void drop_center(String name, boolean deleteData, boolean cascade)
        throws NoSuchObjectException, InvalidOperationException, MetaException, TException {
      // TODO Auto-generated method stub
      throw new MetaException("Not implemented yet!");
    }

    @Override
    public List<Datacenter> get_all_centers() throws MetaException, TException {
      // try to get all centers from top-level metastore
      if (hiveConf.getBoolVar(HiveConf.ConfVars.IS_TOP_DATACENTER)) {
        return getMS().getAllDatacenters();
      } else {
        if (HMSHandler.topdcli == null) {
          connect_to_top_dc(hiveConf);
        }
        try {
          if (HMSHandler.topdcli != null) {
            return HMSHandler.topdcli.get_all_centers();
          } else {
            throw new MetaException("Invalid top DC client handler.");
          }
        } catch (TException e) {
          LOG.error(e, e);
          HMSHandler.topdcli = null;
          throw e;
        }
      }
    }

    @Override
    public Datacenter get_local_center() throws MetaException, TException {
      String local_dc = hiveConf.getVar(HiveConf.ConfVars.LOCAL_DATACENTER);
      if (local_dc == null) {
        throw new MetaException("Please set hive.datacenter.local=NAME in config file.");
      }
      return getMS().getDatacenter(local_dc);
    }

    @Override
    public List<String> get_lucene_index_names(String db_name, String tbl_name, short max_indexes)
        throws MetaException, TException {
      // TODO Auto-generated method stub
      throw new MetaException("Not implemented yet!");
    }

    @Override
    public int add_partition_files(Partition part, List<SFile> files) throws TException {
      Partition p = getMS().getPartition(part.getDbName(), part.getTableName(), part.getPartitionName());
      List<Long> nl = new ArrayList<Long>();
      Set<Long> tmp = new TreeSet<Long>();

      for (SFile f : files) {
        tmp.add(new Long(f.getFid()));
      }
      if (p.getFiles() != null) {
        tmp.addAll(p.getFiles());
      }
      for (Long l : tmp) {
        nl.add(l);
      }
      p.setFiles(nl);

      LOG.info("Begin add partition files " + part.getPartitionName() + " fileset's size " + nl.size());
      getMS().updatePartition(p);
      synchronized (dm.backupQ) {
        BackupEntry be = new BackupEntry(part, files, BackupEntry.FOP.ADD_PART);
        dm.backupQ.add(be);
      }
      return 0;
    }

    @Override
    public int drop_partition_files(Partition part, List<SFile> files) throws TException {
      Partition p = getMS().getPartition(part.getDbName(), part.getTableName(), part.getPartitionName());
      if (part.getFilesSize() == 0 || files == null || files.size() == 0) {
        return 0;
      }
      List<Long> old_files = part.getFiles();
      List<Long> new_files = new ArrayList<Long>();
      new_files.addAll(old_files);
      for (SFile f : files) {
        new_files.remove(f.getFid());
      }
      p.setFiles(new_files);
      LOG.info("Begin drop partition files " + p.getPartitionName() + " fileset's size " + new_files.size());
      getMS().updatePartition(p);
      synchronized (dm.backupQ) {
        BackupEntry be = new BackupEntry(part, files, BackupEntry.FOP.DROP_PART);
        dm.backupQ.add(be);
      }
      return 0;
    }

    @Override
    public int add_subpartition_files(Subpartition subpart, List<SFile> files) throws TException {
      Subpartition p = getMS().getSubpartition(subpart.getDbName(), subpart.getTableName(), subpart.getPartitionName());
      List<Long> nl = new ArrayList<Long>();
      Set<Long> tmp = new TreeSet<Long>();

      for (SFile f : files) {
        tmp.add(new Long(f.getFid()));
      }
      if (p.getFiles() != null) {
        tmp.addAll(p.getFiles());
      }
      for (Long l : tmp) {
        nl.add(l);
      }
      p.setFiles(nl);

      LOG.info("Begin add subpartition files " + subpart.getPartitionName() + " fileset's size " + nl.size());
      getMS().updateSubpartition(p);
      synchronized (dm.backupQ) {
        BackupEntry be = new BackupEntry(subpart, files, BackupEntry.FOP.ADD_SUBPART);
        dm.backupQ.add(be);
      }


      HashMap<String,Object> old_params= new HashMap<String,Object>();

      old_params.put("f_id", tmp);
      old_params.put("partition_name", subpart.getPartitionName());
      old_params.put("parent_partition_name", getMS().getParentPartition(
          subpart.getDbName(), subpart.getTableName(),  subpart.getPartitionName()).getPartitionName());
      old_params.put("partition_level", 2);
      old_params.put("db_name", subpart.getDbName());
      old_params.put("table_name", subpart.getTableName());
      MetaMsgServer.sendMsg(MSGFactory.generateDDLMsgs(MSGType.MSG_NEW_PARTITION_FILE,-1l,-1l, null,nl,old_params));
      return 0;
    }

    @Override
    public int drop_subpartition_files(Subpartition subpart, List<SFile> files) throws TException {
      Subpartition p = getMS().getSubpartition(subpart.getDbName(), subpart.getTableName(), subpart.getPartitionName());
      if (subpart.getFilesSize() == 0 || files == null || files.size() == 0) {
        return 0;
      }
      List<Long> old_files = subpart.getFiles();
      List<Long> new_files = new ArrayList<Long>();
      new_files.addAll(old_files);
      for (SFile f : files) {
        new_files.remove(f.getFid());
      }
      p.setFiles(new_files);
      LOG.info("Begin drop subpartition files " + subpart.getPartitionName() + " fileset's size " + new_files.size());
      getMS().updateSubpartition(p);
      synchronized (dm.backupQ) {
        BackupEntry be = new BackupEntry(subpart, files, BackupEntry.FOP.DROP_SUBPART);
        dm.backupQ.add(be);
      }

      HashMap<String,Object> old_params= new HashMap<String,Object>();

      List<Long> tmp = new ArrayList<Long>();
      for (SFile f : files) {
        tmp.add(f.getFid());
      }
      old_params.put("f_id", tmp);
      old_params.put("partition_name", subpart.getPartitionName());
      old_params.put("parent_partition_name", getMS().getParentPartition(
          subpart.getDbName(), subpart.getTableName(),  subpart.getPartitionName()).getPartitionName());
      old_params.put("partition_level", 2);
      old_params.put("db_name", subpart.getDbName());
      old_params.put("table_name", subpart.getTableName());
      MetaMsgServer.sendMsg(MSGFactory.generateDDLMsgs(MSGType.MSG_DEL_PARTITION_FILE,-1l,-1l, null,tmp,old_params));
      return 0;
    }

    @Override
    public boolean add_partition_index(Index index, Partition part) throws MetaException, AlreadyExistsException, TException {
      getMS().createPartitionIndex(index, part);
      return true;
    }

    @Override
    public boolean drop_partition_index(Index index, Partition part) throws MetaException, InvalidObjectException, TException {
      getMS().dropPartitionIndex(index, part);
      return true;
    }

    @Override
    public boolean add_subpartition_index(Index index, Subpartition part) throws MetaException, AlreadyExistsException, TException {
      getMS().createPartitionIndex(index, part);
      return true;
    }

    @Override
    public boolean drop_subpartition_index(Index index, Subpartition part) throws MetaException, InvalidObjectException, TException {
      getMS().dropPartitionIndex(index, part);
      return true;
    }

    @Override
    public boolean add_partition_index_files(Index index, Partition part, List<SFile> file,
        List<Long> originfid) throws MetaException, TException {
      if (file.size() != originfid.size()) {
        return false;
      }
      getMS().createPartitionIndexStores(index, part, file, originfid);
      return true;
    }

    @Override
    public boolean add_subpartition(String dbname, String tbl_name, List<String> part_vals,
        Subpartition sub_part) throws TException {
      // TODO Auto-generated method stub
      return true;
    }

    @Override
    public boolean drop_partition_index_files(Index index, Partition part, List<SFile> file)
        throws MetaException, TException {
      return getMS().dropPartitionIndexStores(index, part, file);
    }

    @Override
    public List<Subpartition> get_subpartitions(String dbname, String tbl_name, Partition part)
        throws TException {
      return getMS().getSubpartitions(dbname, tbl_name, part);
    }

    @Override
    public String getDMStatus() throws MetaException, TException {
      if (dm != null) {
        return dm.getDMStatus();
      }
      return "+FAIL: No DiskManger!\n";
    }

    @Override
    public boolean add_datawarehouse_sql(int dwnum, String sql) throws InvalidObjectException,
        MetaException, TException {
      getMS().add_datawarehouse_sql(dwnum,  sql);
      return false;
    }



    public List<SFileRef> get_partition_index_files(Index index, Partition part)
        throws MetaException, TException {
      return getMS().getPartitionIndexFiles(index, part);
    }

    @Override
    public boolean add_subpartition_index_files(Index index, Subpartition subpart,
        List<SFile> file, List<Long> originfid) throws MetaException, TException {
      if (file.size() != originfid.size()) {
        return false;
      }
      getMS().createPartitionIndexStores(index, subpart, file, originfid);
      return true;
    }

    @Override
    public List<SFileRef> get_subpartition_index_files(Index index, Subpartition subpart)
        throws MetaException, TException {
      return getMS().getSubpartitionIndexFiles(index, subpart);
    }

    @Override
    public boolean drop_subpartition_index_files(Index index, Subpartition subpart, List<SFile> file)
        throws MetaException, TException {
      return getMS().dropPartitionIndexStores(index, subpart, file);
    }

    @Override
    public List<BusiTypeColumn> get_all_busi_type_cols() throws MetaException, TException {
      return getMS().getAllBusiTypeCols();
    }

    public void update_center(Datacenter datacenter) throws NoSuchObjectException,
        InvalidOperationException, MetaException, TException {
      getMS().updateDatacenter(datacenter);
    }

    private Partition deepCopy(Partition partition) {
      Partition copy = null;
      if (partition != null) {
        copy = new Partition(partition);
      }
      return copy;
    }

    private List<Partition> deepCopyPartitions(List<Partition> partitions) {
      List<Partition> copy = null;
      if (partitions != null) {
        copy = new ArrayList<Partition>();
        for (Partition part : partitions) {
          copy.add(deepCopy(part));
        }
      }
      return copy;
    }

    @Override
    public Map<Long, SFile> migrate_in(Table tbl, List<Partition> parts, String from_dc)
        throws MetaException, TException {
      LOG.info("server parts2 " + parts.get(0).getSubpartitions().get(0).getPartitionName() + ", " + parts.get(0).getSubpartitions().get(0).getFiles());
      Map<Long, SFile> rmap = new HashMap<Long, SFile>();

      // try to create the database, if it doesn't exist
      Datacenter ldc = null;

      if (hiveConf.getVar(HiveConf.ConfVars.LOCAL_DATACENTER) == null) {
        throw new MetaException("Please set 'hive.datacenter.local' as the local DC name");
      }
      try {
        ldc = getMS().getDatacenter(hiveConf.getVar(HiveConf.ConfVars.LOCAL_DATACENTER));
      } catch (NoSuchObjectException e) {
        ldc = new Datacenter(hiveConf.getVar(HiveConf.ConfVars.LOCAL_DATACENTER), null, HMSHandler.msUri == null ? "DEFAULT_URI" : HMSHandler.msUri, null);
        getMS().createDatacenter(ldc);
      }

      try {
        getMS().getDatabase(tbl.getDbName());
      } catch (NoSuchObjectException e) {
        Database db = new Database(tbl.getDbName(), DEFAULT_DATABASE_COMMENT,
                wh.getDefaultDatabasePath(tbl.getDbName()).toString(), null);
        db.setDatacenter(ldc);
        getMS().createDatabase(db);
      }
      // try to create the table, if it doesn't exist
      try {
        create_table(tbl);
      } catch (AlreadyExistsException e) {
        // it is ok, ignore it.
      }
      LOG.info("Create table " + tbl.getTableName() + " done.");

      // try to create the partition, if it doesn't exist
      List<Partition> toAdd = new ArrayList<Partition>();
      List<Partition> toUpdate = new ArrayList<Partition>();
      List<Partition> copy_parts = deepCopyPartitions(parts);
      for (Partition part : copy_parts) {
        LOG.info("Handle partition1 " + part.getPartitionName() + ", subparts NR " + part.getSubpartitionsSize());
        try {
          getMS().getPartition(part.getDbName(), part
              .getTableName(), part.getPartitionName());
        } catch (NoSuchObjectException e) {
          // this means there is no existing partition, it is ok
          toAdd.add(part);
          continue;
        }
        toUpdate.add(part);
      }
      add_partitions(toAdd);
      LOG.info("Add partitions done.");

      // try to create the file
      for (Partition part : parts) {
        LOG.info("Handle partition2 " + part.getPartitionName() + ", subparts NR " + part.getSubpartitionsSize());
        if (part.getSubpartitionsSize() > 0) {
          for (Subpartition subpart : part.getSubpartitions()) {
            LOG.info("subparts " + subpart.getPartitionName() + ", " + subpart.getFiles());
            // handle files
            Subpartition localsubpart = getMS().getSubpartition(subpart.getDbName(), subpart.getTableName(), subpart.getPartitionName());
            List<SFile> files = new ArrayList<SFile>();

            if (localsubpart == null) {
              throw new MetaException("Invalid local subpart: " + subpart.getPartitionName());
            }

            if (subpart.getFilesSize() > 0) {
              for (long fid : subpart.getFiles()) {
                // create a new target file
                SFile f = create_file(null, 3, tbl.getDbName(), tbl.getTableName());
                files.add(f);
                rmap.put(fid, f);
              }
            }
            add_subpartition_files(localsubpart, files);
          }
        } else {
          // handle files
          if (part.getFilesSize() > 0) {
            Partition localpart = getMS().getPartition(part.getDbName(), part.getTableName(), part.getPartitionName());
            List<SFile> files = new ArrayList<SFile>();

            if (localpart == null) {
              throw new MetaException("Invalid local part: " + part.getPartitionName());
            }
            LOG.info("parts " + part.getPartitionName() + ", " + part.getFiles().toString());

            for (long fid : part.getFiles()) {
              // create a new target file
              SFile f = create_file(null, 3, tbl.getDbName(), tbl.getTableName());
              files.add(f);
              rmap.put(fid, f);
            }
            add_partition_files(localpart, files);
          }
        }
      }

      LOG.info("OK, we will migrate DC " + from_dc + " DB " + tbl.getDbName() + " Table " +
          tbl.getTableName() + "'s " + parts.size() + " Partition(s) w/ " + rmap.size() +
          " files to local DC.");

      return rmap;
    }

    @Override
    public boolean migrate_out(String dbName, String tableName, List<String> partNames, String to_dc)
        throws MetaException, TException {
      // prepare datacenter connection
      if (HMSHandler.topdcli == null) {
        connect_to_top_dc(hiveConf);
        if (HMSHandler.topdcli == null) {
          throw new MetaException("Top-level DC metastore is null, please check!");
        }
      }
      if (hiveConf.getVar(ConfVars.LOCAL_DATACENTER) == null) {
        throw new MetaException("Please set 'hive.datacenter.local' as local datacenter NAME.");
      }
      Datacenter rdc = HMSHandler.topdcli.get_center(to_dc);
      Datacenter ldc = get_center(hiveConf.getVar(ConfVars.LOCAL_DATACENTER));

      IMetaStoreClient rcli = new HiveMetaStoreClient(rdc.getLocationUri(),
            HiveConf.getIntVar(hiveConf, HiveConf.ConfVars.METASTORETHRIFTCONNECTIONRETRIES),
            hiveConf.getIntVar(ConfVars.METASTORE_CLIENT_CONNECT_RETRY_DELAY),
            null);

      // prepare tbl, parts
      Table tbl = get_table(dbName, tableName);
      Map<String, String> kvs = new HashMap<String, String>();
      if (tbl.getParametersSize() > 0) {
        LOG.info(tbl.getParameters().toString());
        kvs.putAll(tbl.getParameters());
        kvs.put("store.remote", "both");
        if (kvs.get("store.remote.dcs") == null) {
          kvs.put("store.remote.dcs", to_dc + "," + hiveConf.getVar(ConfVars.LOCAL_DATACENTER));
        } else {
          String olddcs = kvs.get("store.remote.dcs");
          String[] dcsarray = olddcs.split(",");
          boolean ign = false;
          for (int i = 0; i < dcsarray.length; i++) {
            if (dcsarray[i].equals(to_dc)) {
              ign = true;
              break;
            }
          }
          if (!ign) {
            olddcs += "," + to_dc;
          }
          kvs.put("store.remote.dcs", olddcs);
        }
        tbl.setParameters(kvs);
      } else {
        tbl.setParameters(kvs);
      }
      LOG.info("parts: " + partNames.toString());
      List<Partition> parts = get_partitions_by_names(dbName, tableName, partNames);
      if (parts.size() == 0) {
        LOG.info("Zero partition list, do not migrate!");
        rcli.close();
        return false;
      }
      for (Partition p : parts) {
        LOG.info("p " + p.getPartitionName() + " subparts " + p.getSubpartitionsSize());
        for (Subpartition sp : p.getSubpartitions()) {
          LOG.info("sp " + sp.getPartitionName() + " files " + sp.getFiles().toString());
        }
      }

      // call remote metastore's migrate_in to prepare metadata
      Map<Long, SFile> rmap = rcli.migrate_in(tbl, parts, ldc.getName());

      // iterate the value set to generate a devmap
      Map<String, String> devmap = new HashMap<String, String>();
      for (Map.Entry<Long, SFile> e : rmap.entrySet()) {
        SFileLocation sfl = e.getValue().getLocations().get(0);
        String mp = rcli.getMP(sfl.getNode_name(), sfl.getDevid());
        devmap.put(sfl.getDevid(), mp);
      }

      rcli.close();

      // construct a replicate request to source node
      if (rmap.size() > 0) {
        for (Map.Entry<Long, SFile> e : rmap.entrySet()) {
          SFileLocation sfl = e.getValue().getLocations().get(0);
          LOG.info("------migrate----fid " + e.getKey() + "----to---->" + sfl.getNode_name() + ":" +
              sfl.getDevid() + ":" + sfl.getLocation());
          SFile source = get_file_by_id(e.getKey());
          DMRequest dmr = new DMRequest(source, e.getValue(), devmap, to_dc);
          synchronized (dm.repQ) {
            dm.repQ.add(dmr);
            dm.repQ.notify();
          }
        }
      }

      // create triple-id map
      Map<String, Long> timap = new HashMap<String, Long>();
      for (Map.Entry<Long, SFile> e : rmap.entrySet()) {
          SFileLocation sfl = e.getValue().getLocations().get(0);
          timap.put(new SFLTriple(sfl.getNode_name(), sfl.getDevid(), sfl.getLocation()).toString(), e.getKey());
      }

      // add the entries to migrate set
      MigrateEntry thisme = null;
      Map<Long, MigrateEntry> id2me = new HashMap<Long, MigrateEntry>();
      for (Partition part : parts) {
        if (part.getSubpartitionsSize() > 0) {
          for (Subpartition subpart : part.getSubpartitions()) {
            if (subpart.getFilesSize() > 0) {
              thisme = new MigrateEntry(to_dc, subpart, subpart.getFiles(), timap);
              for (Long fid : subpart.getFiles()) {
                id2me.put(fid, thisme);
              }
            }
          }
        } else if (part.getFilesSize() > 0) {
          thisme = new MigrateEntry(to_dc, part, part.getFiles(), timap);
          for (Long fid : part.getFiles()) {
            id2me.put(fid, thisme);
          }
        }
      }

      // add to global rrmap: triple to migrateentry
      if (thisme != null) {
        for (Map.Entry<Long, SFile> e : rmap.entrySet()) {
          SFileLocation sfl = e.getValue().getLocations().get(0);
          synchronized (dm.rrmap) {
            Long fid = timap.get(new SFLTriple(sfl.getNode_name(), sfl.getDevid(), sfl.getLocation()).toString());
            dm.rrmap.put(new SFLTriple(sfl.getNode_name(), sfl.getDevid(), sfl.getLocation()).toString(), id2me.get(fid));
          }
        }
      }

      alter_table(dbName, tableName, tbl);
      LOG.info("Update table properties to reflect the migration.");

      return true;
    }

    // Migrate2 use NAS-WAN-NAS fashion migration,
    // In stage1, we get the file list and generate NAS file location list.
    public List<SFileLocation> migrate2_stage1(String dbName, String tableName, List<String> partNames, String to_dc)
      throws MetaException, TException {
      List<SFileLocation> r = new ArrayList<SFileLocation>();

      // prepare parts
      LOG.info("parts: " + partNames.toString());
      List<Partition> parts = get_partitions_by_names(dbName, tableName, partNames);
      if (parts.size() == 0) {
        LOG.info("Zero partition list, do not migrate!");
        return r;
      }
      for (Partition p : parts) {
        LOG.info("p " + p.getPartitionName() + " subparts " + p.getSubpartitionsSize());
        for (Subpartition sp : p.getSubpartitions()) {
          LOG.info("sp " + sp.getPartitionName() + " files " + sp.getFiles().toString());
          if (sp.getFilesSize() > 0) {
            for (long fid : sp.getFiles()) {
              SFile f = get_file_by_id(fid);
              // check file status
              if (f.getStore_status() == MetaStoreConst.MFileStoreStatus.INCREATE ||
                  f.getStore_status() == MetaStoreConst.MFileStoreStatus.CLOSED) {
                LOG.warn("Invalid file (fid " + fid + ") status (INCREATE or CLOSED).");
                r.clear();
                return r;
              }
              boolean added = false;
              if (f != null && f.getLocationsSize() > 0) {
                for (SFileLocation sfl : f.getLocations()) {
                  if (sfl.getNode_name().equals("")) {
                    // this is the NAS location, record it
                    r.add(sfl);
                    LOG.info("sp -> NAS SFL: DEV " + sfl.getDevid() + ", LOC " + sfl.getLocation());
                    added = true;
                    break;
                  }
                }
                if (!added) {
                  // record a non-NAS location
                  SFileLocation sfl = f.getLocations().get(0);
                  r.add(sfl);
                  LOG.info("sp -> NAS SFL: DEV " + sfl.getDevid() + ", LOC " + sfl.getLocation());
                }
              }
            }
          }
        }
      }

      return r;
    }

    @Override
    public String getMP(String node_name, String devid) throws MetaException, TException {
      if (dm == null) {
        throw new MetaException("Invalid DiskManager!");
      }
      return dm.getMP(node_name, devid);
    }

    private boolean isTopDc(){
      return hiveConf.getBoolVar(HiveConf.ConfVars.IS_TOP_DATACENTER);
    }

    @Override
    public List<BusiTypeDatacenter> get_all_busi_type_datacenters() throws MetaException,
        TException {

      if(isTopDc()){
        return this.getMS().get_all_busi_type_datacenters();
      }else{
        if(topdcli != null){
          return topdcli.get_all_busi_type_datacenters();
        }else{
          throw new MetaException("Top datacenter is not reachable!");
        }
      }
    }



    @Override
    public void append_busi_type_datacenter(BusiTypeDatacenter busiTypeDatacenter)
        throws InvalidObjectException, MetaException, TException {
      if(isTopDc()){
        getMS().append_busi_type_datacenter(busiTypeDatacenter);
      }else{
        if(topdcli != null){
          topdcli.append_busi_type_datacenter(busiTypeDatacenter);
        }else{
          throw new MetaException("Top datacenter is not reachable!");
        }
      }
    }

    @Override
    public SFile get_file_by_name(String node, String devid, String location)
        throws FileOperationException, MetaException, TException {
      SFile r = getMS().getSFile(devid, location);
      if (r == null) {
        throw new FileOperationException("Can not find SFile by name: " + node + ":" + devid + ":" + location, FOFailReason.INVALID_FILE);
      }

      switch (r.getStore_status()) {
      case MetaStoreConst.MFileStoreStatus.RM_LOGICAL:
      case MetaStoreConst.MFileStoreStatus.RM_PHYSICAL:
        break;
      default:
        r.setLocations(getMS().getSFileLocations(r.getFid()));
      }
      identifySharedDevice(r.getLocations());

      return r;

    }

    @Override
    // Migrate2 use NAS-WAN-NAS fashion migration
    // In stage2, we create the metadata structures in remote dc, and update remote/local file relations
    public boolean migrate2_stage2(String dbName, String tableName, List<String> partNames,
        String to_dc, String to_db, String to_nas_devid) throws MetaException, TException {
      // prepare datacenter connection
      if (HMSHandler.topdcli == null) {
        connect_to_top_dc(hiveConf);
        if (HMSHandler.topdcli == null) {
          throw new MetaException("Top-level DC metastore is null, please check!");
        }
      }
      if (hiveConf.getVar(ConfVars.LOCAL_DATACENTER) == null) {
        throw new MetaException("Please set 'hive.datacenter.local' as local datacenter NAME.");
      }
      Datacenter rdc = HMSHandler.topdcli.get_center(to_dc);
      Datacenter ldc = get_center(hiveConf.getVar(ConfVars.LOCAL_DATACENTER));

      IMetaStoreClient rcli = new HiveMetaStoreClient(rdc.getLocationUri(),
            HiveConf.getIntVar(hiveConf, HiveConf.ConfVars.METASTORETHRIFTCONNECTIONRETRIES),
            hiveConf.getIntVar(ConfVars.METASTORE_CLIENT_CONNECT_RETRY_DELAY),
            null);

      // prepare tbl
      Table tbl = get_table(dbName, tableName);
      // use current DC name as the remote db name?
      String rdb;
      if (to_db == null || to_db.equals("")) {
        rdb = "rdb_" + hiveConf.getVar(ConfVars.LOCAL_DATACENTER);
      } else {
        rdb = to_db;
      }
      // change tbl's dbname to rdb
      tbl.setDbName(rdb);

      // set tbl properties
      Map<String, String> kvs = new HashMap<String, String>();
      boolean isMaster = false;
      if (tbl.getParametersSize() > 0) {
        LOG.info(tbl.getParameters().toString());
        kvs.putAll(tbl.getParameters());
        kvs.put("store.remote", "both");
        if (kvs.get("store.identify") == null) {
          kvs.put("store.identify", "slave");
          isMaster = true;
        } else {
          if (kvs.get("store.identify").equalsIgnoreCase("slave")) {
            // this means migrate from slave table back to master table
            LOG.info("Migrate from slave table to other table (slave?).");
            isMaster = false;
          } else {
            LOG.info("Migrate from master table again.");
            kvs.put("store.identify", "slave");
            isMaster = true;
          }
        }

        if (kvs.get("store.remote.dcs") == null) {
          kvs.put("store.remote.dcs", to_dc + "." + rdb +
              "," + hiveConf.getVar(ConfVars.LOCAL_DATACENTER) + "." + dbName);
        } else {
          String olddcs = kvs.get("store.remote.dcs");
          String[] dcsarray = olddcs.split(",");
          boolean ign = false;
          for (int i = 0; i < dcsarray.length; i++) {
            if (dcsarray[i].equals(to_dc + "." + rdb)) {
              ign = true;
              break;
            }
          }
          if (!ign) {
            olddcs += "," + to_dc + "." + rdb;
          }
          kvs.put("store.remote.dcs", olddcs);
        }
        tbl.setParameters(kvs);
      } else {
        tbl.setParameters(kvs);
      }

      // prepare index
      short maxIndexNum = 1000;
      List<Index> idxs = get_indexes(dbName, tableName, maxIndexNum);
      if (idxs != null && idxs.size() > 0) {
        for (Index i : idxs) {
          i.setDbName(rdb);
          LOG.info("IDX -> " + i.getIndexName() + ", " + i.getParameters().toString());
        }
      }

      // prepare parts
      LOG.info("parts: " + partNames.toString());
      List<Partition> parts = get_partitions_by_names(dbName, tableName, partNames);
      if (parts.size() == 0) {
        LOG.info("Zero partition list, do not migrate!");
        rcli.close();
        return false;
      }
      Map<Long, SFileLocation> targetFileMap = new HashMap<Long, SFileLocation>();

      for (Partition p : parts) {
        LOG.info("p " + p.getPartitionName() + " subparts " + p.getSubpartitionsSize());
        if (p.getSubpartitionsSize() > 0) {
          for (Subpartition sp : p.getSubpartitions()) {
            LOG.info("sp " + sp.getPartitionName() + " files " + sp.getFiles().toString());
            if (sp.getFilesSize() > 0) {
              for (long fid : sp.getFiles()) {
                SFile f = get_file_by_id(fid);
                if (f != null && f.getLocationsSize() > 0) {
                  for (SFileLocation sfl : f.getLocations()) {
                    if (sfl.getNode_name().equals("")) {
                      // this is the NAS location, record it
                      LOG.info("sp -> SFL: DEV " + sfl.getDevid() + ", LOC " + sfl.getLocation());
                      sfl.setDevid(to_nas_devid);
                      sfl.setDigest("MIGRATE2-DIGESTED!");
                      targetFileMap.put(fid, sfl);
                    }
                  }
                }
              }
            }
          }
        } else {
          if (p.getFilesSize() > 0) {
            for (long fid : p.getFiles()) {
              SFile f = get_file_by_id(fid);
              if (f != null && f.getLocationsSize() > 0) {
                for (SFileLocation sfl : f.getLocations()) {
                  if (sfl.getNode_name().equals("")) {
                    // this is the NAS location, record it
                    LOG.info("p -> SFL: DEV " + sfl.getDevid() + ", LOC " + sfl.getLocation());
                    sfl.setDevid(to_nas_devid);
                    sfl.setDigest("MIGRATE2-DIGESTED!");
                    targetFileMap.put(fid, sfl);
                  }
                }
              }
            }
          }
        }
      }

      // call remote metastore's migrate2_in to construct metadata
      if (rcli.migrate2_in(tbl, parts, idxs, ldc.getName(), to_nas_devid, targetFileMap)) {
        // wow, it is success, change tbl's dbname to dbName
        tbl.setDbName(dbName);
        // set tbl properties
        kvs = new HashMap<String, String>();
        if (tbl.getParametersSize() > 0) {
          LOG.info(tbl.getParameters().toString());
          kvs.putAll(tbl.getParameters());
          kvs.put("store.remote", "both");
          if (isMaster) {
            kvs.put("store.identify", "master");
          } else {
            kvs.put("store.identify", "slave");
          }
          if (kvs.get("store.remote.dcs") == null) {
            kvs.put("store.remote.dcs", to_dc + "." + rdb +
                "," + hiveConf.getVar(ConfVars.LOCAL_DATACENTER) + "." + dbName);
          } else {
            String olddcs = kvs.get("store.remote.dcs");
            String[] dcsarray = olddcs.split(",");
            boolean ign = false;
            for (int i = 0; i < dcsarray.length; i++) {
              if (dcsarray[i].equals(to_dc + "." + rdb)) {
                ign = true;
                break;
              }
            }
            if (!ign) {
              olddcs += "," + to_dc + "." + rdb;
            }
            kvs.put("store.remote.dcs", olddcs);
          }
          tbl.setParameters(kvs);
        } else {
          tbl.setParameters(kvs);
        }
        alter_table(dbName, tableName, tbl);
        LOG.info("Update table properties to reflect the migration.");

        HashMap<String,Object> old_params= new HashMap<String,Object>();
        List<String> tmp = new ArrayList<String>();
        tmp.add("store.remote");
        tmp.add("store.remote.dcs");

        old_params.put("tbl_param_keys", tmp);
        old_params.put("db_name", tbl.getDbName());
        old_params.put("table_name", tbl.getTableName());
        MetaMsgServer.sendMsg(MSGFactory.generateDDLMsg(MSGType.MSG_ALT_TABLE_PARAM,-1l,-1l, null,-1l,old_params));
      } else {
        LOG.info("Migrate2 through NAS-WAN-NAS failed at remote Datacenter " + to_dc);
        return false;
      }
      rcli.close();

      // finally, remove local partition_file relationship
      for (Partition p : parts) {
        if (p.getSubpartitionsSize() > 0) {
          for (Subpartition sp : p.getSubpartitions()) {
            List<SFile> files = new ArrayList<SFile>();

            if (sp.getFilesSize() > 0) {
              for (long fid : sp.getFiles()) {
                SFile f = get_file_by_id(fid);
                if (f != null) {
                  files.add(f);
                }
              }
            }
            LOG.debug("Begin drop subpartition files.");
            drop_subpartition_files(sp, files);
            LOG.debug("Begin delete files.");
            for (SFile f : files) {
              rm_file_physical(f);
            }
          }
        } else {
          List<SFile> files = new ArrayList<SFile>();

          if (p.getFilesSize() > 0) {
            for (long fid : p.getFiles()) {
              SFile f = get_file_by_id(fid);
              if (f != null) {
                files.add(f);
              }
            }
          }
          LOG.debug("Begin drop partition files.");
          drop_partition_files(p, files);
          LOG.debug("Begin delete files.");
          for (SFile f : files) {
            rm_file_physical(f);
          }
        }
      }

      LOG.info("Finally, our migration succeed, files in this DC is deleted.");

      return true;
    }

    @Override
    public boolean migrate2_in(Table tbl, List<Partition> parts, List<Index> idxs, String from_dc,
        String to_nas_devid, Map<Long, SFileLocation> fileMap) throws MetaException, TException {
      LOG.info("server parts2 " + parts.get(0).getSubpartitions().get(0).getPartitionName() + ", " + parts.get(0).getSubpartitions().get(0).getFiles());

      // try to create the database, if it doesn't exist
      Datacenter ldc = null;

      if (hiveConf.getVar(HiveConf.ConfVars.LOCAL_DATACENTER) == null) {
        throw new MetaException("Please set 'hive.datacenter.local' as the local DC name");
      }
      try {
        ldc = getMS().getDatacenter(hiveConf.getVar(HiveConf.ConfVars.LOCAL_DATACENTER));
      } catch (NoSuchObjectException e) {
        ldc = new Datacenter(hiveConf.getVar(HiveConf.ConfVars.LOCAL_DATACENTER), null, HMSHandler.msUri == null ? "DEFAULT_URI" : HMSHandler.msUri, null);
        getMS().createDatacenter(ldc);
      }

      try {
        getMS().getDatabase(tbl.getDbName());
      } catch (NoSuchObjectException e) {
        Database db = new Database(tbl.getDbName(), DEFAULT_DATABASE_COMMENT,
                wh.getDefaultDatabasePath(tbl.getDbName()).toString(), null);
        db.setDatacenter(ldc);
        getMS().createDatabase(db);
        LOG.info("Create database " + tbl.getDbName() + " done.");
      }
      // try to create the table, if it doesn't exist
      try {
        create_table(tbl);
      } catch (AlreadyExistsException e) {
        // it is ok, ignore it? alter_table?
        alter_table(tbl.getDbName(), tbl.getTableName(), tbl);
      }
      LOG.info("Create table " + tbl.getTableName() + " done.");

      // try to create the idxs, if they don't exist
      if (idxs != null) {
        for (Index i : idxs) {
          try {
            add_index(i, null);
          } catch (AlreadyExistsException e) {
            // it is ok, ignore it
          }
        }
      }
      LOG.info("Create indexs: " + idxs.size() + " done.");

      // try to create the partition, if it doesn't exist
      List<Partition> toAdd = new ArrayList<Partition>();
      List<Partition> toUpdate = new ArrayList<Partition>();
      List<Partition> copy_parts = deepCopyPartitions(parts);
      for (Partition part : copy_parts) {
        // reset dbName for partition
        part.setDbName(tbl.getDbName());

        LOG.info("Handle partition1 " + part.getPartitionName() + ", subparts NR " + part.getSubpartitionsSize());
        try {
          getMS().getPartition(part.getDbName(), part
              .getTableName(), part.getPartitionName());
        } catch (NoSuchObjectException e) {
          // this means there is no existing partition, it is ok
          toAdd.add(part);
          continue;
        }
        toUpdate.add(part);
      }
      add_partitions(toAdd);
      LOG.info("Add partitions done. toUpdate size is " + toUpdate.size());

      // try to create the file without any active locations.
      Set<SFile> fileToDel = new TreeSet<SFile>();
      Set<SFileLocation> sflToDel = new TreeSet<SFileLocation>();
      Map<Long, SFile> oldFidToNewFile = new HashMap<Long, SFile>();

      for (Map.Entry<Long, SFileLocation> entry : fileMap.entrySet()) {
        SFileLocation sfl = entry.getValue();

        LOG.info("Add NEW SFL DEV " + sfl.getDevid() + ", LOC " + sfl.getLocation());
        SFile nfile = create_file_wo_location(3, tbl.getDbName(), tbl.getTableName());
        fileToDel.add(nfile);

        sfl.setNode_name(dm.getAnyNode());
        while (sfl.getNode_name() == null) {
          LOG.warn("No active node in ndmap ... retry it.");
          sfl.setNode_name(dm.getAnyNode());
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            LOG.error(e, e);
          }
        }
        sfl.setFid(nfile.getFid());
        sfl.setRep_id(0);
        sfl.setUpdate_time(System.currentTimeMillis());
        sfl.setVisit_status(MetaStoreConst.MFileLocationVisitStatus.ONLINE);

        if (!getMS().createFileLocation(sfl)) {
          LOG.info("[ROLLBACK] Failed to create SFL " + sfl.getDevid() + ", LOC " + sfl.getLocation());
          // rollback to delete all files and locations
          for (SFileLocation fl : sflToDel) {
            getMS().delSFileLocation(fl.getDevid(), fl.getLocation());
          }
          for (SFile f : fileToDel) {
            getMS().delSFile(f.getFid());
          }
          return false;
        } else {
          sflToDel.add(sfl);
          List<SFileLocation> locations = new ArrayList<SFileLocation>();
          locations.add(sfl);
          nfile.setLocations(locations);
          oldFidToNewFile.put(entry.getKey(), nfile);
        }

      }

      // finally, add SFile to part or subpart
      for (Partition part : parts) {
        LOG.info("Handle partition2 " + part.getPartitionName() + ", subparts NR " + part.getSubpartitionsSize());
        part.setDbName(tbl.getDbName());

        if (part.getSubpartitionsSize() > 0) {
          // call add subpartition_files()
          for (Subpartition subpart : part.getSubpartitions()) {
            subpart.setDbName(tbl.getDbName());
            LOG.info("subparts " + subpart.getPartitionName() + ", " + subpart.getFiles());
            List<SFile> files = new ArrayList<SFile>();
            for (Long fid : subpart.getFiles()) {
              SFile lookup = oldFidToNewFile.get(fid);
              if (lookup == null) {
                LOG.info("Lookup old FID " + fid + " failed in oldFidToNewFile map, ignore this file [data loss].");
              } else {
                files.add(lookup);
              }
            }
            Subpartition localsubpart = getMS().getSubpartition(subpart.getDbName(), subpart.getTableName(), subpart.getPartitionName());

            if (localsubpart == null) {
              throw new MetaException("Invalid local subpart: " + subpart.getPartitionName());
            }

            add_subpartition_files(localsubpart, files);
          }
        } else {
          // call add_partition_files()
          if (part.getFilesSize() > 0) {
            List<SFile> files = new ArrayList<SFile>();
            for (Long fid : part.getFiles()) {
              SFile lookup = oldFidToNewFile.get(fid);
              if (lookup == null) {
                LOG.info("Lookup old FID " + fid + " failed in oldFidToNewFile map, ignore this file [data loss].");
              } else {
                files.add(lookup);
              }
            }
            Partition localpart = getMS().getPartition(part.getDbName(), part.getTableName(), part.getPartitionName());

            if (localpart == null) {
              throw new MetaException("Invalid local part: " + part.getPartitionName());
            }
            LOG.info("parts " + part.getPartitionName() + ", " + part.getFiles().toString());

            add_partition_files(localpart, files);
          }
        }
      }

      // close the files
      for (Map.Entry<Long, SFile> entry : oldFidToNewFile.entrySet()) {
        close_file(entry.getValue());
      }

      LOG.info("OK, we will migrate DC " + from_dc + " DB " + tbl.getDbName() + " Table " +
          tbl.getTableName() + "'s " + parts.size() + " Partition(s) w/ " + fileMap.size() +
          " files to local DC.");

      return true;
    }

    @Override
    public List<Busitype> showBusitypes() throws InvalidObjectException, MetaException, TException {
      return getMS().showBusitypes();
    }

    @Override
    public int createBusitype(Busitype busitype) throws InvalidObjectException, MetaException,
        TException {
      return getMS().createBusitype(busitype);
    }

    @Override
    public boolean online_filelocation(SFile file) throws MetaException, TException {
      // reget the file now
      SFile stored_file = get_file_by_id(file.getFid());

      if (stored_file.getStore_status() != MetaStoreConst.MFileStoreStatus.INCREATE) {
        throw new MetaException("online filelocation can only do on INCREATE file.");
      }
      if (stored_file.getLocationsSize() != 1) {
        throw new MetaException("Invalid file location in SFile fid: " + stored_file.getFid());
      }
      SFileLocation sfl = stored_file.getLocations().get(0);
      assert sfl != null;
      sfl.setVisit_status(MetaStoreConst.MFileLocationVisitStatus.ONLINE);
      getMS().updateSFileLocation(sfl);

      return true;
    }

    @Override
    public boolean toggle_safemode() throws MetaException, TException {
      if (dm != null) {
        synchronized (dm) {
          dm.safeMode = !dm.safeMode;
        }
      }
      return true;
    }

  }

  public static IHMSHandler newHMSHandler(String name, HiveConf hiveConf) throws MetaException {
    return RetryingHMSHandler.getProxy(hiveConf, name);
  }



  /**
   * Discard a current delegation token.
   *
   * @param tokenStrForm
   *          the token in string form
   */
  public static void cancelDelegationToken(String tokenStrForm
      ) throws IOException {
    saslServer.cancelDelegationToken(tokenStrForm);
  }

  /**
   * Get a new delegation token.
   *
   * @param renewer
   *          the designated renewer
   */
  public static String getDelegationToken(String owner, String renewer)
      throws IOException, InterruptedException {
    return saslServer.getDelegationToken(owner, renewer);
  }

  /**
   * Renew a delegation token to extend its lifetime.
   *
   * @param tokenStrForm
   *          the token in string form
   */
  public static long renewDelegationToken(String tokenStrForm
      ) throws IOException {
    return saslServer.renewDelegationToken(tokenStrForm);
  }

  /**
   * HiveMetaStore specific CLI
   *
   */
  static public class HiveMetastoreCli extends CommonCliOptions {
    int port = DEFAULT_HIVE_METASTORE_PORT;

    @SuppressWarnings("static-access")
    public HiveMetastoreCli() {
      super("hivemetastore", true);

      // -p port
      OPTIONS.addOption(OptionBuilder
          .hasArg()
          .withArgName("port")
          .withDescription("Hive Metastore port number, default:"
              + DEFAULT_HIVE_METASTORE_PORT)
          .create('p'));

    }

    @Override
    public void parse(String[] args) {
      super.parse(args);

      // support the old syntax "hivemetastore [port]" but complain
      args = commandLine.getArgs();
      if (args.length > 0) {
        // complain about the deprecated syntax -- but still run
        System.err.println(
            "This usage has been deprecated, consider using the new command "
                + "line syntax (run with -h to see usage information)");

        port = new Integer(args[0]);
      }

      // notice that command line options take precedence over the
      // deprecated (old style) naked args...
      if (commandLine.hasOption('p')) {
        port = Integer.parseInt(commandLine.getOptionValue('p'));
      } else {
        // legacy handling
        String metastorePort = System.getenv("METASTORE_PORT");
        if (metastorePort != null) {
          port = Integer.parseInt(metastorePort);
        }
      }
    }
  }

  /**
   * @param args
   */
  public static void main(String[] args) throws Throwable {
    HiveMetastoreCli cli = new HiveMetastoreCli();
    cli.parse(args);
    final boolean isCliVerbose = cli.isVerbose();
    // NOTE: It is critical to do this prior to initializing log4j, otherwise
    // any log specific settings via hiveconf will be ignored
    Properties hiveconf = cli.addHiveconfToSystemProperties();

    // If the log4j.configuration property hasn't already been explicitly set, use Hive's default
    // log4j configuration
    if (System.getProperty("log4j.configuration") == null) {
      // NOTE: It is critical to do this here so that log4j is reinitialized
      // before any of the other core hive classes are loaded
      try {
        LogUtils.initHiveLog4j();
      } catch (LogInitializationException e) {
        HMSHandler.LOG.warn(e.getMessage());
      }
    }

    try {
      String msg = "Starting hive metastore on port " + cli.port;
      HMSHandler.LOG.info(msg);
      if (cli.isVerbose()) {
        System.err.println(msg);
      }

      HiveConf conf = new HiveConf(HMSHandler.class);

      // set all properties specified on the command line
      for (Map.Entry<Object, Object> item : hiveconf.entrySet()) {
        conf.set((String) item.getKey(), (String) item.getValue());
      }

      // Add shutdown hook.
      Runtime.getRuntime().addShutdownHook(new Thread() {
        @Override
        public void run() {
          String shutdownMsg = "Shutting down hive metastore.";
          HMSHandler.LOG.info(shutdownMsg);
          if (isCliVerbose) {
            System.err.println(shutdownMsg);
          }
        }
      });

      dm = new DiskManager(new HiveConf(DiskManager.class), HMSHandler.LOG);
      startMetaStore(cli.port, ShimLoader.getHadoopThriftAuthBridge(), conf);
    } catch (Throwable t) {
      // Catch the exception, log it and rethrow it.
      HMSHandler.LOG
          .error("Metastore Thrift Server threw an exception...", t);
      throw t;
    }
  }

  static void connect_to_top_dc(HiveConf conf) throws MetaException {
    boolean is_top_dc = conf.getBoolVar(ConfVars.IS_TOP_DATACENTER);
    if (!is_top_dc) {
      LOG.info("Begin connecting to Top-level Datacenter Metastore ...");
      String top_dc_uri = conf.getVar(ConfVars.TOP_DATACENTER);
      if (top_dc_uri == null) {
        throw new MetaException("Please set 'hive.datacenter.top' as top-level metastore URI." );
      }
      try {
      HMSHandler.topdcli = new HiveMetaStoreClient(top_dc_uri,
          HiveConf.getIntVar(conf, HiveConf.ConfVars.METASTORETHRIFTCONNECTIONRETRIES),
          conf.getIntVar(ConfVars.METASTORE_CLIENT_CONNECT_RETRY_DELAY),
          null);
      } catch (MetaException me) {
        LOG.info("Connect to top-level Datacenter failed!");
      }
    }
  }

  /**
   * Start Metastore based on a passed {@link HadoopThriftAuthBridge}
   *
   * @param port
   * @param bridge
   * @throws Throwable
   */
  public static void startMetaStore(int port, HadoopThriftAuthBridge bridge)
      throws Throwable {
    startMetaStore(port, bridge, new HiveConf(HMSHandler.class));
  }

  /**
   * Start Metastore based on a passed {@link HadoopThriftAuthBridge}
   *
   * @param port
   * @param bridge
   * @param conf
   *          configuration overrides
   * @throws Throwable
   */
  public static void startMetaStore(int port, HadoopThriftAuthBridge bridge,
      HiveConf conf) throws Throwable {
    try {
      // init connection to top-level datacenter if it is not top-level dc
      connect_to_top_dc(conf);

      // generate this msuri
      HMSHandler.msUri = "thrift://" + InetAddress.getLocalHost().getHostName() + ":" + port;

      // Server will create new threads up to max as necessary. After an idle
      // period, it will destory threads to keep the number of threads in the
      // pool to min.
      int minWorkerThreads = conf.getIntVar(HiveConf.ConfVars.METASTORESERVERMINTHREADS);
      int maxWorkerThreads = conf.getIntVar(HiveConf.ConfVars.METASTORESERVERMAXTHREADS);
      boolean tcpKeepAlive = conf.getBoolVar(HiveConf.ConfVars.METASTORE_TCP_KEEP_ALIVE);
      boolean useFramedTransport = conf.getBoolVar(ConfVars.METASTORE_USE_THRIFT_FRAMED_TRANSPORT);
      useSasl = conf.getBoolVar(HiveConf.ConfVars.METASTORE_USE_THRIFT_SASL);

      TServerTransport serverTransport = tcpKeepAlive ?
          new TServerSocketKeepAlive(port) : new TServerSocket(port);

      TProcessor processor;
      TTransportFactory transFactory;
      if (useSasl) {
        // we are in secure mode.
        if (useFramedTransport) {
          throw new HiveMetaException("Framed transport is not supported with SASL enabled.");
        }
        saslServer = bridge.createServer(
            conf.getVar(HiveConf.ConfVars.METASTORE_KERBEROS_KEYTAB_FILE),
            conf.getVar(HiveConf.ConfVars.METASTORE_KERBEROS_PRINCIPAL));
        // start delegation token manager
        saslServer.startDelegationTokenSecretManager(conf);
        transFactory = saslServer.createTransportFactory();
        processor = saslServer.wrapProcessor(new ThriftHiveMetastore.Processor<IHMSHandler>(
            newHMSHandler("new db based metaserver", conf)));
        LOG.info("Starting DB backed MetaStore Server in Secure Mode");
      } else {
        // we are in unsecure mode.
        IHMSHandler handler = newHMSHandler("new db based metaserver", conf);

        if (conf.getBoolVar(ConfVars.METASTORE_EXECUTE_SET_UGI)) {
          transFactory = useFramedTransport ?
              new ChainedTTransportFactory(new TFramedTransport.Factory(),
                  new TUGIContainingTransport.Factory())
              : new TUGIContainingTransport.Factory();

          processor = new TUGIBasedProcessor<IHMSHandler>(handler);
          LOG.info("Starting DB backed MetaStore Server with SetUGI enabled");
        } else {
          transFactory = useFramedTransport ?
              new TFramedTransport.Factory() : new TTransportFactory();
          processor = new TSetIpAddressProcessor<IHMSHandler>(handler);
          LOG.info("Starting DB backed MetaStore Server");
        }
      }

      TThreadPoolServer.Args args = new TThreadPoolServer.Args(serverTransport)
          .processor(processor)
          .transportFactory(transFactory)
          .protocolFactory(new TBinaryProtocol.Factory())
          .minWorkerThreads(minWorkerThreads)
          .maxWorkerThreads(maxWorkerThreads);

      TServer tServer = new TThreadPoolServer(args);
      HMSHandler.LOG.info("Started the new metaserver on port [" + port
          + "]...");
      HMSHandler.LOG.info("Options.minWorkerThreads = "
          + minWorkerThreads);
      HMSHandler.LOG.info("Options.maxWorkerThreads = "
          + maxWorkerThreads);
      HMSHandler.LOG.info("TCP keepalive = " + tcpKeepAlive);
      tServer.serve();
    } catch (Throwable x) {
      x.printStackTrace();
      HMSHandler.LOG.error(StringUtils.stringifyException(x));
      throw x;
    }
  }
}
