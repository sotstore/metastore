package org.apache.hadoop.hive.ql.plan;

import java.io.Serializable;
public class ShowNodeAssignmentDesc extends DDLDesc implements Serializable  {
  private static final long serialVersionUID = 1L;
  String resFile;

  private static final String schema = "nodeName,dbName#string:string";

  public ShowNodeAssignmentDesc() {
  }


  public ShowNodeAssignmentDesc(String resFile) {
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
