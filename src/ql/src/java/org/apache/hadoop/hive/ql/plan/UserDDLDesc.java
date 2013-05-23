/**
 * Date: 2012.10.19
 * Author: liulichao
 *
 */

package org.apache.hadoop.hive.ql.plan;

import java.io.Serializable;

import org.apache.hadoop.hive.metastore.api.PrincipalType;

@Explain(displayName = "Create User")
public class UserDDLDesc extends DDLDesc implements Serializable {

  private static final long serialVersionUID = 1L;

  private String name;

  private String passwd;

  private PrincipalType principalType;

  private boolean group;

  private UserOperation operation;

  private String resFile;

  private String userOwnerName;

  private static final String schema = "user_name#string";

  public static enum UserOperation {
    AUTH_USER("authenticate_user"), DROP_USER("drop_user"), CREATE_USER("create_user"),
    CHANGE_PWD("change_password"),
    SHOW_USER_NAME("show_user_name")
    //, SWITCH_USER("switch_user")    //added by liulichao
    ;
    private String operationName;

    private UserOperation() {
    }

    private UserOperation(String operationName) {
      this.operationName = operationName;
    }

    public String getOperationName() {
      return operationName;
    }

    @Override
    public String toString () {
      return this.operationName;
    }
  }

  public UserDDLDesc(){
  }

  public UserDDLDesc(String userName, UserOperation operation) {
    this(userName, null, null, PrincipalType.USER, operation);
  }

  public UserDDLDesc(String userName, String passwd, UserOperation operation) {
    this(userName, passwd, null, PrincipalType.USER, operation);
  }

  public UserDDLDesc(String userName, UserOperation operation, String resFile) {
    this.name = userName;
    this.operation = operation;
    this.resFile = resFile;
  }

  public UserDDLDesc(String userName, String passwd, String ownerName, UserOperation operation) {
    this(userName, passwd, ownerName, PrincipalType.USER, operation);
  }

  public UserDDLDesc(String principalName, String passwd, String ownerName, PrincipalType principalType,
		  UserOperation operation) {
    this.name = principalName;
    this.passwd = passwd;
    this.principalType = principalType;
    this.operation = operation;
    this.userOwnerName = ownerName;
  }

  @Explain(displayName = "name")
  public String getName() {
    return name;
  }

  public void setName(String userName) {
    this.name = userName;
  }

  @Explain(displayName = "name")
  public String getPasswd() {
    return passwd;
  }

  public void setPasswd(String passwd) {
    this.passwd = passwd;
  }

  @Explain(displayName = "user operation")
  public UserOperation getOperation() {
    return operation;
  }

  public void setOperation(UserOperation operation) {
    this.operation = operation;
  }

  public PrincipalType getPrincipalType() {
    return principalType;
  }

  public void setPrincipalType(PrincipalType principalType) {
    this.principalType = principalType;
  }

  public boolean getGroup() {
    return group;
  }

  public void setGroup(boolean group) {
    this.group = group;
  }

  public String getResFile() {
    return resFile;
  }

  public void setResFile(String resFile) {
    this.resFile = resFile;
  }

  public String getUserOwnerName() {
    return userOwnerName;
  }

  public void setUserOwnerName(String userOwnerName) {
    this.userOwnerName = userOwnerName;
  }

  public String getSchema() {
    return schema;
  }

}