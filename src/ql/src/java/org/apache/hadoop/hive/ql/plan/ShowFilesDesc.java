package org.apache.hadoop.hive.ql.plan;

import java.io.Serializable;

import org.apache.hadoop.hive.ql.metadata.Table;

public class ShowFilesDesc extends DDLDesc implements Serializable {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  Table table;
  String partName;
  String resFile;

  private static String schema = "file_id,record_num,all_record_num,length,replicas,store_status#string:string:string:string:string:string";



  public ShowFilesDesc() {
    super();
  }





  public ShowFilesDesc(String resFile) {
    super();
    this.resFile = resFile;
  }

  public ShowFilesDesc(String resFile,Table table,String partName) {
    super();
    this.partName = partName;
    this.table = table;
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





  public Table getTable() {
    return table;
  }





  public void setTable(Table table) {
    this.table = table;
  }





  public static void setSchema(String schema) {
    ShowFilesDesc.schema = schema;
  }





  public String getPartName() {
    return partName;
  }





  public void setPartName(String partName) {
    this.partName = partName;
  }




}
