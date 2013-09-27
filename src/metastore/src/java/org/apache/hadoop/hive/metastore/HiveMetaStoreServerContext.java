package org.apache.hadoop.hive.metastore;

import java.util.Random;

import org.apache.thrift.server.ServerContext;

public class HiveMetaStoreServerContext implements ServerContext {
  private String userName;
  private boolean isAuthenticated = false;
  private final long sessionId;

  public HiveMetaStoreServerContext() {
    Random rand = new Random(System.currentTimeMillis());
    sessionId = rand.nextLong();
  }

  public String getUserName() {
    return userName;
  }
  public void setUserName(String userName) {
    this.userName = userName;
  }
  public boolean isAuthenticated() {
    return isAuthenticated;
  }
  public void setAuthenticated(boolean isAuthenticated) {
    this.isAuthenticated = isAuthenticated;
  }
  public long getSessionId() {
    return sessionId;
  }

}
