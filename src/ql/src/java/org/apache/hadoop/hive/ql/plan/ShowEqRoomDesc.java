package org.apache.hadoop.hive.ql.plan;

import java.io.Serializable;

public class ShowEqRoomDesc implements Serializable {

  private static final long serialVersionUID = 1L;
  String resFile;

  private static final String schema = "eqRoomName,status,geoLocName,comment#string:string:string:string";

  public ShowEqRoomDesc() {
    super();
  }

  public ShowEqRoomDesc(String resFile) {
    super();
    this.resFile = resFile;
  }

  public String getSchema() {
    return schema;
  }


  public String getResFile() {
    return resFile;
  }


  public void setResFile(String resFile) {
    this.resFile = resFile;
  }
}
