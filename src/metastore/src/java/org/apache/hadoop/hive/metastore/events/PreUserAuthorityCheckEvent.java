package org.apache.hadoop.hive.metastore.events;

import org.apache.hadoop.hive.metastore.HiveMetaStore.HMSHandler;
import org.apache.hadoop.hive.metastore.api.MSOperation;
import org.apache.hadoop.hive.metastore.api.Table;

public class PreUserAuthorityCheckEvent extends PreEventContext {

  private final Table table;
  private final MSOperation mso;


  public PreUserAuthorityCheckEvent(Table table, MSOperation mso, HMSHandler handler) {
    super(PreEventType.USER_AUTHORITY_CHECK, handler);
    this.table = table;
    this.mso = mso;
  }

  public Table getTable() {
    return table;
  }

  public MSOperation getMso() {
    return mso;
  }

}
