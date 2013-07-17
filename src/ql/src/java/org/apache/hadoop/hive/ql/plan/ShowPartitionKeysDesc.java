package org.apache.hadoop.hive.ql.plan;

import java.io.Serializable;

public class ShowPartitionKeysDesc extends DDLDesc implements Serializable {

  /**
   *
   */
  private static final long serialVersionUID = 1L;


  String tabName;
  String dbName;

  String resFile;

  private static final String schema = "partition level,partition_column,order,partition_type,partition args,version#string:string:string:string:string:string";



  public ShowPartitionKeysDesc() {
    super();
  }





  public ShowPartitionKeysDesc(String resFile, String tabName, String dbName) {
    super();
    this.resFile = resFile;
    this.tabName = tabName;
    this.dbName = dbName;
  }

  public String getSchema() {
    return schema;
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
