package org.apache.hadoop.hive.ql.plan;

import java.io.Serializable;

/**
 * CreateNode_AssignmentDesc.
 *
 */
@Explain(displayName = "Create Node_Assignment")
public class AddNodeAssignmentDesc extends DDLDesc implements Serializable {
  private static final long serialVersionUID = 1L;

  String nodeName;
  String dbName;

  public AddNodeAssignmentDesc() {
  }

  public AddNodeAssignmentDesc(String nodeName, String name) {
    super();
    this.nodeName = nodeName;
    this.dbName = name;
  }
  @Explain(displayName="NodeName")
  public String getNodeName() {
    return nodeName;
  }

  public void setNodeName(String nodeName) {
    this.nodeName = nodeName;
  }
  @Explain(displayName="DbName")
  public String getDbName() {
    return dbName;
  }

  public void setDbName(String dbName) {
    this.dbName = dbName;
  }




}
