package org.apache.hadoop.hive.metastore.model;

public class MDataCenter {
  private String name;

  // MetaStore Sevice
  private String ms_ip;
  private int ms_port;

  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }
  public String getMs_ip() {
    return ms_ip;
  }
  public void setMs_ip(String ms_ip) {
    this.ms_ip = ms_ip;
  }
  public int getMs_port() {
    return ms_port;
  }
  public void setMs_port(int ms_port) {
    this.ms_port = ms_port;
  }
}
