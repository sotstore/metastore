package org.apache.hadoop.hive.ql.security;

import java.util.LinkedList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.session.SessionState;

public class CloudAuthenticator implements HiveAuthenticationProvider {

  private SessionState ss;

  private Configuration conf;

  @Override
  public Configuration getConf() {
    return conf;
  }

  @Override
  public void setConf(Configuration arg0) {
    // TODO Auto-generated method stub
    conf = arg0;
    ss = SessionState.get();//added by liulichao
  }

  @Override
  public String getUserName() {
    // TODO Auto-generated method stub
    return ss.getUser();
  }

  @Override
  public List<String> getGroupNames() {
    // TODO Auto-generated method stub
    List<String> groups = new LinkedList<String>();
    groups.add(ss.getUser());

    return groups;
  }

  @Override
  public void destroy() throws HiveException {
    // TODO Auto-generated method stub
    return;
  }

  public SessionState getSs() {
    return ss;
  }

  public void setSs(SessionState ss) {
    this.ss = ss;
  }

}