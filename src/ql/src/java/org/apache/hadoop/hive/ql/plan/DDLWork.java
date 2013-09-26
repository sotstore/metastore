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
package org.apache.hadoop.hive.ql.plan;

import java.io.Serializable;
import java.util.HashSet;

import org.apache.hadoop.hive.ql.hooks.ReadEntity;
import org.apache.hadoop.hive.ql.hooks.WriteEntity;
import org.apache.hadoop.hive.ql.parse.AlterTablePartMergeFilesDesc;
import org.mortbay.log.Log;

/**
 * DDLWork.
 *
 */
public class DDLWork implements Serializable {
  private static final long serialVersionUID = 1L;
  private CreateIndexDesc createIndexDesc;
  private AlterIndexDesc alterIndexDesc;
  private DropIndexDesc dropIdxDesc;
  private CreateDatabaseDesc createDatabaseDesc;
  private SwitchDatabaseDesc switchDatabaseDesc;
  private DropDatabaseDesc dropDatabaseDesc;
  private CreateTableDesc createTblDesc;
  private CreateTableLikeDesc createTblLikeDesc;
  private CreateViewDesc createVwDesc;
  private DropTableDesc dropTblDesc;
  private AlterTableDesc alterTblDesc;
  private AlterIndexDesc alterIdxDesc;
  private ShowDatabasesDesc showDatabasesDesc;
  private ShowTablesDesc showTblsDesc;
  private ShowColumnsDesc showColumnsDesc;
  private ShowTblPropertiesDesc showTblPropertiesDesc;
  private LockTableDesc lockTblDesc;
  private UnlockTableDesc unlockTblDesc;
  private ShowFunctionsDesc showFuncsDesc;
  private ShowLocksDesc showLocksDesc;
  private DescFunctionDesc descFunctionDesc;
  private ShowPartitionsDesc showPartsDesc;
  private ShowCreateTableDesc showCreateTblDesc;
  private DescTableDesc descTblDesc;
  private AddPartitionDesc addPartitionDesc;
  private RenamePartitionDesc renamePartitionDesc;
  private AlterTableSimpleDesc alterTblSimpleDesc;
  private MsckDesc msckDesc;
  private ShowTableStatusDesc showTblStatusDesc;
  private ShowIndexesDesc showIndexesDesc;
  private DescDatabaseDesc descDbDesc;
  private AlterDatabaseDesc alterDbDesc;

  private RoleDDLDesc roleDDLDesc;
  private GrantDesc grantDesc;
  private ShowGrantDesc showGrantDesc;
  private RevokeDesc revokeDesc;
  private GrantRevokeRoleDDL grantRevokeRoleDDL;

  boolean needLock = false;

  private UserDDLDesc userDDLDesc;          //added by liulichao

  /**
   * ReadEntitites that are passed to the hooks.
   */
  protected HashSet<ReadEntity> inputs;
  /**
   * List of WriteEntities that are passed to the hooks.
   */
  protected HashSet<WriteEntity> outputs;
  private AlterTablePartMergeFilesDesc mergeFilesDesc;


  // added by zjw
  private CreateDatacenterDesc createDatacenterDesc;
  private SwitchDatacenterDesc switchDatacenterDesc;
  private DropDatacenterDesc dropDatacenterDesc;

  private ModifyNodeDesc modifyNodeDesc;
  private DropNodeDesc dropNodeDesc;
  private AddNodeDesc addNodeDesc;

  private AlterDatawareHouseDesc alterDatawareHouseDesc;

  private ModifySubpartIndexDropFileDesc modifySubpartIndexDropFileDesc;
  private ModifyPartIndexDropFileDesc modifyPartIndexDropFileDesc;
  private ModifySubpartIndexAddFileDesc modifySubpartIndexAddFileDesc;
  private ModifyPartIndexAddFileDesc modifyPartIndexAddFileDesc;
  private AddSubpartIndexDesc addSubpartIndexsDesc;
  private AddPartIndexDesc addPartIndexsDesc;
  private DropSubpartIndexDesc dropSubpartIndexsDesc;
  private DropPartIndexDesc  dropPartIndexsDesc;
  private ModifySubpartitionDropFileDesc modifySubpartitionDropFileDesc;
  private ModifySubpartitionAddFileDesc modifySubpartitionAddFileDesc;
  private ModifyPartitionDropFileDesc modifyPartitionDropFileDesc;
  private ModifyPartitionAddFileDesc modifyPartitionAddFileDesc;
  private AddSubpartitionDesc addSubpartitionDesc;
//  private AddPartitionDesc addPartitionDesc;
  private DropSubpartitionDesc dropSubpartitionDesc;
  private DropPartitionDesc dropPartitionDesc;

  private ShowSubpartitionDesc showSubpartitionDesc;
  private ShowPartitionKeysDesc showPartitionKeysDesc;
  private ShowDatacentersDesc showDatacentersDesc;
  private CreateBusitypeDesc createBusitypeDesc;
  private ShowBusitypesDesc showBusitypesDesc;

  private ShowFilesDesc showFilesDesc;
  private ShowNodesDesc showNodesDesc;
  private ShowFileLocationsDesc showFileLocationsDesc;
  private AddGeoLocDesc addGeoLocDesc;
  private DropGeoLocDesc dropGeoLocDesc;
  private ModifyGeoLocDesc modifyGeoLocDesc;
  private ShowGeoLocDesc showGeoLocDesc;
  private AddEqRoomDesc addEqRoomDesc;
  private DropEqRoomDesc dropEqRoomDesc;
  private ModifyEqRoomDesc modifyEqRoomDesc;
  private ShowEqRoomDesc showEqRoomDesc;
  private AddNodeAssignmentDesc addNode_AssignmentDesc;
  private DropNodeAssignmentDesc dropNodeAssignmentDesc;
  private ShowNodeAssignmentDesc showNodeAssignmentDesc;
  private CreateNodeGroupDesc createNodeGroupDesc;
  private DropNodeGroupDesc dropNodeGroupDesc;
  private ModifyNodeGroupDesc modifyNodeGroupDesc;
  private ShowNodeGroupDesc showNodeGroupDesc;


