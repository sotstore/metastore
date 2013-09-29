package org.apache.hadoop.hive.ql.plan;

import java.io.Serializable;

/**
 * CreateUserAssignmentDesc.
 *
 */
@Explain(displayName = "Create UserAssignment")
public class AddUserAssignmentDesc extends DDLDesc implements Serializable {
  private static final long serialVersionUID = 1L;

  String dbName;
  String userName;


  public AddUserAssignmentDesc() {
  }

  public AddUserAssignmentDesc(String dbName, String userName) {
    super();
    this.dbName = dbName;
    this.userName = userName;
  }


  @Explain(displayName="UserName")
  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  @Explain(displayName="DbName")
  public String getDbName() {
    return dbName;
  }

  public void setDbName(String dbName) {
    this.dbName = dbName;
  }


}
