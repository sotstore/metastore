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

package org.apache.hadoop.hive.metastore.model;


public class MBusiTypeColumn {

  private String busiType; // partitionname ==>  (key=value/)*(key=value)
  private MTable table;
  private MFieldSchema column;

  public MBusiTypeColumn() {}

  public MBusiTypeColumn(String busiType, MTable table, MFieldSchema column) {
    super();
    this.busiType = busiType;
    this.table = table;
    this.column = column;
  }

  public String getBusiType() {
    return busiType;
  }

  public void setBusiType(String busiType) {
    this.busiType = busiType;
  }

  public MTable getTable() {
    return table;
  }

  public void setTable(MTable table) {
    this.table = table;
  }

  public MFieldSchema getColumn() {
    return column;
  }

  public void setColumn(MFieldSchema column) {
    this.column = column;
  }


}
