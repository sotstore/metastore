package org.apache.hadoop.hive.metastore.msg;

import org.apache.hadoop.hive.metastore.msg.MSGFactory.DDLMsg;

public class MetaMsgServer {
  String ip="localhost";
  int port=2180;


  private void initalize(){

  }


  public void start(){
    initalize();

  }

  public void sendMsg(DDLMsg msg){
    switch(msg.msgType){
      case MSGType.MSG_NEW_DATABESE : break;
      //新建库
      case MSGType.MSG_ALTER_DATABESE : break;
            //修改库
      case MSGType.MSG_ALTER_DATABESE_PARAM : break;
            //修改库属性
      case MSGType.MSG_DROP_DATABESE : break;
            //删除库
      case MSGType.MSG_NEW_TALBE : break;
            //新建表
      case MSGType.MSG_ALT_TALBE_NAME : break;
            //修改表名
      case MSGType.MSG_ALT_TALBE_DISTRIBUTE : break;
            //修改表数据分布
      case MSGType.MSG_ALT_TALBE_PARTITIONING : break;
            //修改表分区方式
      case MSGType.MSG_ALT_TALBE_DEL_COL : break;
            //修改表删除列
      case MSGType.MSG_ALT_TALBE_ADD_COL : break;
            //修改表新增列
      case MSGType.MSG_ALT_TALBE_ALT_COL_NAME : break;
            //修改表修改列名
      case MSGType.MSG_ALT_TALBE_ALT_COL_TYPE : break;
            //修改表修改列类型
      case MSGType.MSG_ALT_TALBE_ALT_COL_LENGTH : break;
            //修改表修改列类型长度
      case MSGType.MSG_NEW_PARTITION : break;
            // 新建分区
      case MSGType.MSG_ALT_PARTITION : break;
            //修改分区
      case MSGType.MSG_DEL_DATABESE : break;
            // 删除分区
      case MSGType.MSG_NEW_PARTITION_FILE : break;
            //增加分区文件
      case MSGType.MSG_ALT_PARTITION_FILE : break;
            //修改分区文件
      case MSGType.MSG_REP_PARTITION_FILE_CHAGE : break;
            //分区文件副本变化
      case MSGType.MSG_STA_PARTITION_FILE_CHAGE : break;
            //分区文件状态变化
      case MSGType.MSG_REP_PARTITION_FILE_ONOFF : break;
            //分区文件副本上下线变化
      case MSGType.MSG_DEL_PARTITION_FILE : break;
            //删除分区文件
      case MSGType.MSG_NEW_INDEX : break;
            //新建列索引
      case MSGType.MSG_ALT_INDEX : break;
            //修改列索引
      case MSGType.MSG_ALT_INDEX_PARAM : break;
            //修改列索引属性
      case MSGType.MSG_DEL_INDEX : break;
            //删除列索引
      case MSGType.MSG_NEW_PARTITION_INDEX : break;
            //新建分区索引
      case MSGType.MSG_ALT_PARTITION_INDEX : break;
            //修改分区索引
      case MSGType.MSG_DEL_PARTITION_INDEX : break;
            // 删除分区索引
      case MSGType.MSG_NEW_PARTITION_INDEX_FILE : break;
            //增加分区索引文件
      case MSGType.MSG_ALT_PARTITION_INDEX_FILE : break;
            //修改分区索引文件
      case MSGType.MSG_REP_PARTITION_INDEX_FILE_CHAGE : break;
            //分区索引文件副本变化
      case MSGType.MSG_STA_PARTITION_INDEX_FILE_CHAGE : break;
            //分区索引文件状态变化
      case MSGType.MSG_REP_PARTITION_INDEX_FILE_ONOFF : break;
            //分区索引文件副本上下线变化
      case MSGType.MSG_DEL_PARTITION_INDEX_FILE : break;
            //删除分区索引文件
      case MSGType.MSG_NEW_NODE : break;
            //新增节点
      case MSGType.MSG_DEL_NODE : break;
            //删除节点
      case MSGType.MSG_FAIL_NODE : break;
            //节点故障

      case MSGType.MSG_DDL_DIRECT_DW1 : break;
        //dw1 专用DDL语句
      case MSGType.MSG_DDL_DIRECT_DW2 : break;
        //dw2 专用DDL语句
    }
  }

}
