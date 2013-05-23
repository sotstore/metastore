package org.apache.hadoop.hive.metastore.model;


//Implemented By : liulichao
//Implementation Date : 2013-05-15
public class MUser {

  private String userName;

  private String passwd;

  private int createTime;

  private String ownerName;

  public MUser() {

  }

  public MUser(String userName, String passwd, int createTime, String ownerName) {
    super();
    this.userName = userName;
    this.passwd = passwd;
    this.createTime = createTime;
    this.ownerName = ownerName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getUserName() {
    return this.userName;
  }

  public void setPasswd(String passwd) {
    this.passwd = passwd;
  }

  public String getPasswd() {
    return this.passwd;
  }

    /**
     * @return create time
     */
    public int getCreateTime() {
      return createTime;
    }

    /**
     * @param createTime
     *          user create time
     */
    public void setCreateTime(int createTime) {
      this.createTime = createTime;
    }

    /**
     * @return the principal name who created this user
     */
    public String getOwnerName() {
      return ownerName;
    }

    public void setOwnerName(String ownerName) {
      this.ownerName = ownerName;
    }
}