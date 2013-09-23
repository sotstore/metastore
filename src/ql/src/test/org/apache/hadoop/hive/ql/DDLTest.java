package org.apache.hadoop.hive.ql;

import org.apache.hadoop.hive.conf.HiveConf;

public class DDLTest {

  /**
   * @param args
   */
  public static void main(String[] args) {
    // TODO Auto-generated method stub

    //String sql = "create EQROOM('aaa',qw,'abc','sd')";
//  String sql = "drop geoloc'aaa'";
    //String sql = "modify geoloc('aaa','qw','abc','sd','dsds')";
    String sql = "show geoloc";
    Driver dr = new Driver(new HiveConf());
    try {
      dr.run(sql);
    } catch (CommandNeedRetryException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    dr.compile(sql);

  }

}
