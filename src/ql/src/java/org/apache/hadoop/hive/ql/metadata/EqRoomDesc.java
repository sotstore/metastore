package org.apache.hadoop.hive.ql.metadata;

import java.io.Serializable;

public class EqRoomDesc implements Serializable {

  private static final long serialVersionUID = 1L;
  String eqRoomId;
  String eqRoomName;
  String status;
  String geoLocName;
  String comment;

  public EqRoomDesc(){

  }

  public EqRoomDesc(String eqRoomName) {
    super();
    this.eqRoomName = eqRoomName;
  }

  public EqRoomDesc(String eqRoomName, String status, String geoLocName, String comment) {
    super();
    this.eqRoomName = eqRoomName;
    this.status = status;
    this.geoLocName = geoLocName;
    this.comment = comment;
  }


  public EqRoomDesc(String eqRoomId, String eqRoomName, String status, String geoLocName,
      String comment) {
    super();
    this.eqRoomId = eqRoomId;
    this.eqRoomName = eqRoomName;
    this.status = status;
    this.geoLocName = geoLocName;
    this.comment = comment;
  }

  public String getGeoLocName() {
    return geoLocName;
  }

  public void setGeoLocId(String geoLocName) {
    this.geoLocName = geoLocName;
  }

  public String getEqRoomName() {
    return eqRoomName;
  }

  public void setEqRoomName(String eqRoomName) {
    this.eqRoomName = eqRoomName;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }


}
