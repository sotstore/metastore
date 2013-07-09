package org.apache.hadoop.hive.metastore.model;

public class MFile {
  private long fid;
  private long placement;
  private int store_status;
  private int rep_nr;
  private String digest;
  private long record_nr;
  private long all_record_nr;
  //private List<MFileLocation> locations;
  private long length;

  public MFile(long fid, long placement, int store_status, int rep_nr, String digest,
      long record_nr, long all_record_nr, long length) {
    this.setFid(fid);
    this.placement = placement;
    this.store_status = store_status;
    this.rep_nr = rep_nr;
    this.digest = digest;
    this.record_nr = record_nr;
    this.all_record_nr = all_record_nr;
    this.setLength(length);
  }

  public int getStore_status() {
    return store_status;
  }
  public void setStore_status(int store_status) {
    this.store_status = store_status;
  }
  public long getPlacement() {
    return placement;
  }
  public void setPlacement(long placement) {
    this.placement = placement;
  }
  public int getRep_nr() {
    return rep_nr;
  }
  public void setRep_nr(int rep_nr) {
    this.rep_nr = rep_nr;
  }
  public String getDigest() {
    return digest;
  }
  public void setDigest(String digest) {
    this.digest = digest;
  }
  public long getRecord_nr() {
    return record_nr;
  }
  public void setRecord_nr(long record_nr) {
    this.record_nr = record_nr;
  }
  public long getAll_record_nr() {
    return all_record_nr;
  }
  public void setAll_record_nr(long all_record_nr) {
    this.all_record_nr = all_record_nr;
  }
  public long getFid() {
    return fid;
  }

  public void setFid(long fid) {
    this.fid = fid;
  }

  public long getLength() {
    return length;
  }

  public void setLength(long length) {
    this.length = length;
  }
}
