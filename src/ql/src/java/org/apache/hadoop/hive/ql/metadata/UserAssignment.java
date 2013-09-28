package org.apache.hadoop.hive.ql.metadata;

import java.io.Serializable;

public class UserAssignment implements Serializable {

  private static final long serialVersionUID = 1L;
  String dbName;
  String userName;

  public UserAssignment() {
  }

  public UserAssignment(String dbName, String userName) {
    super();
    this.dbName = dbName;
    this.userName = userName;
  }

  public String getDbName() {
    return dbName;
  }

  public void setDbName(String dbName) {
    this.dbName = dbName;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }


}
