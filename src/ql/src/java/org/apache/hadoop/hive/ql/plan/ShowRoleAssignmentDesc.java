package org.apache.hadoop.hive.ql.plan;

import java.io.Serializable;

public class ShowRoleAssignmentDesc extends DDLDesc implements Serializable {

  private static final long serialVersionUID = 1L;
  String resFile;

  private static final String schema = "dbName,roleName#string:string";

  public ShowRoleAssignmentDesc() {
    super();
  }

  public ShowRoleAssignmentDesc(String resFile) {
    super();
    this.resFile = resFile;
  }

  public String getSchema() {
    return schema;
  }


  public String getResFile() {
    return resFile;
  }


  public void setResFile(String resFile) {
    this.resFile = resFile;
  }
}
