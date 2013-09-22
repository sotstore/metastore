package org.apache.hadoop.hive.ql.plan;

import java.io.Serializable;


/**
 * DropGeoLocDesc.
 *
 */
@Explain(displayName = "Drop GeoLoc")
public class DropGeoLocDesc extends DDLDesc implements Serializable {

  private static final long serialVersionUID = 1L;

  String geoLocName;
  boolean ifExists;

  public DropGeoLocDesc(String geoLocName) {
    this(geoLocName,true);
  }

  public DropGeoLocDesc(String geoLocName, boolean ifExists) {
    super();
    this.geoLocName = geoLocName;
    this.ifExists = ifExists;
  }

  @Explain(displayName = "GeoLoc")
  public String getGeoLocName() {
    return geoLocName;
  }

  public void setGeoLocId(String geoLocName) {
    this.geoLocName = geoLocName;
  }

  @Explain(displayName = "if exists")
  public boolean getIfExists() {
    return ifExists;
  }

  public void setIfExists(boolean ifExists) {
    this.ifExists = ifExists;
  }


}
