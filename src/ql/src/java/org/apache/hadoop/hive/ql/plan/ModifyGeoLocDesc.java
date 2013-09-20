package org.apache.hadoop.hive.ql.plan;

import java.io.Serializable;


/**
 * ModifyGeoLocDesc.
 *
 */
@Explain(displayName = "Modify GeoLoc")
public class ModifyGeoLocDesc extends DDLDesc implements Serializable {

  private static final long serialVersionUID = 1L;

  String geoLocName;
  String nation;
  String province;
  String city;
  String dist;

  public ModifyGeoLocDesc() {

  }

  public ModifyGeoLocDesc(String geoLocName, String nation, String province, String city, String dist) {
    super();
    this.geoLocName = geoLocName;
    this.nation = nation;
    this.province = province;
    this.city = city;
    this.dist = dist;
  }

  @Explain(displayName="geoLocName")
  public String getGeoLocName() {
    return geoLocName;
  }


  public void setGeoLocId(String geoLocName) {
    this.geoLocName = geoLocName;
  }

  @Explain(displayName="nation")
  public String getNation() {
    return nation;
  }


  public void setNation(String nation) {
    this.nation = nation;
  }

  @Explain(displayName="province")
  public String getProvince() {
    return province;
  }


  public void setProvince(String province) {
    this.province = province;
  }

  @Explain(displayName="city")
  public String getCity() {
    return city;
  }


  public void setCity(String city) {
    this.city = city;
  }

  @Explain(displayName="dist")
  public String getDist() {
    return dist;
  }


  public void setDist(String dist) {
    this.dist = dist;
  }

}
