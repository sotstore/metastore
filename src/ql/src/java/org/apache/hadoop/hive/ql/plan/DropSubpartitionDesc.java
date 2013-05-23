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

/**
 * Contains the information needed to add a partition.
 */
public class DropSubpartitionDesc extends DDLDesc implements Serializable {

  private static final long serialVersionUID = 1L;

  String tableName;
  String dbName;
  String subpartitionName;
  boolean expectView;

  /**
   * For serialization only.
   */
  public DropSubpartitionDesc() {
  }

  /**
   * @param dbName
   *          database to add to.
   * @param tableName
   *          table to add to.
   * @param subpartitionName
   *          subpartitionName specification.
   */
  public DropSubpartitionDesc(String dbName, String tableName,String subpartitionName) {
    this(dbName, tableName, subpartitionName, false);
  }

  /**
   * @param dbName
   *          database to add to.
   * @param tableName
   *          table to add to.
   * @param subpartitionName
   *          subpartitionName.
   * @param expectView
   *          true for ALTER VIEW, false for ALTER TABLE
   */
  public DropSubpartitionDesc(String dbName, String tableName,String subpartitionName,boolean expectView) {
    super();
    this.dbName = dbName;
    this.tableName = tableName;
    this.subpartitionName = subpartitionName;
    this.expectView = expectView;
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

  /*
   * @return whether to expect a view being altered
   */
  public boolean getExpectView() {
    return expectView;
  }

  /**
   * @param expectView
   *          set whether to expect a view being altered
   */
  public void setExpectView(boolean expectView) {
    this.expectView = expectView;
  }

  public String getSubpartitionName() {
    return subpartitionName;
  }

  public void setSubpartitionName(String subpartitionName) {
    this.subpartitionName = subpartitionName;
  }



}
