package org.apache.hadoop.hive.ql.plan;

import java.io.Serializable;

/**
 * CreateNodeGroup_AssignmentDesc.
 *
 */
@Explain(displayName = "Create NodeGroup_Assignment")
public class AddNodeGroupAssignmentDesc extends DDLDesc implements Serializable {
  private static final long serialVersionUID = 1L;

  String dbName;
  String nodeGroupName;


  public AddNodeGroupAssignmentDesc() {
  }

  public AddNodeGroupAssignmentDesc(String dbName, String nodeGroupName) {
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
