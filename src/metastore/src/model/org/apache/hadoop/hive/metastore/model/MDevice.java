package org.apache.hadoop.hive.metastore.model;

public class MDevice {
  private MNode node;
  private String dev_name;
  private int prop;

  public MDevice(MNode node, String name, int prop) {
    this.node = node;
    this.dev_name = name;
    this.prop = prop;
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

  public int getProp() {
    return prop;
  }

  public void setProp(int prop) {
    this.prop = prop;
  }
}
