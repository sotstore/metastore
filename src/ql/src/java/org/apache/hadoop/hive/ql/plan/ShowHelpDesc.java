package org.apache.hadoop.hive.ql.plan;

import java.io.Serializable;


public class ShowHelpDesc extends DDLDesc implements Serializable {

  /**
   *
   */
  private static final long serialVersionUID = 1L;


  String resFile;

  private static String schema = "hint#string";



  public ShowHelpDesc() {
    super();
  }





  public ShowHelpDesc( String resFile) {
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





  public static void setSchema(String schema) {
    ShowHelpDesc.schema = schema;
  }




}
