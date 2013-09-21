package org.apache.hadoop.hive.ql.plan;

import java.io.Serializable;

/**
 * DropNodeAssignmentDesc.
 *
 */
@Explain(displayName = "Drop NodeAssignment")
public class DropNodeAssignmentDesc extends DDLDesc implements Serializable {

  private static final long serialVersionUID = 1L;

  String nodeName;
  String dbName;

  public DropNodeAssignmentDesc() {
  }
  public DropNodeAssignmentDesc(String nodeName, String dbName) {
    super();
    this.nodeName = nodeName;
    this.dbName = dbName;
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
