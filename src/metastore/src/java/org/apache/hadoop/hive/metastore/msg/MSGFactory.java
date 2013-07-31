package org.apache.hadoop.hive.metastore.msg;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jdo.PersistenceManager;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hive.metastore.model.MDatabase;
import org.apache.hadoop.hive.metastore.model.MDirectDDL;
import org.apache.hadoop.hive.metastore.model.MFile;
import org.apache.hadoop.hive.metastore.model.MIndex;
import org.apache.hadoop.hive.metastore.model.MNode;
import org.apache.hadoop.hive.metastore.model.MPartition;
import org.apache.hadoop.hive.metastore.model.MPartitionIndex;
import org.apache.hadoop.hive.metastore.model.MTable;

public class MSGFactory {
  private static final Log LOG = LogFactory.getLog(MSGFactory.class.getName());
  private static long max_msg_id = 0;

  public static class DDLMsg{
     private long event_id;//事件类型
     private long object_id;
     private HashMap<String,Object> msg_data;//事件内容（可能为空）
     private Object eventObject;

     private long msg_id;//     消息ID
     private long db_id;//库ID
     private long node_id;//节点ID
     private long event_time;//事件发生事件
     private String event_handler = "";//事件处理函数（可能为空）
     private HashMap<String,Object> old_object_params;//对于修改操作，提供修改前对象的参数



     public DDLMsg(){}

     public DDLMsg(long event_id, long object_id,  HashMap<String,Object> msg_data, Object eventObject, long msg_id,
        long db_id, long node_id, long event_time, String event_handler,HashMap<String,Object> old_object_params) {
      super();
      this.event_id = event_id;
      this.object_id = object_id;
      this.msg_data = msg_data;
      this.eventObject = eventObject;
      this.msg_id = msg_id;
      this.db_id = db_id;
      this.node_id = node_id;
      this.event_time = event_time;
      if(event_handler == null){
        this.event_handler = "";
      }else{
        this.event_handler = event_handler;
      }
      this.old_object_params = old_object_params;
    }


    public long getObject_id() {
      return object_id;
    }
    public void setObject_id(long object_id) {
      this.object_id = object_id;
    }
    public HashMap<String,Object> getMsg_data() {
      return msg_data;
    }
    public void setMsg_data( HashMap<String,Object> msg_data) {
      this.msg_data = msg_data;
    }
    public Object getEventObject() {
      return eventObject;
    }
    public void setEventObject(Object eventObject) {
      this.eventObject = eventObject;
    }
    public long getMsg_id() {
      return msg_id;
    }
    public void setMsg_id(long msg_id) {
      this.msg_id = msg_id;
    }
    public long getDb_id() {
      return db_id;
    }
    public void setDb_id(long db_id) {
      this.db_id = db_id;
    }
    public long getNode_id() {
      return node_id;
    }
    public void setNode_id(long node_id) {
      this.node_id = node_id;
    }
    public long getEvent_id() {
      return event_id;
    }
    public void setEvent_id(long event_id) {
      this.event_id = event_id;
    }
    public long getEvent_time() {
      return event_time;
    }
    public void setEvent_time(long event_time) {
      this.event_time = event_time;
    }
    public String getEvent_handler() {
      return event_handler;
    }
    public void setEvent_handler(String event_handler) {
      this.event_handler = event_handler;
    }

    public HashMap<String,Object> getOld_object_params() {
      return old_object_params;
    }

    public void setOld_object_params(HashMap<String,Object> old_object_params) {
      this.old_object_params = old_object_params;
    }


    public String toJson(){
      JSONObject jsonObject = new JSONObject();
      jsonObject.put("event_id", event_id);
      jsonObject.put("object_id", object_id);
      jsonObject.put("msg_data", parserMapToJson(msg_data));
//      jsonObject.put("eventObject", eventObject);
      jsonObject.put("msg_id", msg_id);
      jsonObject.put("db_id", db_id);
      jsonObject.put("node_id", node_id);
      jsonObject.put("event_time", event_time);
      jsonObject.put("event_handler", event_handler);



      String jsonData = jsonObject.toString();
      LOG.warn("---zjw--json:"+jsonData);

      return jsonData;
    }


