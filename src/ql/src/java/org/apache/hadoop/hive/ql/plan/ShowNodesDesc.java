package org.apache.hadoop.hive.ql.plan;

import java.io.Serializable;

public class ShowNodesDesc extends DDLDesc implements Serializable {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  String resFile;

  private static final String schema = "node_name,ip,stauts#string:string:string";



  public ShowNodesDesc() {
    super();
  }





  public ShowNodesDesc(String resFile) {
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