  public DDLWork() {
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs) {
    this.inputs = inputs;
    this.outputs = outputs;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      CreateIndexDesc createIndex) {
    this(inputs, outputs);
    this.createIndexDesc = createIndex;
  }

  public DDLWork(AlterIndexDesc alterIndex) {
    this.alterIndexDesc = alterIndex;
  }

  /**
   * @param createDatabaseDesc
   *          Create Database descriptor
   */
  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      CreateDatabaseDesc createDatabaseDesc) {
    this(inputs, outputs);
    this.createDatabaseDesc = createDatabaseDesc;
  }

  /**
   * @param inputs
   * @param outputs
   * @param descDatabaseDesc Database descriptor
   */
  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      DescDatabaseDesc descDatabaseDesc) {
    this(inputs, outputs);
    this.descDbDesc = descDatabaseDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      AlterDatabaseDesc alterDbDesc) {
    this(inputs, outputs);
    this.alterDbDesc = alterDbDesc;
  }

  public DescDatabaseDesc getDescDatabaseDesc() {
    return descDbDesc;
  }

  /**
   * @param dropDatabaseDesc
   *          Drop Database descriptor
   */
  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      DropDatabaseDesc dropDatabaseDesc) {
    this(inputs, outputs);
    this.dropDatabaseDesc = dropDatabaseDesc;
  }

  /**
   * @param switchDatabaseDesc
   *          Switch Database descriptor
   */
  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      SwitchDatabaseDesc switchDatabaseDesc) {
    this(inputs, outputs);
    this.switchDatabaseDesc = switchDatabaseDesc;
  }

  /**
   * @param alterTblDesc
   *          alter table descriptor
   */
  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      AlterTableDesc alterTblDesc) {
    this(inputs, outputs);
    this.alterTblDesc = alterTblDesc;
  }

  /**
   * @param alterIdxDesc
   *          alter index descriptor
   */
  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      AlterIndexDesc alterIdxDesc) {
    this(inputs, outputs);
    this.alterIdxDesc = alterIdxDesc;
  }

  /**
   * @param createTblDesc
   *          create table descriptor
   */
  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      CreateTableDesc createTblDesc) {
    this(inputs, outputs);

    this.createTblDesc = createTblDesc;
  }

  /**
   * @param createTblLikeDesc
   *          create table like descriptor
   */
  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      CreateTableLikeDesc createTblLikeDesc) {
    this(inputs, outputs);

    this.createTblLikeDesc = createTblLikeDesc;
  }

  /**
   * @param createVwDesc
   *          create view descriptor
   */
  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      CreateViewDesc createVwDesc) {
    this(inputs, outputs);

    this.createVwDesc = createVwDesc;
  }

  /**
   * @param dropTblDesc
   *          drop table descriptor
   */
  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      DropTableDesc dropTblDesc) {
    this(inputs, outputs);

    this.dropTblDesc = dropTblDesc;
  }

  /**
   * @param descTblDesc
   */
  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      DescTableDesc descTblDesc) {
    this(inputs, outputs);

    this.descTblDesc = descTblDesc;
  }

  /**
   * @param showDatabasesDesc
   */
  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      ShowDatabasesDesc showDatabasesDesc) {
    this(inputs, outputs);

    this.showDatabasesDesc = showDatabasesDesc;
  }

  /**
   * @param showTblsDesc
   */
  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      ShowTablesDesc showTblsDesc) {
    this(inputs, outputs);

    this.showTblsDesc = showTblsDesc;
  }

  /**
   * @param showColumnsDesc
   */
  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      ShowColumnsDesc showColumnsDesc) {
    this(inputs, outputs);

    this.showColumnsDesc = showColumnsDesc;
  }

  /**
   * @param lockTblDesc
   */
  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      LockTableDesc lockTblDesc) {
    this(inputs, outputs);

    this.lockTblDesc = lockTblDesc;
  }

  /**
   * @param unlockTblDesc
   */
  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      UnlockTableDesc unlockTblDesc) {
    this(inputs, outputs);

    this.unlockTblDesc = unlockTblDesc;
  }

  /**
   * @param showFuncsDesc
   */
  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      ShowFunctionsDesc showFuncsDesc) {
    this(inputs, outputs);

    this.showFuncsDesc = showFuncsDesc;
  }

  /**
   * @param showLocksDesc
   */
  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      ShowLocksDesc showLocksDesc) {
    this(inputs, outputs);

    this.showLocksDesc = showLocksDesc;
  }

  /**
   * @param descFuncDesc
   */
  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      DescFunctionDesc descFuncDesc) {
    this(inputs, outputs);

    descFunctionDesc = descFuncDesc;
  }

  /**
   * @param showPartsDesc
   */
  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      ShowPartitionsDesc showPartsDesc) {
    this(inputs, outputs);

    this.showPartsDesc = showPartsDesc;
  }

  /**
   * @param showCreateTblDesc
   */
  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      ShowCreateTableDesc showCreateTblDesc) {
    this(inputs, outputs);

    this.showCreateTblDesc = showCreateTblDesc;
  }

  /**
   * @param addPartitionDesc
   *          information about the partitions we want to add.
   */
  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      AddPartitionDesc addPartitionDesc) {
    this(inputs, outputs);

    this.addPartitionDesc = addPartitionDesc;
  }

  /**
   * @param renamePartitionDesc
   *          information about the partitions we want to add.
   */
  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      RenamePartitionDesc renamePartitionDesc) {
    this(inputs, outputs);

    this.renamePartitionDesc = renamePartitionDesc;
  }

  /**
   * @param inputs
   * @param outputs
   * @param simpleDesc
   */
  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      AlterTableSimpleDesc simpleDesc) {
    this(inputs, outputs);

    this.alterTblSimpleDesc = simpleDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      MsckDesc checkDesc) {
    this(inputs, outputs);

    msckDesc = checkDesc;
  }

  /**
   * @param showTblStatusDesc
   *          show table status descriptor
   */
  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      ShowTableStatusDesc showTblStatusDesc) {
    this(inputs, outputs);

    this.showTblStatusDesc = showTblStatusDesc;
  }

  /**
   * @param showTblPropertiesDesc
   *          show table properties descriptor
   */
  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      ShowTblPropertiesDesc showTblPropertiesDesc) {
    this(inputs, outputs);

    this.showTblPropertiesDesc = showTblPropertiesDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      DropIndexDesc dropIndexDesc) {
    this(inputs, outputs);
    this.dropIdxDesc = dropIndexDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      RoleDDLDesc roleDDLDesc) {
    this(inputs, outputs);
    this.roleDDLDesc = roleDDLDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      GrantDesc grantDesc) {
    this(inputs, outputs);
    this.grantDesc = grantDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      ShowGrantDesc showGrant) {
    this(inputs, outputs);
    this.showGrantDesc = showGrant;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      RevokeDesc revokeDesc) {
    this(inputs, outputs);
    this.revokeDesc = revokeDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      GrantRevokeRoleDDL grantRevokeRoleDDL) {
    this(inputs, outputs);
    this.grantRevokeRoleDDL = grantRevokeRoleDDL;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      ShowIndexesDesc showIndexesDesc) {
    this(inputs, outputs);
    this.showIndexesDesc = showIndexesDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      AlterTablePartMergeFilesDesc mergeDesc) {
    this(inputs, outputs);
    this.mergeFilesDesc = mergeDesc;
  }

  //added by liulichao.
  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      UserDDLDesc userDDLDesc) {
    this(inputs, outputs);
    this.userDDLDesc = userDDLDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      CreateDatacenterDesc createDatacenterDesc) {
    this(inputs, outputs);
    this.createDatacenterDesc = createDatacenterDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      SwitchDatacenterDesc switchDatacenterDesc) {
    this(inputs, outputs);
    this.switchDatacenterDesc = switchDatacenterDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      DropDatacenterDesc dropDatacenterDesc) {
    this(inputs, outputs);
    this.dropDatacenterDesc = dropDatacenterDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      DropNodeDesc dropNodeDesc) {
    this(inputs, outputs);
    this.dropNodeDesc = dropNodeDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      ModifyNodeDesc modifyNodeDesc) {
    this(inputs, outputs);
    this.modifyNodeDesc = modifyNodeDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      AddNodeDesc addNodeDesc) {
    this(inputs, outputs);
    this.addNodeDesc = addNodeDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      AddSubpartitionDesc addSubpartitionDesc) {
    this(inputs, outputs);
    this.addSubpartitionDesc = addSubpartitionDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      ModifyPartitionAddFileDesc modifyPartitionAddFileDesc) {
    this(inputs, outputs);
    this.modifyPartitionAddFileDesc = modifyPartitionAddFileDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      DropPartitionDesc dropPartitionDesc) {
    this(inputs, outputs);
    this.dropPartitionDesc = dropPartitionDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      DropSubpartitionDesc dropSubpartitionDesc) {
    this(inputs, outputs);
    this.dropSubpartitionDesc = dropSubpartitionDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      ModifySubpartitionDropFileDesc modifySubpartitionDropFileDesc) {
    this(inputs, outputs);
    this.modifySubpartitionDropFileDesc = modifySubpartitionDropFileDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      ModifySubpartitionAddFileDesc modifySubpartitionAddFileDesc) {
    this(inputs, outputs);
    this.modifySubpartitionAddFileDesc = modifySubpartitionAddFileDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      ModifyPartitionDropFileDesc modifyPartitionDropFileDesc) {
    this(inputs, outputs);
    this.modifyPartitionDropFileDesc = modifyPartitionDropFileDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      DropPartIndexDesc dropPartIndexsDesc) {
    this(inputs, outputs);
    this.dropPartIndexsDesc = dropPartIndexsDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      DropSubpartIndexDesc dropSubpartIndexsDesc) {
    this(inputs, outputs);
    this.dropSubpartIndexsDesc = dropSubpartIndexsDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      AddPartIndexDesc addPartIndexsDesc) {
    this(inputs, outputs);
    this.addPartIndexsDesc = addPartIndexsDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      AddSubpartIndexDesc addSubpartIndexsDesc) {
    this(inputs, outputs);
    this.addSubpartIndexsDesc = addSubpartIndexsDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      ModifyPartIndexAddFileDesc modifyPartIndexAddFileDesc) {
    this(inputs, outputs);
    this.modifyPartIndexAddFileDesc = modifyPartIndexAddFileDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      ModifySubpartIndexAddFileDesc modifySubpartIndexAddFileDesc) {
    this(inputs, outputs);
    this.modifySubpartIndexAddFileDesc = modifySubpartIndexAddFileDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      ModifyPartIndexDropFileDesc modifyPartIndexDropFileDesc) {
    this(inputs, outputs);
    this.modifyPartIndexDropFileDesc = modifyPartIndexDropFileDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      ModifySubpartIndexDropFileDesc modifySubpartIndexDropFileDesc) {
    this(inputs, outputs);
    this.modifySubpartIndexDropFileDesc = modifySubpartIndexDropFileDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      AlterDatawareHouseDesc alterDatawareHouseDesc) {
    this(inputs, outputs);
    this.setAlterDatawareHouseDesc(alterDatawareHouseDesc);
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      ShowSubpartitionDesc showSubpartitionDesc) {
    this(inputs, outputs);
    this.setShowSubpartitionDesc(showSubpartitionDesc);
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      ShowPartitionKeysDesc showPartitionKeysDesc) {
    this(inputs, outputs);
    this.setShowPartitionKeysDesc(showPartitionKeysDesc);
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      ShowDatacentersDesc showDatacentersDesc) {
    this(inputs, outputs);
    this.setShowDatacentersDesc(showDatacentersDesc);
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      CreateBusitypeDesc createBusitypeDesc) {
    this(inputs, outputs);
    this.setCreateBusitypeDesc(createBusitypeDesc);
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      ShowBusitypesDesc showBusitypesDesc) {
    this(inputs, outputs);
    this.setShowBusitypesDesc(showBusitypesDesc);
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      ShowNodesDesc showNodesDesc) {
    this(inputs, outputs);
    this.setShowNodesDesc(showNodesDesc);
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      ShowFilesDesc showFilesDesc) {
    this(inputs, outputs);
    Log.info("---zjw--show"+showFilesDesc.getPartName()+"--tab"+showFilesDesc.getTable().getTableName());
    this.setShowFilesDesc(showFilesDesc);
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      ShowFileLocationsDesc showFileLocationsDesc) {
    this(inputs, outputs);
    this.setShowFileLocationsDesc(showFileLocationsDesc);
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      AddGeoLocDesc addGeoLocDesc) {
    this(inputs, outputs);
    this.addGeoLocDesc = addGeoLocDesc;
  }



  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      DropGeoLocDesc dropGeoLocDesc) {
    this(inputs, outputs);
    this.dropGeoLocDesc = dropGeoLocDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      ModifyGeoLocDesc modifyGeoLocDesc) {
    this(inputs, outputs);
    this.modifyGeoLocDesc = modifyGeoLocDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      ShowGeoLocDesc showGeoLocDesc) {
    this(inputs, outputs);
    this.showGeoLocDesc = showGeoLocDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      AddEqRoomDesc addEqRoomDesc) {
    this(inputs, outputs);
    this.addEqRoomDesc = addEqRoomDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      DropEqRoomDesc dropEqRoomDesc) {
    this(inputs, outputs);
    this.dropEqRoomDesc = dropEqRoomDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      ModifyEqRoomDesc modifyEqRoomDesc) {
    this(inputs, outputs);
    this.modifyEqRoomDesc = modifyEqRoomDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      ShowEqRoomDesc showEqRoomDesc) {
    this(inputs, outputs);
    this.showEqRoomDesc = showEqRoomDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      AddNodeAssignmentDesc addNode_AssignmentDesc) {
    this(inputs, outputs);
    this.addNode_AssignmentDesc = addNode_AssignmentDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      DropNodeAssignmentDesc dropNodeAssignmentDesc) {
    this(inputs, outputs);
    this.dropNodeAssignmentDesc = dropNodeAssignmentDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      ShowNodeAssignmentDesc showNodeAssignmentDesc) {
    this(inputs, outputs);
    this.showNodeAssignmentDesc = showNodeAssignmentDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      CreateNodeGroupDesc createNodeGroupDesc) {
    this(inputs, outputs);
    this.createNodeGroupDesc = createNodeGroupDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      DropNodeGroupDesc dropNodeGroupDesc) {
    this(inputs, outputs);
    this.dropNodeGroupDesc = dropNodeGroupDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      ModifyNodeGroupDesc modifyNodeGroupDesc) {
    this(inputs, outputs);
    this.modifyNodeGroupDesc = modifyNodeGroupDesc;
  }

  public DDLWork(HashSet<ReadEntity> inputs, HashSet<WriteEntity> outputs,
      ShowNodeGroupDesc showNodeGroupDesc) {
    this(inputs, outputs);
    this.showNodeGroupDesc = showNodeGroupDesc;
  }

  /**
   * @return Create Database descriptor
   */
  public CreateDatabaseDesc getCreateDatabaseDesc() {
    return createDatabaseDesc;
  }

  /**
   * Set Create Database descriptor
   * @param createDatabaseDesc
   */
  public void setCreateDatabaseDesc(CreateDatabaseDesc createDatabaseDesc) {
    this.createDatabaseDesc = createDatabaseDesc;
  }

  /**
   * @return Drop Database descriptor
   */
  public DropDatabaseDesc getDropDatabaseDesc() {
    return dropDatabaseDesc;
  }

  /**
   * Set Drop Database descriptor
   * @param dropDatabaseDesc
   */
  public void setDropDatabaseDesc(DropDatabaseDesc dropDatabaseDesc) {
    this.dropDatabaseDesc = dropDatabaseDesc;
  }

  /**
   * @return Switch Database descriptor
   */
  public SwitchDatabaseDesc getSwitchDatabaseDesc() {
    return switchDatabaseDesc;
  }

  /**
   * Set Switch Database descriptor
   * @param switchDatabaseDesc
   */
  public void setSwitchDatabaseDesc(SwitchDatabaseDesc switchDatabaseDesc) {
    this.switchDatabaseDesc = switchDatabaseDesc;
  }

  /**
   * @return the createTblDesc
   */
  @Explain(displayName = "Create Table Operator")
  public CreateTableDesc getCreateTblDesc() {
    return createTblDesc;
  }

  /**
   * @param createTblDesc
   *          the createTblDesc to set
   */
  public void setCreateTblDesc(CreateTableDesc createTblDesc) {
    this.createTblDesc = createTblDesc;
  }

  /**
   * @return the createIndexDesc
   */
  public CreateIndexDesc getCreateIndexDesc() {
    return createIndexDesc;
  }

  /**
   * @param createIndexDesc
   *          the createIndexDesc to set
   */
  public void setCreateIndexDesc(CreateIndexDesc createIndexDesc) {
    this.createIndexDesc = createIndexDesc;
  }

  /**
   * @return the alterIndexDesc
   */
  public AlterIndexDesc getAlterIndexDesc() {
    return alterIndexDesc;
  }

  /**
   * @param alterIndexDesc
   *          the alterIndexDesc to set
   */
  public void setAlterIndexDesc(AlterIndexDesc alterIndexDesc) {
    this.alterIndexDesc = alterIndexDesc;
  }

  /**
   * @return the createTblDesc
   */
  @Explain(displayName = "Create Table Operator")
  public CreateTableLikeDesc getCreateTblLikeDesc() {
    return createTblLikeDesc;
  }

  /**
   * @param createTblLikeDesc
   *          the createTblDesc to set
   */
  public void setCreateTblLikeDesc(CreateTableLikeDesc createTblLikeDesc) {
    this.createTblLikeDesc = createTblLikeDesc;
  }

  /**
   * @return the createTblDesc
   */
  @Explain(displayName = "Create View Operator")
  public CreateViewDesc getCreateViewDesc() {
    return createVwDesc;
  }

  /**
   * @param createVwDesc
   *          the createViewDesc to set
   */
  public void setCreateViewDesc(CreateViewDesc createVwDesc) {
    this.createVwDesc = createVwDesc;
  }

  /**
   * @return the dropTblDesc
   */
  @Explain(displayName = "Drop Table Operator")
  public DropTableDesc getDropTblDesc() {
    return dropTblDesc;
  }

  /**
   * @param dropTblDesc
   *          the dropTblDesc to set
   */
  public void setDropTblDesc(DropTableDesc dropTblDesc) {
    this.dropTblDesc = dropTblDesc;
  }

  /**
   * @return the alterTblDesc
   */
  @Explain(displayName = "Alter Table Operator")
  public AlterTableDesc getAlterTblDesc() {
    return alterTblDesc;
  }

  /**
   * @param alterTblDesc
   *          the alterTblDesc to set
   */
  public void setAlterTblDesc(AlterTableDesc alterTblDesc) {
    this.alterTblDesc = alterTblDesc;
  }

  /**
   * @return the showDatabasesDesc
   */
  @Explain(displayName = "Show Databases Operator")
  public ShowDatabasesDesc getShowDatabasesDesc() {
    return showDatabasesDesc;
  }

  /**
   * @param showDatabasesDesc
   *          the showDatabasesDesc to set
   */
  public void setShowDatabasesDesc(ShowDatabasesDesc showDatabasesDesc) {
    this.showDatabasesDesc = showDatabasesDesc;
  }

  /**
   * @return the showTblsDesc
   */
  @Explain(displayName = "Show Table Operator")
  public ShowTablesDesc getShowTblsDesc() {
    return showTblsDesc;
  }

  /**
   * @param showTblsDesc
   *          the showTblsDesc to set
   */
  public void setShowTblsDesc(ShowTablesDesc showTblsDesc) {
    this.showTblsDesc = showTblsDesc;
  }

  /**
   * @return the showColumnsDesc
   */
  @Explain(displayName = "Show Columns Operator")
  public ShowColumnsDesc getShowColumnsDesc() {
    return showColumnsDesc;
  }

  /**
   * @param showColumnsDesc
   *          the showColumnsDesc to set
   */
  public void setShowColumnsDesc(ShowColumnsDesc showColumnsDesc) {
    this.showColumnsDesc = showColumnsDesc;
  }

  /**
   * @return the showFuncsDesc
   */
  @Explain(displayName = "Show Function Operator")
  public ShowFunctionsDesc getShowFuncsDesc() {
    return showFuncsDesc;
  }

  /**
   * @return the showLocksDesc
   */
  @Explain(displayName = "Show Lock Operator")
  public ShowLocksDesc getShowLocksDesc() {
    return showLocksDesc;
  }

  /**
   * @return the lockTblDesc
   */
  @Explain(displayName = "Lock Table Operator")
  public LockTableDesc getLockTblDesc() {
    return lockTblDesc;
  }

  /**
   * @return the unlockTblDesc
   */
  @Explain(displayName = "Unlock Table Operator")
  public UnlockTableDesc getUnlockTblDesc() {
    return unlockTblDesc;
  }

  /**
   * @return the descFuncDesc
   */
  @Explain(displayName = "Show Function Operator")
  public DescFunctionDesc getDescFunctionDesc() {
    return descFunctionDesc;
  }

  /**
   * @param showFuncsDesc
   *          the showFuncsDesc to set
   */
  public void setShowFuncsDesc(ShowFunctionsDesc showFuncsDesc) {
    this.showFuncsDesc = showFuncsDesc;
  }

  /**
   * @param showLocksDesc
   *          the showLocksDesc to set
   */
  public void setShowLocksDesc(ShowLocksDesc showLocksDesc) {
    this.showLocksDesc = showLocksDesc;
  }

  /**
   * @param lockTblDesc
   *          the lockTblDesc to set
   */
  public void setLockTblDesc(LockTableDesc lockTblDesc) {
    this.lockTblDesc = lockTblDesc;
  }

  /**
   * @param unlockTblDesc
   *          the unlockTblDesc to set
   */
  public void setUnlockTblDesc(UnlockTableDesc unlockTblDesc) {
    this.unlockTblDesc = unlockTblDesc;
  }

  /**
   * @param descFuncDesc
   *          the showFuncsDesc to set
   */
  public void setDescFuncDesc(DescFunctionDesc descFuncDesc) {
    descFunctionDesc = descFuncDesc;
  }

  /**
   * @return the showPartsDesc
   */
  @Explain(displayName = "Show Partitions Operator")
  public ShowPartitionsDesc getShowPartsDesc() {
    return showPartsDesc;
  }

  /**
   * @param showPartsDesc
   *          the showPartsDesc to set
   */
  public void setShowPartsDesc(ShowPartitionsDesc showPartsDesc) {
    this.showPartsDesc = showPartsDesc;
  }

  /**
   * @return the showCreateTblDesc
   */
  @Explain(displayName = "Show Create Table Operator")
  public ShowCreateTableDesc getShowCreateTblDesc() {
    return showCreateTblDesc;
  }

  /**
   * @param showCreateTblDesc
   *          the showCreateTblDesc to set
   */
  public void setShowCreateTblDesc(ShowCreateTableDesc showCreateTblDesc) {
    this.showCreateTblDesc = showCreateTblDesc;
  }

  /**
   * @return the showIndexesDesc
   */
  @Explain(displayName = "Show Index Operator")
  public ShowIndexesDesc getShowIndexesDesc() {
    return showIndexesDesc;
  }

  public void setShowIndexesDesc(ShowIndexesDesc showIndexesDesc) {
    this.showIndexesDesc = showIndexesDesc;
  }

  /**
   * @return the descTblDesc
   */
  @Explain(displayName = "Describe Table Operator")
  public DescTableDesc getDescTblDesc() {
    return descTblDesc;
  }

  /**
   * @param descTblDesc
   *          the descTblDesc to set
   */
  public void setDescTblDesc(DescTableDesc descTblDesc) {
    this.descTblDesc = descTblDesc;
  }

  /**
   * @return information about the partitions we want to add.
   */
  public AddPartitionDesc getAddPartitionDesc() {
    return addPartitionDesc;
  }

  /**
   * @param addPartitionDesc
   *          information about the partitions we want to add.
   */
  public void setAddPartitionDesc(AddPartitionDesc addPartitionDesc) {
    this.addPartitionDesc = addPartitionDesc;
  }

  /**
   * @return information about the partitions we want to rename.
   */
  public RenamePartitionDesc getRenamePartitionDesc() {
    return renamePartitionDesc;
  }

  /**
   * @param renamePartitionDesc
   *          information about the partitions we want to rename.
   */
  public void setRenamePartitionDesc(RenamePartitionDesc renamePartitionDesc) {
    this.renamePartitionDesc = renamePartitionDesc;
  }

  /**
   * @return information about the table/partitions we want to alter.
   */
  public AlterTableSimpleDesc getAlterTblSimpleDesc() {
    return alterTblSimpleDesc;
  }

  /**
   * @param desc
   *          information about the table/partitions we want to alter.
   */
  public void setAlterTblSimpleDesc(AlterTableSimpleDesc desc) {
    this.alterTblSimpleDesc = desc;
  }

  /**
   * @return Metastore check description
   */
  public MsckDesc getMsckDesc() {
    return msckDesc;
  }

  /**
   * @param msckDesc
   *          metastore check description
   */
  public void setMsckDesc(MsckDesc msckDesc) {
    this.msckDesc = msckDesc;
  }

  /**
   * @return show table descriptor
   */
  public ShowTableStatusDesc getShowTblStatusDesc() {
    return showTblStatusDesc;
  }

  /**
   * @param showTblStatusDesc
   *          show table descriptor
   */
  public void setShowTblStatusDesc(ShowTableStatusDesc showTblStatusDesc) {
    this.showTblStatusDesc = showTblStatusDesc;
  }

  public ShowTblPropertiesDesc getShowTblPropertiesDesc() {
    return showTblPropertiesDesc;
  }

  public void setShowTblPropertiesDesc(ShowTblPropertiesDesc showTblPropertiesDesc) {
    this.showTblPropertiesDesc = showTblPropertiesDesc;
  }

  public CreateViewDesc getCreateVwDesc() {
    return createVwDesc;
  }

  public void setCreateVwDesc(CreateViewDesc createVwDesc) {
    this.createVwDesc = createVwDesc;
  }

  public void setDescFunctionDesc(DescFunctionDesc descFunctionDesc) {
    this.descFunctionDesc = descFunctionDesc;
  }

  public HashSet<ReadEntity> getInputs() {
    return inputs;
  }

  public HashSet<WriteEntity> getOutputs() {
    return outputs;
  }

  public void setInputs(HashSet<ReadEntity> inputs) {
    this.inputs = inputs;
  }

  public void setOutputs(HashSet<WriteEntity> outputs) {
    this.outputs = outputs;
  }

  public DropIndexDesc getDropIdxDesc() {
    return dropIdxDesc;
  }

  public void setDropIdxDesc(DropIndexDesc dropIdxDesc) {
    this.dropIdxDesc = dropIdxDesc;
  }

  /**
   * @return role ddl desc
   */
  public RoleDDLDesc getRoleDDLDesc() {
    return roleDDLDesc;
  }

  /**
   * @param roleDDLDesc role ddl desc
   */
  public void setRoleDDLDesc(RoleDDLDesc roleDDLDesc) {
    this.roleDDLDesc = roleDDLDesc;
  }

  /**
   * @return grant desc
   */
  public GrantDesc getGrantDesc() {
    return grantDesc;
  }

  /**
   * @param grantDesc grant desc
   */
  public void setGrantDesc(GrantDesc grantDesc) {
    this.grantDesc = grantDesc;
  }

  /**
   * @return show grant desc
   */
  public ShowGrantDesc getShowGrantDesc() {
    return showGrantDesc;
  }

  /**
   * @param showGrantDesc
   */
  public void setShowGrantDesc(ShowGrantDesc showGrantDesc) {
    this.showGrantDesc = showGrantDesc;
  }

  public RevokeDesc getRevokeDesc() {
    return revokeDesc;
  }

  public void setRevokeDesc(RevokeDesc revokeDesc) {
    this.revokeDesc = revokeDesc;
  }

  public GrantRevokeRoleDDL getGrantRevokeRoleDDL() {
    return grantRevokeRoleDDL;
  }

  /**
   * @param grantRevokeRoleDDL
   */
  public void setGrantRevokeRoleDDL(GrantRevokeRoleDDL grantRevokeRoleDDL) {
    this.grantRevokeRoleDDL = grantRevokeRoleDDL;
  }

  public void setAlterDatabaseDesc(AlterDatabaseDesc alterDbDesc) {
    this.alterDbDesc = alterDbDesc;
  }

  public AlterDatabaseDesc getAlterDatabaseDesc() {
    return this.alterDbDesc;
  }

  /**
   * @return descriptor for merging files
   */
  public AlterTablePartMergeFilesDesc getMergeFilesDesc() {
    return mergeFilesDesc;
  }

  /**
   * @param mergeDesc descriptor of merging files
   */
  public void setMergeFilesDesc(AlterTablePartMergeFilesDesc mergeDesc) {
    this.mergeFilesDesc = mergeDesc;
  }

  public boolean getNeedLock() {
    return needLock;
  }

  public void setNeedLock(boolean needLock) {
    this.needLock = needLock;
  }

