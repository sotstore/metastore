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

package org.apache.hadoop.hive.ql.security.authorization;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStore.HMSHandler.MSSessionState;
import org.apache.hadoop.hive.metastore.MetaStorePreEventListener;
import org.apache.hadoop.hive.metastore.MetaStoreUtils;
import org.apache.hadoop.hive.metastore.TableType;
import org.apache.hadoop.hive.metastore.api.Database;
import org.apache.hadoop.hive.metastore.api.InvalidOperationException;
import org.apache.hadoop.hive.metastore.api.MSOperation;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.hive.metastore.api.NoSuchObjectException;
import org.apache.hadoop.hive.metastore.events.PreAddPartitionEvent;
import org.apache.hadoop.hive.metastore.events.PreAlterPartitionEvent;
import org.apache.hadoop.hive.metastore.events.PreAlterTableEvent;
import org.apache.hadoop.hive.metastore.events.PreCreateDatabaseEvent;
import org.apache.hadoop.hive.metastore.events.PreCreateTableEvent;
import org.apache.hadoop.hive.metastore.events.PreDropDatabaseEvent;
import org.apache.hadoop.hive.metastore.events.PreDropPartitionEvent;
import org.apache.hadoop.hive.metastore.events.PreDropTableEvent;
import org.apache.hadoop.hive.metastore.events.PreEventContext;
import org.apache.hadoop.hive.metastore.events.PreUserAuthorityCheckEvent;
import org.apache.hadoop.hive.ql.metadata.AuthorizationException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.metadata.HiveUtils;
import org.apache.hadoop.hive.ql.metadata.Partition;
import org.apache.hadoop.hive.ql.metadata.Table;
import org.apache.hadoop.hive.ql.plan.HiveOperation;
import org.apache.hadoop.hive.ql.security.HiveMetastoreAuthenticationProvider;

/**
 * AuthorizationPreEventListener : A MetaStorePreEventListener that
 * performs authorization/authentication checks on the metastore-side.
 *
 * Note that this can only perform authorization checks on defined
 * metastore PreEventContexts, such as the adding/dropping and altering
 * of databases, tables and partitions.
 */
public class AuthorizationPreEventListener extends MetaStorePreEventListener {

  public static final Log LOG = LogFactory.getLog(
      AuthorizationPreEventListener.class);

  private static HiveConf conf;
  private static HiveMetastoreAuthorizationProvider authorizer;
  private static HiveMetastoreAuthenticationProvider authenticator;
  private static Map<MSOperation, HiveOperation> ms2hive = new HashMap<MSOperation, HiveOperation>();

