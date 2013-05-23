package org.apache.hadoop.hive.metastore.model;

public class MPartitionIndexStore {
  private MPartitionIndex pi;
  private MFile indexFile;
  private long originFid;

  public MPartitionIndexStore(MPartitionIndex pi, MFile indexFile, long originFid) {
    this.setPi(pi);
    this.setIndexFile(indexFile);
    this.originFid = originFid;
  }

  public MPartitionIndex getPi() {
    return pi;
  }

  public void setPi(MPartitionIndex pi) {
    this.pi = pi;
  }

  public MFile getIndexFile() {
    return indexFile;
  }

  public void setIndexFile(MFile indexFile) {
    this.indexFile = indexFile;
  }

  public long getOriginFid() {
    return originFid;
  }

  public void setOriginFid(long originFid) {
    this.originFid = originFid;
  }

}
