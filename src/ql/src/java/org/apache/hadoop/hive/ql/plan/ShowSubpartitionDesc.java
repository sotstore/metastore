package org.apache.hadoop.hive.ql.plan;

import java.io.Serializable;

public class ShowSubpartitionDesc extends DDLDesc implements Serializable {

  /**
   *
   */
  private static final long serialVersionUID = 1L;


  String partName;
  String tabName;
  String dbName;

  String resFile;



  public ShowSubpartitionDesc() {
    super();
  }





  public ShowSubpartitionDesc(String resFile,String partName, String tabName, String dbName) {
    super();
    this.resFile = resFile;
    this.partName = partName;
    this.tabName = tabName;
    this.dbName = dbName;
  }



  public String getPartName() {
    return partName;
  }




  public void setPartName(String partName) {
    this.partName = partName;
  }




  public String getTabName() {
    return tabName;
  }




  public void setTabName(String tabName) {
    this.tabName = tabName;
  }





  public String getDbName() {
    return dbName;
  }





  public void setDbName(String dbName) {
    this.dbName = dbName;
  }





  public String getResFile() {
    return resFile;
  }





  public void setResFile(String resFile) {
    this.resFile = resFile;
  }




}