    public static DDLMsg fromJson(String jsonData){
      JSONObject json = (JSONObject)JSONSerializer.toJSON(jsonData);

      DDLMsg msg = new DDLMsg();
      msg.event_id = Long.parseLong(json.getString("event_id"));
      msg.object_id = Long.parseLong(json.getString("object_id"));
//      msg.msg_data = json.getString("msg_data");
      msg.msg_data = (HashMap<String,Object>)parserJsonToMap(json.getString("msg_data"));
      msg.msg_id = Long.parseLong(json.getString("msg_id"));
      msg.db_id = Long.parseLong(json.getString("db_id"));
      msg.node_id = Long.parseLong(json.getString("node_id"));
      msg.event_time = Long.parseLong(json.getString("event_time"));
      msg.event_handler = json.getString("event_handler");

      return msg;

    }
  }

  public static String parserMapToJson(Map<String,Object> map){
    JSONObject json = new JSONObject();
    if(map != null){
      for(Entry<String,Object> arg : map.entrySet()){
        json.put(arg.getKey(),arg.getValue());
      }
    }
    return json.toString();
  }


  public static Map<String, Object> parserJsonToMap(String json) {
    Map<String, Object> map = new HashMap<String, Object>();
//    Map<String, String> map = new HashMap<String, String>();
    JSONObject jsonObject = JSONObject.fromObject(json);
    Iterator<String> keys = jsonObject.keys();
    while (keys.hasNext()) {
        String key = (String) keys.next();
        String value = jsonObject.get(key).toString();
        if (value.startsWith("{") && value.endsWith("}")) {
            map.put(key, parserJsonToMap(value));
        } else if (value.startsWith("[") && value.endsWith("]")) {
          map.put(key, JSONArray.toList(JSONArray.fromObject(value),Long.class));
        } else{
            map.put(key, value);
        }
    }
    return map;
}



/**
 * todo:按照要求，传送给后端的类字符串应该是同数据库字段是一一对应的
 * @param msgType
 * @param object
 * @param eventObject
 * @return
 */

   public static String  getIDFromJdoObjectId(String objectId){
    if(objectId == null || objectId.equals("null") || objectId.equals("")){
      return "";
    }
    else{
      return objectId.split("\\[")[0];
    }
  }

   public static DDLMsg generateDDLMsg(long event_id,long db_id,long node_id ,PersistenceManager pm , Object eventObject,HashMap<String,Object> old_object_params){

     String jsonData;
     Long id = -1l;
     if(eventObject instanceof Long){
       id = (Long)eventObject;
     }else{
       Object objectId = pm.getObjectId(eventObject);
       LOG.info("Sending DDL message:"+event_id+"---"+objectId.toString());
       try{
         id = Long.parseLong(getIDFromJdoObjectId(objectId.toString()));
       }catch(Exception e){

       }
     }
     long now = new Date().getTime()/1000;

//     net.sf.json.JSONObject jsonObject = net.sf.json.JSONObject.fromObject(eventObject);
//     jsonData = jsonObject.toString();
//     LOG.info("---zjw--json:"+jsonData);
     return new MSGFactory.DDLMsg(event_id, id, null, eventObject, max_msg_id++, db_id, node_id, now, null,old_object_params);
   }

   public static List<DDLMsg> generateDDLMsgs(int event_id ,long db_id,long node_id,PersistenceManager pm ,List<Long> eventObjects,HashMap<String,Object> old_object_params){

     List<DDLMsg> msgs = new ArrayList<DDLMsg>();
     long now = new Date().getTime()/1000;
     Long id = 1l;
     for(Object  eventObject : eventObjects){
       String jsonData;
       if(eventObject instanceof Long){
         id = (Long)eventObject;
       }else{
         Object objectId = pm.getObjectId(eventObject);

         LOG.warn("Sending DDL message:"+event_id+"---"+objectId.toString());

         try{
           id = Long.parseLong(getIDFromJdoObjectId(objectId.toString()));
         }catch(Exception e){

         }
       }

//       net.sf.json.JSONObject jsonObject = net.sf.json.JSONObject.fromObject(eventObject);
//       jsonData = jsonObject.toString();
//       LOG.warn("---zjw--json:"+jsonData);
       msgs.add(new MSGFactory.DDLMsg(event_id, id, null, eventObject, max_msg_id++, db_id, node_id, now, null,old_object_params));
     }
     return msgs;
   }

