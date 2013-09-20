package org.apache.hadoop.hive.ql.plan;
import java.io.Serializable;


/**
 * DropEqRoomDesc.
 *
 */
@Explain(displayName = "Drop EqRoom")
public class DropEqRoomDesc extends DDLDesc implements Serializable {

  private static final long serialVersionUID = 1L;

  String eqRoomName;
  boolean ifExists;


  public DropEqRoomDesc(String eqRoomName) {
    super();
    this.eqRoomName = eqRoomName;
  }

  public DropEqRoomDesc(String eqRoomName, boolean ifExists) {
    super();
    this.eqRoomName = eqRoomName;
    this.ifExists = ifExists;
  }

  @Explain(displayName = "EqRoom")
  public String getEqRoomName() {
    return eqRoomName;
  }

  public void setGeoLocId(String eqRoomName) {
    this.eqRoomName = eqRoomName;
  }

  @Explain(displayName = "if exists")
  public boolean getIfExists() {
    return ifExists;
  }

  public void setIfExists(boolean ifExists) {
    this.ifExists = ifExists;
  }

}