  // FIXME: we can't put our authorizer to HiveMetaStore.java, thus, we put it here!
  private static void init_ms_to_hive_map() {
    ms2hive.clear();
    ms2hive.put(MSOperation.ALTERDATABASE, HiveOperation.ALTERDATABASE);
    ms2hive.put(MSOperation.ALTERINDEX_PROPS, HiveOperation.ALTERINDEX_PROPS);
    ms2hive.put(MSOperation.ALTERINDEX_REBUILD, HiveOperation.ALTERINDEX_REBUILD);
    ms2hive.put(MSOperation.ALTERTABLE_ADDCOLS, HiveOperation.ALTERTABLE_ADDCOLS);
    ms2hive.put(MSOperation.ALTERTABLE_ADDPARTS, HiveOperation.ALTERTABLE_ADDPARTS);
    ms2hive.put(MSOperation.ALTERTABLE_DROPPARTS, HiveOperation.ALTERTABLE_DROPPARTS);
    ms2hive.put(MSOperation.ALTERTABLE_PROPERTIES, HiveOperation.ALTERTABLE_PROPERTIES);
    ms2hive.put(MSOperation.ALTERTABLE_RENAME, HiveOperation.ALTERTABLE_RENAME);
    ms2hive.put(MSOperation.ALTERTABLE_RENAMECOL, HiveOperation.ALTERTABLE_RENAMECOL);
    ms2hive.put(MSOperation.ALTERTABLE_RENAMEPART, HiveOperation.ALTERTABLE_RENAMEPART);
    ms2hive.put(MSOperation.ALTERTABLE_REPLACECOLS, HiveOperation.ALTERTABLE_REPLACECOLS);
    ms2hive.put(MSOperation.ALTERVIEW_PROPERTIES, HiveOperation.ALTERVIEW_PROPERTIES);
    ms2hive.put(MSOperation.AUTHENTICATION, HiveOperation.AUTHENTICATION);
    ms2hive.put(MSOperation.CHANGE_PWD, HiveOperation.CHANGE_PWD);
    ms2hive.put(MSOperation.CREATEDATABASE, HiveOperation.CREATEDATABASE);
    ms2hive.put(MSOperation.CREATEINDEX, HiveOperation.CREATEINDEX);
    ms2hive.put(MSOperation.CREATEROLE, HiveOperation.CREATEROLE);
    ms2hive.put(MSOperation.CREATETABLE, HiveOperation.CREATETABLE);
    ms2hive.put(MSOperation.CREATEUSER, HiveOperation.CREATEUSER);
    ms2hive.put(MSOperation.CREATEVIEW, HiveOperation.CREATEVIEW);
    ms2hive.put(MSOperation.DESCDATABASE, HiveOperation.DESCDATABASE);
    ms2hive.put(MSOperation.DESCTABLE, HiveOperation.DESCTABLE);
    ms2hive.put(MSOperation.DROPDATABASE, HiveOperation.DROPDATABASE);
    ms2hive.put(MSOperation.DROPINDEX, HiveOperation.DROPINDEX);
    ms2hive.put(MSOperation.DROPROLE, HiveOperation.DROPROLE);
    ms2hive.put(MSOperation.DROPTABLE, HiveOperation.DROPTABLE);
    ms2hive.put(MSOperation.DROPUSER, HiveOperation.DROPUSER);
    ms2hive.put(MSOperation.DROPVIEW, HiveOperation.DROPVIEW);
    ms2hive.put(MSOperation.EXPLAIN, HiveOperation.EXPLAIN);
    ms2hive.put(MSOperation.GRANT_PRIVILEGE, HiveOperation.GRANT_PRIVILEGE);
    ms2hive.put(MSOperation.GRANT_ROLE, HiveOperation.GRANT_ROLE);
    ms2hive.put(MSOperation.QUERY, HiveOperation.QUERY);
    ms2hive.put(MSOperation.REVOKE_PRIVILEGE, HiveOperation.REVOKE_PRIVILEGE);
    ms2hive.put(MSOperation.REVOKE_ROLE, HiveOperation.REVOKE_ROLE);
    ms2hive.put(MSOperation.SHOW_CREATETABLE, HiveOperation.SHOW_CREATETABLE);
    ms2hive.put(MSOperation.SHOW_GRANT, HiveOperation.SHOW_GRANT);
    ms2hive.put(MSOperation.SHOW_ROLE_GRANT, HiveOperation.SHOW_ROLE_GRANT);
    ms2hive.put(MSOperation.SHOW_TABLESTATUS, HiveOperation.SHOW_TABLESTATUS);
    ms2hive.put(MSOperation.SHOW_TBLPROPERTIES, HiveOperation.SHOW_TBLPROPERTIES);
    ms2hive.put(MSOperation.SHOW_USERNAMES, HiveOperation.SHOW_USERNAMES);
    ms2hive.put(MSOperation.SHOWCOLUMNS, HiveOperation.SHOWCOLUMNS);
    ms2hive.put(MSOperation.SHOWDATABASES, HiveOperation.SHOWDATABASES);
    ms2hive.put(MSOperation.SHOWINDEXES, HiveOperation.SHOWINDEXES);
    ms2hive.put(MSOperation.SHOWPARTITIONS, HiveOperation.SHOWPARTITIONS);
    ms2hive.put(MSOperation.SHOWTABLES, HiveOperation.SHOWTABLES);
  }

  public AuthorizationPreEventListener(Configuration config) throws HiveException {
    super(config);

    authenticator = (HiveMetastoreAuthenticationProvider) HiveUtils.getAuthenticator(
        config, HiveConf.ConfVars.HIVE_METASTORE_AUTHENTICATOR_MANAGER);
    authorizer = (HiveMetastoreAuthorizationProvider) HiveUtils.getAuthorizeProviderManager(
        config, HiveConf.ConfVars.HIVE_METASTORE_AUTHORIZATION_MANAGER, authenticator);
    init_ms_to_hive_map();
  }

