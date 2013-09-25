package org.apache.hadoop.hive.ql.metadata;

import java.io.Serializable;

public class GeoLoc implements Serializable {

  private static final long serialVersionUID = 1L;
  String geoLocName;
  String nation;
  String province;
  String city;
  String dist;

  public GeoLoc(){

  }

  public GeoLoc(String geoLocName) {
    super();
    this.geoLocName = geoLocName;
  }

  public GeoLoc(String geoLocName, String nation, String province, String city, String dist) {
    super();
    this.geoLocName = geoLocName;
    this.nation = nation;
    this.province = province;
    this.city = city;
    this.dist = dist;
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
