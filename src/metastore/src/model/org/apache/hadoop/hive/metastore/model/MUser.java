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

import java.util.Set;


//Implemented By : liulichao
//Implementation Date : 2013-05-15
public class MUser {

  private String userName;

  private MRole role;

  private String passwd;

  private int createTime;

  private String ownerName;

  private Set<MDatabase> dbs;

  public MUser(String userName, int createTime, String ownerName) {
    super();
    this.userName = userName;
    this.createTime = createTime;
    this.ownerName = ownerName;
  }

  public MUser(String userName, MRole role, int createTime, String ownerName, String passwd) {
    super();
    this.userName = userName;
    this.role = role;
    this.createTime = createTime;
    this.passwd = passwd;
  }

  public MUser(String userName, MRole role, int createTime, String ownerName, String passwd,Set<MDatabase> dbs) {
    this(userName, role, createTime, ownerName, passwd);
    this.dbs = dbs;
  }

  /**
   * @param roleName
   */
  public MUser() {

  }

  public MUser(String userName, String passwd, int createTime, String ownerName) {
    super();
    this.userName = userName;
    this.passwd = passwd;
    this.createTime = createTime;
    this.ownerName = ownerName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public MRole getRole() {
    return role;
  }

  public void setRole(MRole role) {
    this.role = role;
  }

  /**
   * @return create time
   */
  public int getCreateTime() {
    return createTime;
  }

  /**
   * @param createTime
   *          role create time
   */
  public void setCreateTime(int createTime) {
    this.createTime = createTime;
  }

  public String getPasswd() {
    return passwd;
  }

  public String getUserName() {
    return this.userName;
  }

  public void setPasswd(String passwd) {
    this.passwd = passwd;
  }

  /**
   * @return the principal name who created this role
   */
  public String getOwnerName() {
    return ownerName;
  }

  public void setOwnerName(String ownerName) {
    this.ownerName = ownerName;
  }

  public Set<MDatabase> getDbs() {
    return dbs;
  }

  public void setDbs(Set<MDatabase> dbs) {
    this.dbs = dbs;
  }

}
