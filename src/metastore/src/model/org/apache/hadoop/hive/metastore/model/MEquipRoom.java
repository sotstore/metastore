package org.apache.hadoop.hive.metastore.model;

import org.apache.hadoop.hive.metastore.api.GeoLocation;


public class MEquipRoom {
  private String eqRoomName;
  private int status;
  private String comment;
  private GeoLocation geolocation;

  /**
   * @author cry
   */
  public MEquipRoom() {}
  /**
   * @author cry
   * @param eqRoomName
   * @param status
   * @param
   * @param comment
   * @param geolocation
   */
  public MEquipRoom(String eqRoomName, int status, String geoLocName, String comment, GeoLocation geolocation) {
    super();
    this.eqRoomName = eqRoomName;
    this.status = MetaStoreConst.MEquipRoomStatus.SUSPECT;
    this.comment = comment;
    this.geolocation = geolocation;
  }

  public String getComment() {
    return comment;
  }
  public void setComment(String comment) {
    this.comment = comment;
  }
  public String getEqRoomName() {
    return eqRoomName;
  }
  public void setEqRoomName(String eqRoomName) {
    this.eqRoomName = eqRoomName;
  }
  public int getStatus() {
    return status;
  }
  public void setStatus(int status) {
    this.status = status;
  }
  public GeoLocation getGeolocation() {
    return geolocation;
  }
  public void setGeolocation(GeoLocation geolocation) {
    this.geolocation = geolocation;
  }

}
