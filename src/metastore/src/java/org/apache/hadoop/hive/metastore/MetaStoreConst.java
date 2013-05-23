package org.apache.hadoop.hive.metastore;

public class MetaStoreConst {
  public class MFileStoreStatus {
    public static final int INCREATE = 0;
    public static final int CLOSED = 1;
    public static final int REPLICATED = 2;
    public static final int RM_LOGICAL = 3;
    public static final int RM_PHYSICAL = 4;
  }

  public class MFileLocationVisitStatus {
    public static final int OFFLINE = 0;
    public static final int ONLINE = 1;
  }
}
