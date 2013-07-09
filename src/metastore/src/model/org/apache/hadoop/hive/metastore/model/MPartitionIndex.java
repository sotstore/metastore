package org.apache.hadoop.hive.metastore.model;

public class MPartitionIndex {
  private MIndex index;
  private MPartition partition;

  public MPartitionIndex(MIndex index, MPartition partition) {
    this.index = index;
    this.partition = partition;
  }

  public MIndex getIndex() {
    return index;
  }

  public void setIndex(MIndex index) {
    this.index = index;
  }

  public MPartition getPartition() {
    return partition;
  }

  public void setPartition(MPartition partition) {
    this.partition = partition;
  }
}
