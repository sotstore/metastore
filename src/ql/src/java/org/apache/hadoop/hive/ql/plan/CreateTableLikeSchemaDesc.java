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
import java.util.Map;

import org.apache.hadoop.hive.metastore.api.FieldSchema;

/**
 * CreateTableLikeDesc.
 *
 */
@Explain(displayName = "Create Table")
public class CreateTableLikeSchemaDesc extends DDLDesc implements Serializable {
  private static final long serialVersionUID = 1L;
  String dbName;
  String tableName;
  boolean isExternal;
  String defaultInputFormat;
  String defaultOutputFormat;
  String defaultSerName;
  Map<String, String> defaultSerdeProps;
  String location;
  boolean ifNotExists;
  String likeTableName;
  List<String> nodeGroupNames;
  private List<FieldSchema> fileSplitCols;
  private List<FieldSchema> partCols;

  public CreateTableLikeSchemaDesc() {
  }

  public CreateTableLikeSchemaDesc(String dbName,String tableName, boolean isExternal,List<FieldSchema> partCols,
      String defaultInputFormat, String defaultOutputFormat, String location,
      String defaultSerName, Map<String, String> defaultSerdeProps, boolean ifNotExists,
      String likeTableName) {
    this.dbName = dbName;
    this.tableName = tableName;
    this.isExternal = isExternal;
    this.setPartCols(partCols);
    this.defaultInputFormat=defaultInputFormat;
    this.defaultOutputFormat=defaultOutputFormat;
    this.defaultSerName=defaultSerName;
    this.defaultSerdeProps=defaultSerdeProps;
    this.location = location;
    this.ifNotExists = ifNotExists;
    this.likeTableName = likeTableName;
  }



  public String getDbName() {
    return dbName;
  }

  public void setDbName(String dbName) {
    this.dbName = dbName;
  }

  @Explain(displayName = "if not exists")
  public boolean getIfNotExists() {
    return ifNotExists;
  }

  public void setIfNotExists(boolean ifNotExists) {
    this.ifNotExists = ifNotExists;
  }

  @Explain(displayName = "name")
  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  @Explain(displayName = "default input format")
  public String getDefaultInputFormat() {
    return defaultInputFormat;
  }

  public void setInputFormat(String inputFormat) {
    this.defaultInputFormat = inputFormat;
  }

  @Explain(displayName = "default output format")
  public String getDefaultOutputFormat() {
    return defaultOutputFormat;
  }

  public void setOutputFormat(String outputFormat) {
    this.defaultOutputFormat = outputFormat;
  }

  @Explain(displayName = "location")
  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  @Explain(displayName = "isExternal")
  public boolean isExternal() {
    return isExternal;
  }

  public void setExternal(boolean isExternal) {
    this.isExternal = isExternal;
  }

  /**
   * @return the default serDeName
   */
  @Explain(displayName = "default serde name")
  public String getDefaultSerName() {
    return defaultSerName;
  }

  /**
   * @param serName
   *          the serName to set
   */
  public void setDefaultSerName(String serName) {
    this.defaultSerName = serName;
  }

  /**
   * @return the default serDe properties
   */
  @Explain(displayName = "serde properties")
  public Map<String, String> getDefaultSerdeProps() {
    return defaultSerdeProps;
  }

  /**
   * @param serdeProps
   *          the default serde properties to set
   */
  public void setDefaultSerdeProps(Map<String, String> serdeProps) {
    this.defaultSerdeProps = serdeProps;
  }

  @Explain(displayName = "like")
  public String getLikeTableName() {
    return likeTableName;
  }

  public void setLikeTableName(String likeTableName) {
    this.likeTableName = likeTableName;
  }

  public List<String> getNodeGroupNames() {
    return nodeGroupNames;
  }

  public void setNodeGroupNames(List<String> nodeGroupNames) {
    this.nodeGroupNames = nodeGroupNames;
  }

  public List<FieldSchema> getPartCols() {
    return partCols;
  }

  public void setPartCols(List<FieldSchema> partCols) {
    this.partCols = partCols;
  }

  public List<FieldSchema> getFileSplitCols() {
    return fileSplitCols;
  }

  public void setFileSplitCols(List<FieldSchema> fileSplitCols) {
    this.fileSplitCols = fileSplitCols;
  }



}
