package org.apache.hadoop.hive.ql.metadata;

import java.io.Serializable;

public class RoleAssignment implements Serializable {

  private static final long serialVersionUID = 1L;
  String dbName;
  String roleName;

  public RoleAssignment() {
  }

  public RoleAssignment(String dbName, String roleName) {
    super();
    this.dbName = dbName;
    this.roleName = roleName;
  }

  public String getDbName() {
    return dbName;
  }

  public void setDbName(String dbName) {
    this.dbName = dbName;
  }

  public String getRoleName() {
    return roleName;
  }

  public void setRoleName(String roleName) {
    this.roleName = roleName;
  }



}
