package org.apache.hadoop.hive.metastore.model;

public class MGeoLocation {
  private String geoLocName;
  private String nation;
  private String province;
  private String city;
  private String dist;

  /**
   * 空参构造方法
   */
  public MGeoLocation(){}
  /**
   * @author cry
   * @param nation
   * @param province
   * @param city
   * @param dist
   */
  public MGeoLocation(String nation, String province, String city, String dist) {
    super();
    this.nation = nation;
    this.province = province;
    this.city = city;
    this.dist = dist;
  }

  /**
   * @return
   */

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
  };

}