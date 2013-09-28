package org.apache.hadoop.hive.metastore.model;


public class MSplitValue {

  private String pkname;
  private int level;
  private String value;
  private long version;

  public MSplitValue(String pkname, int level, String value, long version) {
    this.setPkname(pkname);
    this.setLevel(level);
    this.setValue(value);
    this.setVersion(version);
  }

  public int getLevel() {
    return level;
  }

  public void setLevel(int level) {
    this.level = level;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getPkname() {
    return pkname;
  }

  public void setPkname(String pkname) {
    this.pkname = pkname;
  }

  public long getVersion() {
    return version;
  }

  public void setVersion(long version) {
    this.version = version;
  }
}
