package org.apache.hadoop.hive.ql.metadata;

import java.io.Serializable;

public class NodeAssignmentDesc implements Serializable {

  private static final long serialVersionUID = 1L;
  String nodeName;
  String dbName;

  public NodeAssignmentDesc() {
  }

  public NodeAssignmentDesc(String nodeName, String dbName) {
    super();
    this.nodeName = nodeName;
    this.dbName = dbName;
  }

  public String getNodeName() {
    return nodeName;
  }

  public void setNodeName(String nodeName) {
    this.nodeName = nodeName;
  }

  public String getDbName() {
    return dbName;
  }

  public void setDbName(String dbName) {
    this.dbName = dbName;
  }

}
