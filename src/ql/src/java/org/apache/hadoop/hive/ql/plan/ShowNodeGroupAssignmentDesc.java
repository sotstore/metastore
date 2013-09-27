package org.apache.hadoop.hive.ql.plan;

import java.io.Serializable;

public class ShowNodeGroupAssignmentDesc extends DDLDesc implements Serializable {

  private static final long serialVersionUID = 1L;
  String resFile;

  private static final String schema = "dbName,nodeGroupName#string:string";

  public ShowNodeGroupAssignmentDesc() {
    super();
  }

  public ShowNodeGroupAssignmentDesc(String resFile) {
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
