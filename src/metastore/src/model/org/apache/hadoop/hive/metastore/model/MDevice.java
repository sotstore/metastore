package org.apache.hadoop.hive.metastore.model;

public class MDevice {
  private String dev_name;

  public MDevice(String name) {
    setDev_name(name);
  }

  public String getDev_name() {
    return dev_name;
  }

  public void setDev_name(String dev_name) {
    this.dev_name = dev_name;
  }
}