//added by liulichao
  /**
   * @return user ddl desc
   */
  public UserDDLDesc getUserDDLDesc() {
    return userDDLDesc;
  }

  /**
   * @param userDDLDesc user ddl desc
   */
  public void setUserDDLDesc(UserDDLDesc userDDLDesc) {
    this.userDDLDesc = userDDLDesc;
  }
  //added by liulichao

  public CreateDatacenterDesc getCreateDatacenterDesc() {
    return createDatacenterDesc;
  }

  public void setCreateDatacenterDesc(CreateDatacenterDesc createDatacenterDesc) {
    this.createDatacenterDesc = createDatacenterDesc;
  }

  public SwitchDatacenterDesc getSwitchDatacenterDesc() {
    return switchDatacenterDesc;
  }

  public void setSwitchDatacenterDesc(SwitchDatacenterDesc switchDatacenterDesc) {
    this.switchDatacenterDesc = switchDatacenterDesc;
  }

  public DropDatacenterDesc getDropDatacenterDesc() {
    return dropDatacenterDesc;
  }

  public void setDropDatacenterDesc(DropDatacenterDesc dropDatacenterDesc) {
    this.dropDatacenterDesc = dropDatacenterDesc;
  }

  public ModifyNodeDesc getModifyNodeDesc() {
    return modifyNodeDesc;
  }

  public void setModifyNodeDesc(ModifyNodeDesc modifyNodeDesc) {
    this.modifyNodeDesc = modifyNodeDesc;
  }

  public DropNodeDesc getDropNodeDesc() {
    return dropNodeDesc;
  }

  public void setDropNodeDesc(DropNodeDesc dropNodeDesc) {
    this.dropNodeDesc = dropNodeDesc;
  }

  public AddNodeDesc getAddNodeDesc() {
    return addNodeDesc;
  }

  public void setAddNodeDesc(AddNodeDesc addNodeDesc) {
    this.addNodeDesc = addNodeDesc;
  }

  public ModifySubpartIndexDropFileDesc getModifySubpartIndexDropFileDesc() {
    return modifySubpartIndexDropFileDesc;
  }

  public void setModifySubpartIndexDropFileDesc(
      ModifySubpartIndexDropFileDesc modifySubpartIndexDropFileDesc) {
    this.modifySubpartIndexDropFileDesc = modifySubpartIndexDropFileDesc;
  }

  public ModifyPartIndexDropFileDesc getModifyPartIndexDropFileDesc() {
    return modifyPartIndexDropFileDesc;
  }

  public void setModifyPartIndexDropFileDesc(ModifyPartIndexDropFileDesc modifyPartIndexDropFileDesc) {
    this.modifyPartIndexDropFileDesc = modifyPartIndexDropFileDesc;
  }

  public ModifySubpartIndexAddFileDesc getModifySubpartIndexAddFileDesc() {
    return modifySubpartIndexAddFileDesc;
  }

  public void setModifySubpartIndexAddFileDesc(
      ModifySubpartIndexAddFileDesc modifySubpartIndexAddFileDesc) {
    this.modifySubpartIndexAddFileDesc = modifySubpartIndexAddFileDesc;
  }

  public ModifyPartIndexAddFileDesc getModifyPartIndexAddFileDesc() {
    return modifyPartIndexAddFileDesc;
  }

  public void setModifyPartIndexAddFileDesc(ModifyPartIndexAddFileDesc modifyPartIndexAddFileDesc) {
    this.modifyPartIndexAddFileDesc = modifyPartIndexAddFileDesc;
  }

  public AddSubpartIndexDesc getAddSubpartIndexsDesc() {
    return addSubpartIndexsDesc;
  }

  public void setAddSubpartIndexsDesc(AddSubpartIndexDesc addSubpartIndexsDesc) {
    this.addSubpartIndexsDesc = addSubpartIndexsDesc;
  }

  public AddPartIndexDesc getAddPartIndexsDesc() {
    return addPartIndexsDesc;
  }

  public void setAddPartIndexsDesc(AddPartIndexDesc addPartIndexsDesc) {
    this.addPartIndexsDesc = addPartIndexsDesc;
  }

  public DropSubpartIndexDesc getDropSubpartIndexsDesc() {
    return dropSubpartIndexsDesc;
  }

  public void setDropSubpartIndexsDesc(DropSubpartIndexDesc dropSubpartIndexsDesc) {
    this.dropSubpartIndexsDesc = dropSubpartIndexsDesc;
  }

  public DropPartIndexDesc getDropPartIndexsDesc() {
    return dropPartIndexsDesc;
  }

  public void setDropPartIndexsDesc(DropPartIndexDesc dropPartIndexsDesc) {
    this.dropPartIndexsDesc = dropPartIndexsDesc;
  }

  public ModifySubpartitionDropFileDesc getModifySubpartitionDropFileDesc() {
    return modifySubpartitionDropFileDesc;
  }

  public void setModifySubpartitionDropFileDesc(
      ModifySubpartitionDropFileDesc modifySubpartitionDropFileDesc) {
    this.modifySubpartitionDropFileDesc = modifySubpartitionDropFileDesc;
  }

  public ModifySubpartitionAddFileDesc getModifySubpartitionAddFileDesc() {
    return modifySubpartitionAddFileDesc;
  }

  public void setModifySubpartitionAddFileDesc(
      ModifySubpartitionAddFileDesc modifySubpartitionAddFileDesc) {
    this.modifySubpartitionAddFileDesc = modifySubpartitionAddFileDesc;
  }

  public ModifyPartitionDropFileDesc getModifyPartitionDropFileDesc() {
    return modifyPartitionDropFileDesc;
  }

  public void setModifyPartitionDropFileDesc(ModifyPartitionDropFileDesc modifyPartitionDropFileDesc) {
    this.modifyPartitionDropFileDesc = modifyPartitionDropFileDesc;
  }

  public ModifyPartitionAddFileDesc getModifyPartitionAddFileDesc() {
    return modifyPartitionAddFileDesc;
  }

  public void setModifyPartitionAddFileDesc(ModifyPartitionAddFileDesc modifyPartitionAddFileDesc) {
    this.modifyPartitionAddFileDesc = modifyPartitionAddFileDesc;
  }

  public AddSubpartitionDesc getAddSubpartitionDesc() {
    return addSubpartitionDesc;
  }

  public void setAddSubpartitionDesc(AddSubpartitionDesc addSubpartitionDesc) {
    this.addSubpartitionDesc = addSubpartitionDesc;
  }

  public DropSubpartitionDesc getDropSubpartitionDesc() {
    return dropSubpartitionDesc;
  }

  public void setDropSubpartitionDesc(DropSubpartitionDesc dropSubpartitionDesc) {
    this.dropSubpartitionDesc = dropSubpartitionDesc;
  }

  public DropPartitionDesc getDropPartitionDesc() {
    return dropPartitionDesc;
  }

  public void setDropPartitionDesc(DropPartitionDesc dropPartitionDesc) {
    this.dropPartitionDesc = dropPartitionDesc;
  }

  public AlterDatawareHouseDesc getAlterDatawareHouseDesc() {
    return alterDatawareHouseDesc;
  }

  public void setAlterDatawareHouseDesc(AlterDatawareHouseDesc alterDatawareHouseDesc) {
    this.alterDatawareHouseDesc = alterDatawareHouseDesc;
  }

  public ShowSubpartitionDesc getShowSubpartitionDesc() {
    return showSubpartitionDesc;
  }

  public void setShowSubpartitionDesc(ShowSubpartitionDesc showSubpartitionDesc) {
    this.showSubpartitionDesc = showSubpartitionDesc;
  }

  public ShowPartitionKeysDesc getShowPartitionKeysDesc() {
    return showPartitionKeysDesc;
  }

  public void setShowPartitionKeysDesc(ShowPartitionKeysDesc showPartitionKeysDesc) {
    this.showPartitionKeysDesc = showPartitionKeysDesc;
  }

  public ShowDatacentersDesc getShowDatacentersDesc() {
    return showDatacentersDesc;
  }

  public void setShowDatacentersDesc(ShowDatacentersDesc showDatacentersDesc) {
    this.showDatacentersDesc = showDatacentersDesc;
  }

  public CreateBusitypeDesc getCreateBusitypeDesc() {
    return createBusitypeDesc;
  }

  public void setCreateBusitypeDesc(CreateBusitypeDesc createBusitypeDesc) {
    this.createBusitypeDesc = createBusitypeDesc;
  }

  public ShowBusitypesDesc getShowBusitypesDesc() {
    return showBusitypesDesc;
  }

  public void setShowBusitypesDesc(ShowBusitypesDesc showBusitypesDesc) {
    this.showBusitypesDesc = showBusitypesDesc;
  }

  public ShowFilesDesc getShowFilesDesc() {
    return showFilesDesc;
  }

  public void setShowFilesDesc(ShowFilesDesc showFilesDesc) {
    this.showFilesDesc = showFilesDesc;
  }

  public ShowNodesDesc getShowNodesDesc() {
    return showNodesDesc;
  }

  public void setShowNodesDesc(ShowNodesDesc showNodesDesc) {
    this.showNodesDesc = showNodesDesc;
  }

  public ShowFileLocationsDesc getShowFileLocationsDesc() {
    return showFileLocationsDesc;
  }

  public void setShowFileLocationsDesc(ShowFileLocationsDesc showFileLocationsDesc) {
    this.showFileLocationsDesc = showFileLocationsDesc;
  }

  public AddGeoLocDesc getAddGeoLocDesc() {
    return addGeoLocDesc;
  }

  public void setAddGeoLocDesc(AddGeoLocDesc addGeoLocDesc) {
    this.addGeoLocDesc = addGeoLocDesc;
  }

  public DropGeoLocDesc getDropGeoLocDesc() {
    return dropGeoLocDesc;
  }

  public void setDropGeoLocDesc(DropGeoLocDesc dropGeoLocDesc) {
    this.dropGeoLocDesc = dropGeoLocDesc;
  }

  public ModifyGeoLocDesc getModifyGeoLocDesc() {
    return modifyGeoLocDesc;
  }

  public void setModifyGeoLocDesc(ModifyGeoLocDesc modifyGeoLocDesc) {
    this.modifyGeoLocDesc = modifyGeoLocDesc;
  }

  public ShowGeoLocDesc getShowGeoLocDesc() {
    return showGeoLocDesc;
  }

  public void setShowGeoLocDesc(ShowGeoLocDesc showGeoLocDesc) {
    this.showGeoLocDesc = showGeoLocDesc;
  }

  public AddEqRoomDesc getAddEqRoomDesc() {
    return addEqRoomDesc;
  }

  public void setAddEqRoomDesc(AddEqRoomDesc addEqRoomDesc) {
    this.addEqRoomDesc = addEqRoomDesc;
  }

  public DropEqRoomDesc getDropEqRoomDesc() {
    return dropEqRoomDesc;
  }

  public void setDropEqRoomDesc(DropEqRoomDesc dropEqRoomDesc) {
    this.dropEqRoomDesc = dropEqRoomDesc;
  }

  public ModifyEqRoomDesc getModifyEqRoomDesc() {
    return modifyEqRoomDesc;
  }

  public void setModifyEqRoomDesc(ModifyEqRoomDesc modifyEqRoomDesc) {
    this.modifyEqRoomDesc = modifyEqRoomDesc;
  }

  public ShowEqRoomDesc getShowEqRoomDesc() {
    return showEqRoomDesc;
  }

  public void setShowEqRoomDesc(ShowEqRoomDesc showEqRoomDesc) {
    this.showEqRoomDesc = showEqRoomDesc;
  }

  public AddNodeAssignmentDesc getAddNode_AssignmentDesc() {
    return addNode_AssignmentDesc;
  }

  public void setAddNode_AssignmentDesc(AddNodeAssignmentDesc addNode_AssignmentDesc) {
    this.addNode_AssignmentDesc = addNode_AssignmentDesc;
  }

  public DropNodeAssignmentDesc getDropNodeAssignmentDesc() {
    return dropNodeAssignmentDesc;
  }

  public void setDropNodeAssignmentDesc(DropNodeAssignmentDesc dropNodeAssignmentDesc) {
    this.dropNodeAssignmentDesc = dropNodeAssignmentDesc;
  }

  public ShowNodeAssignmentDesc getShowNodeAssignmentDesc() {
    return showNodeAssignmentDesc;
  }

  public void setShowNodeAssignmentDesc(ShowNodeAssignmentDesc showNodeAssignmentDesc) {
    this.showNodeAssignmentDesc = showNodeAssignmentDesc;
  }

  public CreateNodeGroupDesc getCreateNodeGroupDesc() {
    return createNodeGroupDesc;
  }

  public void setCreateNodeGroupDesc(CreateNodeGroupDesc createNodeGroupDesc) {
    this.createNodeGroupDesc = createNodeGroupDesc;
  }

  public DropNodeGroupDesc getDropNodeGroupDesc() {
    return dropNodeGroupDesc;
  }

  public void setDropNodeGroupDesc(DropNodeGroupDesc dropNodeGroupDesc) {
    this.dropNodeGroupDesc = dropNodeGroupDesc;
  }

  public ModifyNodeGroupDesc getModifyNodeGroupDesc() {
    return modifyNodeGroupDesc;
  }

  public void setModifyNodeGroupDesc(ModifyNodeGroupDesc modifyNodeGroupDesc) {
    this.modifyNodeGroupDesc = modifyNodeGroupDesc;
  }

  public ShowNodeGroupDesc getShowNodeGroupDesc() {
    return showNodeGroupDesc;
  }

  public void setShowNodeGroupDesc(ShowNodeGroupDesc showNodeGroupDesc) {
    this.showNodeGroupDesc = showNodeGroupDesc;
  }

}
