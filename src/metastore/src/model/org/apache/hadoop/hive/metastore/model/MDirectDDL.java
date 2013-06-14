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


public class MDirectDDL {

  private Integer dwNum;

  private String sql;


  private long createTime;

  private long updateTime;



  public MDirectDDL() {}


  public MDirectDDL(Integer dwNum, String sql, long createTime, long updateTime) {
    super();
    this.dwNum = dwNum;
    this.sql = sql;
    this.createTime = createTime;
    this.updateTime = updateTime;
  }





  public Integer getDwNum() {
    return dwNum;
  }


  public void setDwNum(Integer dwNum) {
    this.dwNum = dwNum;
  }


  public String getSql() {
    return sql;
  }


  public void setSql(String sql) {
    this.sql = sql;
  }


  public long getCreateTime() {
    return createTime;
  }


  public void setCreateTime(long createTime) {
    this.createTime = createTime;
  }


  public long getUpdateTime() {
    return updateTime;
  }


  public void setUpdateTime(long updateTime) {
    this.updateTime = updateTime;
  }


  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "MPartitionEvent [dwNum=" + dwNum + ", sql=" + sql + ", createTime=" + createTime
        + ", updateTime=" + updateTime + "]";
  }


}
