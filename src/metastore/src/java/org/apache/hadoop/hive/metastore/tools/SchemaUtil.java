package org.apache.hadoop.hive.metastore.tools;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SchemaUtil {

  private static final Log LOG = LogFactory.getLog("org.apache.hadoop.hive.metastore.tools.SchemaUtil");


  HashMap<String,String> originTabNameMap;
  HashMap<String, LinkedHashMap<String, String>> originNameMap;

  public SchemaUtil(){
    originNameMap =  new HashMap<String, LinkedHashMap<String, String>>();
    originTabNameMap = new HashMap<String, String>();

  }

  public HashMap<String, String> getOriginTabNameMap() {
    return originTabNameMap;
  }

  public void setOriginTabNameMap(HashMap<String, String> originTabNameMap) {
    this.originTabNameMap = originTabNameMap;
  }

  public HashMap<String, LinkedHashMap<String, String>> getOriginNameMap() {
    return originNameMap;
  }

  public void setOriginNameMap(HashMap<String, LinkedHashMap<String, String>> originNameMap) {
    this.originNameMap = originNameMap;
  }

  public String toJson(){
    JSONObject jsonObject = new JSONObject();
    for(Entry<String,LinkedHashMap<String,String>> e : getOriginNameMap().entrySet()){

      JSONArray jsonArgs = new JSONArray();
      for(Entry<String,String> v : e.getValue().entrySet()){
        jsonArgs.add(v.getKey()+"."+v.getValue());
      }
      jsonObject.put(e.getKey(),jsonArgs);
    }


    String jsonData = jsonObject.toString();
    LOG.warn("---zjw--json:"+jsonData);
    return jsonData;

  }

  public static SchemaUtil fromJson(String jsonData){
    SchemaUtil su = new SchemaUtil();

    JSONObject json = (JSONObject)JSONSerializer.toJSON(jsonData);
    for(Object key : json.keySet()){
      String value = json.get(key.toString()).toString();
      String[] tab_col = value.split(".", 2);
      LinkedHashMap<String,String> map = su.getOriginNameMap().get(key.toString());
      if( map ==  null){
        map = new LinkedHashMap<String,String>();
        su.getOriginNameMap().put(key.toString(), map);
      }
      map.put(tab_col[0], tab_col[1]);
    }

    return su;
  }

}
