package org.apache.hadoop.hive.metastore.msg;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MSGFactory {
  private static final Log LOG = LogFactory.getLog(MSGFactory.class.getName());

  public static class DDLMsg{
     long msgType;
     long objectId;
     String jsonData;

     public DDLMsg(){}
     public DDLMsg(long msgType,long objectId,String jsonData){
       this.msgType = msgType;
       this.objectId = objectId;
       this.jsonData = jsonData;
     }
     public String toJson(){
           String json = "";

           return json;
     }
  }
/**
 * todo:按照要求，传送给后端的类字符串应该是同数据库字段是一一对应的
 * @param msgType
 * @param object
 * @param eventObject
 * @return
 */

   private static String  getIDFromJdoObjectId(String objectId){
    if(objectId == null || objectId.equals("null") || objectId.equals("")){
      return "";
    }
    else{
      return objectId.split("[")[0];
    }
  }

  public static DDLMsg generateDDLMsg(long msgType ,Object objectId ,Object eventObject){

    String jsonData;

    LOG.warn("---"+objectId.toString());
    Long id = 1l;
    try{
      id = Long.parseLong(getIDFromJdoObjectId(objectId.toString()));
    }catch(Exception e){

    }

    net.sf.json.JSONObject jsonObject = net.sf.json.JSONObject.fromObject(eventObject);
    jsonData = jsonObject.toString();
    LOG.warn("---zjw--json:"+jsonData);
    return new MSGFactory.DDLMsg(msgType, id, jsonData);
  }
}
