package org.apache.hadoop.hive.ql.metadata;

import java.io.Serializable;

public class EqRoom implements Serializable {

  private static final long serialVersionUID = 1L;
  String eqRoomName;
  String status;
  String comment;
  GeoLoc geoLoc;

  public EqRoom(){

  }

  public EqRoom(String eqRoomName) {
    super();
    this.eqRoomName = eqRoomName;
  }

  public EqRoom(String eqRoomName, String status, String comment, GeoLoc geoLoc) {
    super();
    this.eqRoomName = eqRoomName;
    this.status = status;
    this.comment = comment;
    this.geoLoc = geoLoc;
  }


  public GeoLoc getGeoLoc() {
    return geoLoc;
  }

  public void setGeoLoc(GeoLoc geoLoc) {
    this.geoLoc = geoLoc;
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
