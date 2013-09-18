package org.apache.hadoop.hive.ql.security;

import java.util.LinkedList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.metastore.HiveMetaStore.HMSHandler.MSSessionState;
import org.apache.hadoop.hive.ql.metadata.HiveException;

public class SOTAuthenticator implements HiveAuthenticationProvider {
  private final MSSessionState msss = new MSSessionState();

  private Configuration conf;

  @Override
  public Configuration getConf() {
    return conf;
  }

  @Override
  public void setConf(Configuration arg0) {
    conf = arg0;
  }

  @Override
  public String getUserName() {
    return msss.getUserName();
  }

  @Override
  public List<String> getGroupNames() {
    List<String> groups = new LinkedList<String>();
    groups.add(msss.getUserName());

    return groups;
  }

  @Override
  public void destroy() throws HiveException {
  }

}
