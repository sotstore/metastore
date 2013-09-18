package org.apache.hadoop.hive.metastore.model;


public class MFileLocation {
  private MFile file;
  private MDevice dev;
  private String location;
  private int rep_id;
  private long update_time;
  private int visit_status;
  private String digest;

  public MFileLocation(MFile file, MDevice dev, String location, int rep_id,
      long update_time, int visit_status, String digest) {
    this.file = file;
    this.dev = dev;
    this.location = location;
    this.rep_id = rep_id;
    this.update_time = update_time;
    this.visit_status = visit_status;
    this.digest = digest;
  }

  public MFile getFile() {
    return file;
  }
  public void setFile(MFile file) {
    this.file = file;
  }
  public MDevice getDev() {
    return dev;
  }
  public void setDev(MDevice dev) {
    this.dev = dev;
  }
  public String getLocation() {
    return location;
  }
  public void setLocation(String location) {
    this.location = location;
  }
  public int getRep_id() {
    return rep_id;
  }
  public void setRep_id(int rep_id) {
    this.rep_id = rep_id;
  }
  public long getUpdate_time() {
    return update_time;
  }
  public void setUpdate_time(long update_time) {
    this.update_time = update_time;
  }
  public int getVisit_status() {
    return visit_status;
  }
  public void setVisit_status(int visit_status) {
    this.visit_status = visit_status;
  }
  public String getDigest() {
    return digest;
  }
  public void setDigest(String digest) {
    this.digest = digest;
  }


}
