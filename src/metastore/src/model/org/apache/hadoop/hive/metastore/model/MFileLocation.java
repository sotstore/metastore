package org.apache.hadoop.hive.metastore.model;


public class MFileLocation {
  private MNode node;
  private MFile file;
  private String devid;
  private String location;
  private int rep_id;
  private long update_time;
  private int visit_status;
  private String digest;

  public MFileLocation(MNode node, MFile file, String devid, String location, int rep_id,
      long update_time, int visit_status, String digest) {
    this.node = node;
    this.file = file;
    this.devid = devid;
    this.location = location;
    this.rep_id = rep_id;
    this.update_time = update_time;
    this.visit_status = visit_status;
    this.digest = digest;
  }

  public MNode getNode() {
    return node;
  }
  public void setNode(MNode node) {
    this.node = node;
  }
  public MFile getFile() {
    return file;
  }
  public void setFile(MFile file) {
    this.file = file;
  }
  public String getDevid() {
    return devid;
  }
  public void setDevid(String devid) {
    this.devid = devid;
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