  public static List<DDLMsg> generateDDLMsgs(long event_id ,long db_id,long node_id,PersistenceManager pm ,List<Object> eventObjects,HashMap<String,Object> old_object_params){

    List<DDLMsg> msgs = new ArrayList<DDLMsg>();
    long now = new Date().getTime()/1000;
    Long id = 1l;
    for(Object  eventObject : eventObjects){
      String jsonData;
      if(eventObject instanceof Long){
        id = (Long)eventObject;
      }else{
        Object objectId = pm.getObjectId(eventObject);

        LOG.warn("Sending DDL message:"+event_id+"---"+objectId.toString());

        try{
          id = Long.parseLong(getIDFromJdoObjectId(objectId.toString()));
        }catch(Exception e){

        }
      }

//      net.sf.json.JSONObject jsonObject = net.sf.json.JSONObject.fromObject(eventObject);
//      jsonData = jsonObject.toString();
//      LOG.warn("---zjw--json:"+jsonData);
      msgs.add(new MSGFactory.DDLMsg(event_id, id, null, eventObject, max_msg_id++, db_id, node_id, now, null,old_object_params));
    }
    return msgs;
  }



  public static String getMsgData(DDLMsg msg) {

    HashMap<String,Object> params = new HashMap<String,Object>();


    switch((int)msg.getEvent_id()){
      case MSGType.MSG_NEW_DATABESE :
      //新建库
            MDatabase db = (MDatabase)msg.getEventObject();
            params.put("datacenter_name",db.getDatacenter() == null ? "null":db.getDatacenter().getName());
            params.put("db_name",db.getName());

            break;
      case MSGType.MSG_ALTER_DATABESE :
            //修改库
          MDatabase alt_db = (MDatabase)msg.getEventObject();
          params.put("datacenter_name",alt_db.getDatacenter().getName());
          params.put("db_name",alt_db.getName());
          if(msg.getOld_object_params().containsKey("old_db_name")){
            params.put("old_db_name",msg.getOld_object_params().get("old_db_name"));
          }
          break;
      case MSGType.MSG_ALTER_DATABESE_PARAM :
            //修改库属性
          MDatabase alt_param_db = (MDatabase)msg.getEventObject();
          params.put("datacenter_name",alt_param_db.getDatacenter().getName());
          params.put("db_name",alt_param_db.getName());
          if(msg.getOld_object_params().containsKey("param_name")){
            params.put("param_name",msg.getOld_object_params().get("param_name"));
          }
          break;
      case MSGType.MSG_DROP_DATABESE :
            //删除库
          MDatabase drop_db = (MDatabase)msg.getEventObject();
          params.put("datacenter_name",drop_db.getDatacenter().getName());
          params.put("db_name",drop_db.getName());

          break;
      case MSGType.MSG_NEW_TALBE :
            //新建表
          MTable tbl = (MTable)msg.getEventObject();
          params.put("db_name",tbl.getDatabase().getName());
          params.put("table_name",tbl.getTableName());
          break;
      case MSGType.MSG_ALT_TALBE_NAME :
            //修改表名
          MTable alt_tbl = (MTable)msg.getEventObject();
          params.put("db_name",alt_tbl.getDatabase().getName());
          params.put("table_name",alt_tbl.getTableName());
          if(msg.getOld_object_params().containsKey("old_table_name")){
            params.put("old_table_name",msg.getOld_object_params().get("old_table_name"));
          }
          break;
      case MSGType.MSG_ALT_TALBE_DISTRIBUTE :
            //修改表数据分布
        /**
         * 目前，表的分布方式有两种，全分布和全复制
         */
          MTable alt_distribute_tbl = (MTable)msg.getEventObject();
          params.put("db_name",alt_distribute_tbl.getDatabase().getName());
          params.put("table_name",alt_distribute_tbl.getTableName());
          if(msg.getOld_object_params().containsKey("table_distribute")){
            params.put("table_distribute",msg.getOld_object_params().get("table_distribute"));
          }
          if(msg.getOld_object_params().containsKey("old_table_distribute")){
            params.put("old_table_distribute",msg.getOld_object_params().get("old_table_distribute"));
          }
          break;
      case MSGType.MSG_ALT_TALBE_PARTITIONING :
            //修改表分区方式
          MTable alt_partitioning_tbl = (MTable)msg.getEventObject();
          params.put("db_name",alt_partitioning_tbl.getDatabase().getName());
          params.put("table_name",alt_partitioning_tbl.getTableName());
          if(msg.getOld_object_params().containsKey("p_version")){
            params.put("p_version",msg.getOld_object_params().get("old_table_name"));
          }
          break;
      case MSGType.MSG_ALT_TALBE_DEL_COL :
            //修改表删除列
          MTable del_col_tbl = (MTable)msg.getEventObject();
          params.put("db_name",del_col_tbl.getDatabase().getName());
          params.put("table_name",del_col_tbl.getTableName());
          if(msg.getOld_object_params().containsKey("column_name")){
            params.put("column_name",msg.getOld_object_params().get("column_name"));
          }
          break;
      case MSGType.MSG_ALT_TALBE_ADD_COL :
            //修改表新增列
          MTable add_col_tbl = (MTable)msg.getEventObject();
          params.put("db_name",add_col_tbl.getDatabase().getName());
          params.put("table_name",add_col_tbl.getTableName());
          if(msg.getOld_object_params().containsKey("column_name")){
            params.put("column_name",msg.getOld_object_params().get("column_name"));
          }
          break;
      case MSGType.MSG_ALT_TALBE_ALT_COL_NAME :
            //修改表修改列名
          MTable alt_col_tbl = (MTable)msg.getEventObject();
          params.put("db_name",alt_col_tbl.getDatabase().getName());
          params.put("table_name",alt_col_tbl.getTableName());
          if(msg.getOld_object_params().containsKey("column_name")){
            params.put("column_name",msg.getOld_object_params().get("column_name"));
          }
          if(msg.getOld_object_params().containsKey("old_column_name")){
            params.put("old_column_name",msg.getOld_object_params().get("old_column_name"));
          }
          break;
      case MSGType.MSG_ALT_TALBE_ALT_COL_TYPE :
            //修改表修改列类型
          MTable alt_col_type_tbl = (MTable)msg.getEventObject();
          params.put("db_name",alt_col_type_tbl.getDatabase().getName());
          params.put("table_name",alt_col_type_tbl.getTableName());
          if(msg.getOld_object_params().containsKey("column_type")){
            params.put("column_type",msg.getOld_object_params().get("column_type"));
          }
          if(msg.getOld_object_params().containsKey("old_column_type")){
            params.put("old_column_type",msg.getOld_object_params().get("old_column_type"));
          }
      break;
      case MSGType.MSG_ALT_TALBE_ALT_COL_LENGTH : break;
            //修改表修改列类型长度,
            //注意：本事件不会触发！！！

      case MSGType.MSG_ALT_TABLE_PARAM:
            // 修改表参数
        if (msg.getOld_object_params().containsKey("db_name")) {
          params.put("db_name", msg.getOld_object_params().get("db_name"));
        }
        if (msg.getOld_object_params().containsKey("table_name")) {
          params.put("table_name", msg.getOld_object_params().get("table_name"));
        }
        if (msg.getOld_object_params().containsKey("tbl_param_keys")) {
          params.put("tbl_param_keys", msg.getOld_object_params().get("tbl_param_keys"));
        }
        break;
      case MSGType.MSG_NEW_PARTITION :
            // 新建分区
          MPartition p = (MPartition)msg.getEventObject();
          params.put("db_name", msg.getOld_object_params().get("db_name"));
          params.put("table_name", msg.getOld_object_params().get("table_name"));
          params.put("partition_name", msg.getOld_object_params().get("partition_name"));
          if(p.getPartition_level() ==2){
            Long.parseLong(getIDFromJdoObjectId(p.getParent().toString()));
            params.put("parent_partition_name", p.getPartitionName());
          }
          params.put("partition_level", p.getPartition_level());
          break;
      case MSGType.MSG_ALT_PARTITION :
            //修改分区
          MPartition alt_part = (MPartition)msg.getEventObject();
          params.put("db_name",alt_part.getTable().getDatabase().getName());
          params.put("table_name",alt_part.getTable().getTableName());
          params.put("partition_name", alt_part.getPartitionName());
          if(alt_part.getPartition_level() ==2){
            Long.parseLong(getIDFromJdoObjectId(alt_part.getParent().toString()));
            params.put("parent_partition_name", alt_part.getPartitionName());
          }
          params.put("partition_level", alt_part.getPartition_level());
          if(msg.getOld_object_params().containsKey("old_partition_name")){
            params.put("old_partition_name",msg.getOld_object_params().get("old_partition_name"));
          }
          break;
      case MSGType.MSG_DEL_PARTITION :
            // 删除分区
          MPartition del_part = (MPartition)msg.getEventObject();
          params.put("db_name",del_part.getTable().getDatabase().getName());
          params.put("table_name",del_part.getTable().getTableName());
          params.put("partition_name", del_part.getPartitionName());
          if(del_part.getPartition_level() ==2){
            Long.parseLong(getIDFromJdoObjectId(del_part.getParent().toString()));
            params.put("parent_partition_name", del_part.getPartitionName());
          }
          params.put("partition_level", del_part.getPartition_level());
          break;
      case MSGType.MSG_NEW_PARTITION_FILE :
            //增加分区文件
//          MFile file = (MFile)msg.getEventObject();
//          params.put("f_id",file.getFid());
          if(msg.getOld_object_params().containsKey("f_id")){
            params.put("f_id",msg.getOld_object_params().get("f_id"));
          }
          if(msg.getOld_object_params().containsKey("partition_name")){
            params.put("partition_name",msg.getOld_object_params().get("partition_name"));
          }
          if(msg.getOld_object_params().containsKey("parent_partition_name")){
            params.put("parent_partition_name",msg.getOld_object_params().get("parent_partition_name"));
          }
          if(msg.getOld_object_params().containsKey("partition_level")){
            params.put("partition_level",msg.getOld_object_params().get("partition_level"));
          }
          if(msg.getOld_object_params().containsKey("db_name")){
            params.put("db_name",msg.getOld_object_params().get("db_name"));
          }
          if(msg.getOld_object_params().containsKey("table_name")){
            params.put("table_name",msg.getOld_object_params().get("table_name"));
          }
          break;
      case MSGType.MSG_ALT_PARTITION_FILE :
            //修改分区文件
        if(msg.getOld_object_params().containsKey("f_id")){
          params.put("f_id",msg.getOld_object_params().get("f_id"));
        }
        if(msg.getOld_object_params().containsKey("partition_name")){
          params.put("partition_name",msg.getOld_object_params().get("partition_name"));
        }
        if(msg.getOld_object_params().containsKey("partition_level")){
          params.put("partition_level",msg.getOld_object_params().get("partition_level"));
        }
        if(msg.getOld_object_params().containsKey("parent_partition_name")){
          params.put("parent_partition_name",msg.getOld_object_params().get("parent_partition_name"));
        }
        if(msg.getOld_object_params().containsKey("db_name")){
          params.put("db_name",msg.getOld_object_params().get("db_name"));
        }
        if(msg.getOld_object_params().containsKey("table_name")){
          params.put("table_name",msg.getOld_object_params().get("table_name"));
        }
          break;
      case MSGType.MSG_REP_PARTITION_FILE_CHAGE :
            //分区文件副本变化
        if(msg.getOld_object_params().containsKey("f_id")){
          params.put("f_id",msg.getOld_object_params().get("f_id"));
        }
        if(msg.getOld_object_params().containsKey("partition_name")){
          params.put("partition_name",msg.getOld_object_params().get("partition_name"));
        }
        if(msg.getOld_object_params().containsKey("partition_level")){
          params.put("partition_level",msg.getOld_object_params().get("partition_level"));
        }
        if(msg.getOld_object_params().containsKey("parent_partition_name")){
          params.put("parent_partition_name",msg.getOld_object_params().get("parent_partition_name"));
        }
        if(msg.getOld_object_params().containsKey("parent_partition_name")){
          params.put("parent_partition_name",msg.getOld_object_params().get("parent_partition_name"));
        }
        if(msg.getOld_object_params().containsKey("db_name")){
          params.put("db_name",msg.getOld_object_params().get("db_name"));
        }
        if(msg.getOld_object_params().containsKey("table_name")){
          params.put("table_name",msg.getOld_object_params().get("table_name"));
        }
        if (msg.getOld_object_params().containsKey("fid")) {
          params.put("fid", msg.getOld_object_params().get("fid"));
        }
        if (msg.getOld_object_params().containsKey("new_repnr")) {
          params.put("new_repnr", msg.getOld_object_params().get("new_repnr"));
        }
          break;
      case MSGType.MSG_STA_PARTITION_FILE_CHAGE :
            //分区文件状态变化
        if(msg.getOld_object_params().containsKey("f_id")){
          params.put("f_id",msg.getOld_object_params().get("f_id"));
        }
        if(msg.getOld_object_params().containsKey("partition_name")){
          params.put("partition_name",msg.getOld_object_params().get("partition_name"));
        }
        if(msg.getOld_object_params().containsKey("partition_level")){
          params.put("partition_level",msg.getOld_object_params().get("partition_level"));
        }
        if(msg.getOld_object_params().containsKey("parent_partition_name")){
          params.put("parent_partition_name",msg.getOld_object_params().get("parent_partition_name"));
        }
        if(msg.getOld_object_params().containsKey("db_name")){
          params.put("db_name",msg.getOld_object_params().get("db_name"));
        }
        if(msg.getOld_object_params().containsKey("table_name")){
          params.put("table_name",msg.getOld_object_params().get("table_name"));
        }
        if (msg.getOld_object_params().containsKey("fid")) {
          params.put("fid", msg.getOld_object_params().get("fid"));
        }
        if (msg.getOld_object_params().containsKey("new_status")) {
          params.put("new_status", msg.getOld_object_params().get("new_status"));
        }
          break;
      case MSGType.MSG_REP_PARTITION_FILE_ONOFF :
            //分区文件副本上下线变化
          if(msg.getOld_object_params().containsKey("f_id")){
            params.put("f_id",msg.getOld_object_params().get("f_id"));
          }
          if(msg.getOld_object_params().containsKey("partition_name")){
            params.put("partition_name",msg.getOld_object_params().get("partition_name"));
          }
          if(msg.getOld_object_params().containsKey("partition_level")){
            params.put("partition_level",msg.getOld_object_params().get("partition_level"));
          }
          if(msg.getOld_object_params().containsKey("parent_partition_name")){
            params.put("parent_partition_name",msg.getOld_object_params().get("parent_partition_name"));
          }
          if(msg.getOld_object_params().containsKey("db_name")){
            params.put("db_name",msg.getOld_object_params().get("db_name"));
          }
          if(msg.getOld_object_params().containsKey("table_name")){
            params.put("table_name",msg.getOld_object_params().get("table_name"));
          }
          if (msg.getOld_object_params().containsKey("fid")) {
            params.put("fid", msg.getOld_object_params().get("fid"));
          }
          if (msg.getOld_object_params().containsKey("new_status")) {
            params.put("new_status", msg.getOld_object_params().get("new_status"));
          }
          break;
      case MSGType.MSG_DEL_PARTITION_FILE :
            //删除分区文件
          if(msg.getOld_object_params().containsKey("f_id")){
            params.put("f_id",msg.getOld_object_params().get("f_id"));
          }
          if(msg.getOld_object_params().containsKey("partition_name")){
            params.put("partition_name",msg.getOld_object_params().get("partition_name"));
          }
          if(msg.getOld_object_params().containsKey("partition_level")){
            params.put("partition_level",msg.getOld_object_params().get("partition_level"));
          }
          if(msg.getOld_object_params().containsKey("parent_partition_name")){
            params.put("parent_partition_name",msg.getOld_object_params().get("parent_partition_name"));
          }
          if(msg.getOld_object_params().containsKey("db_name")){
            params.put("db_name",msg.getOld_object_params().get("db_name"));
          }
          if(msg.getOld_object_params().containsKey("table_name")){
            params.put("table_name",msg.getOld_object_params().get("table_name"));
          }
          break;
      case MSGType.MSG_NEW_INDEX :
            //新建列索引
          MIndex index = (MIndex)msg.getEventObject();
          params.put("db_name",index.getOrigTable().getDatabase().getName());
          params.put("index_name",index.getIndexName());

          break;
      case MSGType.MSG_ALT_INDEX :
            //修改列索引
          MIndex alt_index = (MIndex)msg.getEventObject();
          params.put("db_name",alt_index.getOrigTable().getDatabase().getName());
          params.put("index_name",alt_index.getIndexName());
      case MSGType.MSG_ALT_INDEX_PARAM :
            //修改列索引属性
          MIndex alt_param_index = (MIndex)msg.getEventObject();
          params.put("db_name",alt_param_index.getOrigTable().getDatabase().getName());
          params.put("index_name",alt_param_index.getIndexName());
          if(msg.getOld_object_params().containsKey("param_name")){
            params.put("param_name",msg.getOld_object_params().get("param_name"));
          }
      case MSGType.MSG_DEL_INDEX : break;
            //删除列索引
      case MSGType.MSG_NEW_PARTITION_INDEX :
            //新建分区索引
          MPartitionIndex part_idx = (MPartitionIndex)msg.getEventObject();
          params.put("db_name",part_idx.getPartition().getTable().getDatabase().getName());
          params.put("table_name",part_idx.getPartition().getTable().getTableName());
          params.put("partition_name",part_idx.getPartition().getPartitionName());
          params.put("index_name",part_idx.getIndex().getIndexName());
      case MSGType.MSG_ALT_PARTITION_INDEX :
            //修改分区索引
          MPartitionIndex alt_part_idx = (MPartitionIndex)msg.getEventObject();
          params.put("db_name",alt_part_idx.getPartition().getTable().getDatabase().getName());
          params.put("table_name",alt_part_idx.getPartition().getTable().getTableName());
          params.put("partition_name",alt_part_idx.getPartition().getPartitionName());
          params.put("index_name",alt_part_idx.getIndex().getIndexName());
      case MSGType.MSG_DEL_PARTITION_INDEX :
            // 删除分区索引
          MPartitionIndex del_part_idx = (MPartitionIndex)msg.getEventObject();
          params.put("db_name",del_part_idx.getPartition().getTable().getDatabase().getName());
          params.put("table_name",del_part_idx.getPartition().getTable().getTableName());
          params.put("partition_name",del_part_idx.getPartition().getPartitionName());
          params.put("index_name",del_part_idx.getIndex().getIndexName());
      case MSGType.MSG_NEW_PARTITION_INDEX_FILE :
            //增加分区索引文件
          MFile idx_file = (MFile)msg.getEventObject();
          params.put("file_id",idx_file.getFid());
          if(msg.getOld_object_params().containsKey("part_index_store_id")){
            params.put("part_index_store_id",msg.getOld_object_params().get("part_index_store_id"));
          }
          break;
      case MSGType.MSG_ALT_PARTITION_INDEX_FILE :
            //修改分区索引文件
          MFile alt_idx_file = (MFile)msg.getEventObject();
          params.put("file_id",alt_idx_file.getFid());
          if(msg.getOld_object_params().containsKey("part_index_store_id")){
            params.put("part_index_store_id",msg.getOld_object_params().get("part_index_store_id"));
          }
          break;
      case MSGType.MSG_REP_PARTITION_INDEX_FILE_CHAGE :
            //分区索引文件副本变化
          MFile idx_file_rep = (MFile)msg.getEventObject();
          params.put("file_id",idx_file_rep.getFid());
          if(msg.getOld_object_params().containsKey("part_index_store_id")){
            params.put("part_index_store_id",msg.getOld_object_params().get("part_index_store_id"));
          }
          break;
      case MSGType.MSG_STA_PARTITION_INDEX_FILE_CHAGE :
            //分区索引文件状态变化
          MFile stat_idx_file = (MFile)msg.getEventObject();
          params.put("file_id",stat_idx_file.getFid());
          if(msg.getOld_object_params().containsKey("part_index_store_id")){
            params.put("part_index_store_id",msg.getOld_object_params().get("part_index_store_id"));
          }
          break;
      case MSGType.MSG_REP_PARTITION_INDEX_FILE_ONOFF :
            //分区索引文件副本上下线变化
          MFile onoff_idx_file = (MFile)msg.getEventObject();
          params.put("file_id",onoff_idx_file.getFid());
          if(msg.getOld_object_params().containsKey("part_index_store_id")){
            params.put("part_index_store_id",msg.getOld_object_params().get("part_index_store_id"));
          }
          break;
      case MSGType.MSG_DEL_PARTITION_INDEX_FILE :
            //删除分区索引文件
          MFile del_idx_file = (MFile)msg.getEventObject();
          params.put("file_id",del_idx_file.getFid());
          if(msg.getOld_object_params().containsKey("part_index_store_id")){
            params.put("part_index_store_id",msg.getOld_object_params().get("part_index_store_id"));
          }
          break;
      case MSGType.MSG_NEW_NODE :
            //新增节点
          MNode node = (MNode)msg.getEventObject();
          params.put("node_name",node.getNode_name());
          break;
      case MSGType.MSG_DEL_NODE :
            //删除节点
          MNode del_node = (MNode)msg.getEventObject();
          params.put("node_name",del_node.getNode_name());
          break;
      case MSGType.MSG_FAIL_NODE :
            //节点故障
          MNode fail_node = (MNode)msg.getEventObject();
          params.put("node_name",fail_node.getNode_name());
          break;
      case MSGType.MSG_DDL_DIRECT_DW1 :
        //dw1 专用DDL语句
          MDirectDDL direct_ddl1 = (MDirectDDL)msg.getEventObject();
          params.put("sql",direct_ddl1.getSql());
          break;
      case MSGType.MSG_DDL_DIRECT_DW2 :
        //dw2 专用DDL语句
          MDirectDDL direct_ddl2 = (MDirectDDL)msg.getEventObject();
          params.put("sql",direct_ddl2.getSql());
          break;
    }//end of switch

//    String jsonData = params.toString();
    msg.setMsg_data(params);
    return msg.toJson();
  }

  public static void main(String args[]){
//    Map<String,Object > map = new HashMap<String ,Object>();
//    map.put("a", 1);
//    map.put("b", "haah");
//    String s = MSGFactory.parserMapToJson(map);
//    System.out.println(s);
//    Map r = MSGFactory.parserJsonToMap(s);
//    System.out.println(s);

    DDLMsg msg = new DDLMsg();
    System.out.println(msg.toJson());

    String js="{\"event_id\":1304,\"object_id\":3962," +
    		"\"msg_data\":{\"partition_level\":2,\"f_id\":[3958,3962,3966,3973]," +
    		"\"table_name\":\"bf_dxx\",\"db_name\":\"default\"," +
    		"\"partition_name\":\"interval_2013-07-09_16:00:00_p0\"}," +
    		"\"msg_id\":49,\"db_id\":-1,\"node_id\":-1,\"event_time\":1373887036," +
    		"\"event_handler\":\"\"}";

     System.out.println("==="+js);
     msg = msg.fromJson(js);
     List<Long> ob = (List<Long>)msg.msg_data.get("f_id");
         System.out.println(msg.toJson());
         Long l = ob.get(0);
         System.out.println(ob.get(0).toString());
  }

}
