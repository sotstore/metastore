package org.apache.hadoop.hive.ql.metadata;

import java.io.Serializable;

public class NodeGroupAssignment implements Serializable {

  private static final long serialVersionUID = 1L;
  String dbName;
  String nodeGroupName;

  public NodeGroupAssignment() {
  }

  public NodeGroupAssignment(String dbName, String nodeGroupName) {
    super();
    this.dbName = dbName;
    this.nodeGroupName = nodeGroupName;
  }

  public String getDbName() {
    return dbName;
  }

  public void setDbName(String dbName) {
    this.dbName = dbName;
  }

  public String getNodeGroupName() {
    return nodeGroupName;
  }

  public void setNodeGroupName(String nodeGroupName) {
    this.nodeGroupName = nodeGroupName;
  }

}
