package org.apache.hadoop.hive.ql.plan;

import java.io.Serializable;

/**
 * DropNodeGroupAssignmentDesc.
 *
 */
@Explain(displayName = "Drop NodeGroupAssignment")
public class DropNodeGroupAssignmentDesc extends DDLDesc implements Serializable {

  private static final long serialVersionUID = 1L;

  String dbName;
  String nodeGroupName;

  public DropNodeGroupAssignmentDesc() {
  }

  public DropNodeGroupAssignmentDesc(String dbName, String nodeGroupName) {
    super();
    this.dbName = dbName;
    this.nodeGroupName = nodeGroupName;
  }

  @Explain(displayName="NodeGroupName")
  public String getNodeGroupName() {
    return nodeGroupName;
  }

  public void setNodeGroupName(String nodeGroupName) {
    this.nodeGroupName = nodeGroupName;
  }

  @Explain(displayName="DbName")
  public String getDbName() {
    return dbName;
  }
  public void setDbName(String dbName) {
    this.dbName = dbName;
  }

}
