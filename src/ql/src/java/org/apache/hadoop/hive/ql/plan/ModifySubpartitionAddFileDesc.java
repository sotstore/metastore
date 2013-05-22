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
import java.util.List;

/**
 * Contains the information needed to add a subpartition.
 */
public class ModifySubpartitionAddFileDesc extends DDLDesc implements Serializable {

  private static final long serialVersionUID = 1L;

  String tableName;
  String dbName;
  String partitionName;
  List<Long> fids;//file id list

  /**
   * For serialization only.
   */
  public ModifySubpartitionAddFileDesc() {
  }

  /**
   * @param dbName
   *          database to add to.
   * @param tableName
   *          table to add to.
   * @param partSpec
   *          partition specification.
   * @param location
   *          partition location, relative to table location.
   * @param params
   *          partition parameters.
   */
  public ModifySubpartitionAddFileDesc(String dbName, String tableName, String partitionName,
      List<Long> fids) {

    this.dbName = dbName;
    this.tableName = tableName;
    this.partitionName  = partitionName;
    this.fids = fids;
  }

  /**
   * @return database name
   */
  public String getDbName() {
    return dbName;
  }

  /**
   * @param dbName
   *          database name
   */
  public void setDbName(String dbName) {
    this.dbName = dbName;
  }

  /**
   * @return the table we're going to add the partitions to.
   */
  public String getTableName() {
    return tableName;
  }

  /**
   * @param tableName
   *          the table we're going to add the partitions to.
   */
  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public String getPartitionName() {
    return partitionName;
  }

  public void setPartitionName(String partitionName) {
    this.partitionName = partitionName;
  }

  public List<Long> getFids() {
    return fids;
  }

  public void setFids(List<Long> fids) {
    this.fids = fids;
  }



}