  @Override
  public void onEvent(PreEventContext context) throws MetaException, NoSuchObjectException,
      InvalidOperationException {

    authenticator.setMetaStoreHandler(context.getHandler());
    authorizer.setMetaStoreHandler(context.getHandler());

    switch (context.getEventType()) {
    case CREATE_TABLE:
      authorizeCreateTable((PreCreateTableEvent)context);
      break;
    case DROP_TABLE:
      authorizeDropTable((PreDropTableEvent)context);
      break;
    case ALTER_TABLE:
      authorizeAlterTable((PreAlterTableEvent)context);
      break;
    case ADD_PARTITION:
      authorizeAddPartition((PreAddPartitionEvent)context);
      break;
    case DROP_PARTITION:
      authorizeDropPartition((PreDropPartitionEvent)context);
      break;
    case ALTER_PARTITION:
      authorizeAlterPartition((PreAlterPartitionEvent)context);
      break;
    case CREATE_DATABASE:
      authorizeCreateDatabase((PreCreateDatabaseEvent)context);
      break;
    case DROP_DATABASE:
      authorizeDropDatabase((PreDropDatabaseEvent)context);
      break;
    case LOAD_PARTITION_DONE:
      // noop for now
      break;
    case USER_AUTHORITY_CHECK:
      authorizeUserAuthorityCheck((PreUserAuthorityCheckEvent)context);
      break;
    default:
      break;
    }
  }

  private void authorizeUserAuthorityCheck(PreUserAuthorityCheckEvent uac) throws InvalidOperationException, MetaException {
    MSSessionState msss = new MSSessionState();
    LOG.debug("Begin check authority for table" + uac.getTable().getTableName() + " on user " + msss.getUserName());
    try {
      authorizer.authorize(new Table(uac.getTable()), ms2hive.get(uac.getMso()).getInputRequiredPrivileges(),
          ms2hive.get(uac.getMso()).getOutputRequiredPrivileges());
    } catch (AuthorizationException e) {
      throw invalidOperationException(e);
    } catch (HiveException e) {
      throw metaException(e);
    }
  }

  private void authorizeCreateDatabase(PreCreateDatabaseEvent context)
      throws InvalidOperationException, MetaException {
    try {
      authorizer.authorize(new Database(context.getDatabase()),
          HiveOperation.CREATEDATABASE.getInputRequiredPrivileges(),
          HiveOperation.CREATEDATABASE.getOutputRequiredPrivileges());
    } catch (AuthorizationException e) {
      throw invalidOperationException(e);
    } catch (HiveException e) {
      throw metaException(e);
    }
  }

  private void authorizeDropDatabase(PreDropDatabaseEvent context)
      throws InvalidOperationException, MetaException {
    try {
      authorizer.authorize(new Database(context.getDatabase()),
          HiveOperation.DROPDATABASE.getInputRequiredPrivileges(),
          HiveOperation.DROPDATABASE.getOutputRequiredPrivileges());
    } catch (AuthorizationException e) {
      throw invalidOperationException(e);
    } catch (HiveException e) {
      throw metaException(e);
    }
  }

  private void authorizeCreateTable(PreCreateTableEvent context)
      throws InvalidOperationException, MetaException {
    try {
      authorizer.authorize(getTableFromApiTable(context.getTable()),
          HiveOperation.CREATETABLE.getInputRequiredPrivileges(),
          HiveOperation.CREATETABLE.getOutputRequiredPrivileges());
    } catch (AuthorizationException e) {
      throw invalidOperationException(e);
    } catch (HiveException e) {
      throw metaException(e);
    }
  }

  private void authorizeDropTable(PreDropTableEvent context)
      throws InvalidOperationException, MetaException {
    try {
      authorizer.authorize(getTableFromApiTable(context.getTable()),
          HiveOperation.DROPTABLE.getInputRequiredPrivileges(),
          HiveOperation.DROPTABLE.getOutputRequiredPrivileges());
    } catch (AuthorizationException e) {
      throw invalidOperationException(e);
    } catch (HiveException e) {
      throw metaException(e);
    }
  }

  private void authorizeAlterTable(PreAlterTableEvent context)
      throws InvalidOperationException, MetaException {
    try {
      authorizer.authorize(getTableFromApiTable(context.getOldTable()),
          null,
          new Privilege[]{Privilege.ALTER_METADATA});
    } catch (AuthorizationException e) {
      throw invalidOperationException(e);
    } catch (HiveException e) {
      throw metaException(e);
    }
  }

