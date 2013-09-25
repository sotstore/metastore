package org.apache.hadoop.hive.ql.plan;

import java.io.Serializable;


/**
 * CreateEqRoomDesc.
 *
 */
@Explain(displayName = "Create EqRoom")
public class AddEqRoomDesc extends DDLDesc implements Serializable {

  private static final long serialVersionUID = 1L;

  String eqRoomName;
  String status;
  String comment;
  String geoLocName;

  public AddEqRoomDesc() {
  }


  public AddEqRoomDesc(String eqRoomName, String status) {
    super();
    this.eqRoomName = eqRoomName;
    this.status = status;
  }

  public AddEqRoomDesc(String eqRoomName, String status, String comment, String geoLocName) {
    super();
    this.eqRoomName = eqRoomName;
    this.status = status;
    this.comment = comment;
    this.geoLocName = geoLocName;
  }


  @Explain(displayName="getGeoLocName")
  public String getGeoLocName() {
    return geoLocName;
  }

  public void setGeoLocName(String geoLocName) {
    this.geoLocName = geoLocName;
  }

  @Explain(displayName="getEqRoomName")
  public String getEqRoomName() {
    return eqRoomName;
  }

  public void setEqRoomName(String eqRoomName) {
    this.eqRoomName = eqRoomName;
  }

  @Explain(displayName="getStatus")
  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  @Explain(displayName="getComment")
  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }



}
