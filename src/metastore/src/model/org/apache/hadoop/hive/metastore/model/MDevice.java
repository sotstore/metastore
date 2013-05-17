package org.apache.hadoop.hive.metastore.model;

public class MDevice {
  private MNode node;
  private String dev_name;

  public MDevice(MNode node, String name) {
    this.node = node;
    this.dev_name = name;
  }

  public String getDev_name() {
    return dev_name;
  }

  public void setDev_name(String dev_name) {
    this.dev_name = dev_name;
  }

  public MNode getNode() {
    return node;
  }

  public void setNode(MNode node) {
    this.node = node;
  }
}
