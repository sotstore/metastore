package org.apache.hadoop.hive.ql.plan;

import java.io.Serializable;

public class ShowDatacentersDesc extends DDLDesc implements Serializable {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  String resFile;

  private static final String schema = "data_center#string";



  public ShowDatacentersDesc() {
    super();
  }





  public ShowDatacentersDesc(String resFile) {
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