  private void authorizeAddPartition(PreAddPartitionEvent context)
      throws InvalidOperationException, MetaException {
    try {
      org.apache.hadoop.hive.metastore.api.Partition mapiPart = context.getPartition();
      authorizer.authorize(getPartitionFromApiPartition(mapiPart, context),
          HiveOperation.ALTERTABLE_ADDPARTS.getInputRequiredPrivileges(),
          HiveOperation.ALTERTABLE_ADDPARTS.getOutputRequiredPrivileges());
    } catch (AuthorizationException e) {
      throw invalidOperationException(e);
    } catch (NoSuchObjectException e) {
      throw invalidOperationException(e);
    } catch (HiveException e) {
      throw metaException(e);
    }
  }

  private void authorizeDropPartition(PreDropPartitionEvent context)
      throws InvalidOperationException, MetaException {
    try {
      org.apache.hadoop.hive.metastore.api.Partition mapiPart = context.getPartition();
      authorizer.authorize(getPartitionFromApiPartition(mapiPart, context),
          HiveOperation.ALTERTABLE_DROPPARTS.getInputRequiredPrivileges(),
          HiveOperation.ALTERTABLE_DROPPARTS.getOutputRequiredPrivileges());
    } catch (AuthorizationException e) {
      throw invalidOperationException(e);
    } catch (NoSuchObjectException e) {
      throw invalidOperationException(e);
    } catch (HiveException e) {
      throw metaException(e);
    }
  }

  private void authorizeAlterPartition(PreAlterPartitionEvent context)
      throws InvalidOperationException, MetaException {
    try {
      org.apache.hadoop.hive.metastore.api.Partition mapiPart = context.getNewPartition();
      authorizer.authorize(getPartitionFromApiPartition(mapiPart, context),
          null,
          new Privilege[]{Privilege.ALTER_METADATA});
    } catch (AuthorizationException e) {
      throw invalidOperationException(e);
    } catch (NoSuchObjectException e) {
      throw invalidOperationException(e);
    } catch (HiveException e) {
      throw metaException(e);
    }
  }

  private Table getTableFromApiTable(org.apache.hadoop.hive.metastore.api.Table apiTable) {
    org.apache.hadoop.hive.metastore.api.Table tTable = apiTable.deepCopy();
    if (tTable.getTableType() == null){
      // TableType specified was null, we need to figure out what type it was.
      if (MetaStoreUtils.isExternalTable(tTable)){
        tTable.setTableType(TableType.EXTERNAL_TABLE.toString());
      } else if (MetaStoreUtils.isIndexTable(tTable)) {
        tTable.setTableType(TableType.INDEX_TABLE.toString());
      } else if ((tTable.getSd() == null) || (tTable.getSd().getLocation() == null)) {
        tTable.setTableType(TableType.VIRTUAL_VIEW.toString());
      } else {
        tTable.setTableType(TableType.MANAGED_TABLE.toString());
      }
    }
    Table tbl = new Table(tTable);
    return tbl;
  }

  private Partition getPartitionFromApiPartition(
      org.apache.hadoop.hive.metastore.api.Partition mapiPart,
      PreEventContext context) throws HiveException, NoSuchObjectException, MetaException {
    org.apache.hadoop.hive.metastore.api.Partition tPart = mapiPart.deepCopy();
    org.apache.hadoop.hive.metastore.api.Table t = context.getHandler().get_table(
        mapiPart.getDbName(), mapiPart.getTableName());
    if (tPart.getSd() == null){
      // In the cases of create partition, by the time this event fires, the partition
      // object has not yet come into existence, and thus will not yet have a
      // location or an SD, but these are needed to create a ql.metadata.Partition,
      // so we use the table's SD. The only place this is used is by the
      // authorization hooks, so we will not affect code flow in the metastore itself.
      tPart.setSd(t.getSd());
    }
    return new Partition(getTableFromApiTable(t),tPart);
  }

  private InvalidOperationException invalidOperationException(Exception e) {
    InvalidOperationException ex = new InvalidOperationException(e.getMessage());
    ex.initCause(e.getCause());
    return ex;
  }

  private MetaException metaException(HiveException e) {
    MetaException ex =  new MetaException(e.getMessage());
    ex.initCause(e);
    return ex;
  }

}
