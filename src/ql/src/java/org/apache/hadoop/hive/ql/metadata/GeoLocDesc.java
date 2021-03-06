package org.apache.hadoop.hive.ql.metadata;

import java.io.Serializable;

public class GeoLocDesc implements Serializable {

  private static final long serialVersionUID = 1L;
  String geoLocId;
  String geoLocName;
  String nation;
  String province;
  String city;
  String dist;

  public GeoLocDesc(){

  }

  public GeoLocDesc(String geoLocName) {
    super();
    this.geoLocName = geoLocName;
  }

  public GeoLocDesc(String geoLocName, String nation, String province, String city, String dist) {
    super();
    this.geoLocName = geoLocName;
    this.nation = nation;
    this.province = province;
    this.city = city;
    this.dist = dist;
  }

  public GeoLocDesc(String geoLocId, String geoLocName, String nation, String province,
      String city, String dist) {
    super();
    this.geoLocId = geoLocId;
    this.geoLocName = geoLocName;
    this.nation = nation;
    this.province = province;
    this.city = city;
    this.dist = dist;
  }

  public String getGeoLocId() {
    return geoLocId;
  }

  public void setGeoLocId(String geoLocId) {
    this.geoLocId = geoLocId;
  }

  public String getGeoLocName() {
    return geoLocName;
  }

  public void setGeoLocName(String geoLocName) {
    this.geoLocName = geoLocName;
  }

  public String getNation() {
    return nation;
  }

  public void setNation(String nation) {
    this.nation = nation;
  }

  public String getProvince() {
    return province;
  }

  public void setProvince(String province) {
    this.province = province;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getDist() {
    return dist;
  }

  public void setDist(String dist) {
    this.dist = dist;
  }


}
