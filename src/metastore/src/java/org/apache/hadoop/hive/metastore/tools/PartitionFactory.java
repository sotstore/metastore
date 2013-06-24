package org.apache.hadoop.hive.metastore.tools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.Subpartition;


/**
 * Support part function:hash/list/range/interval
 * PartitionFactory.
 * @author zjw
 */
public class PartitionFactory {
  private static final Log LOG = LogFactory.getLog(PartitionFactory.class.getName());

  public static class PartitionConstants{
    public static String MAXVALUE="maxvalue";
    public static String MINVALUE="minvalue";
    public static String DEFAULT="default";
  }

  public static long getIntervalSeconds(String interval_unit) throws Exception{
    long interval_seconds=0;
    if("'Y'".equalsIgnoreCase(interval_unit)){
      interval_seconds = interval_seconds * 3600 * 24 * 7 *30 * 365;
    }else if("'M'".equalsIgnoreCase(interval_unit)){
      interval_seconds = interval_seconds * 3600 * 24 * 7 *30;
    }else if("'W'".equalsIgnoreCase(interval_unit)){
      interval_seconds = interval_seconds * 3600 * 24 * 7;
    }else if("'D'".equalsIgnoreCase(interval_unit)){
      interval_seconds = interval_seconds * 3600 * 24;
    }else if("'H'".equalsIgnoreCase(interval_unit)){
      interval_seconds = interval_seconds * 3600;
    }else if("'MI'".equalsIgnoreCase(interval_unit)){
      interval_seconds = interval_seconds * 60;
    }else{
      throw new Exception("Not valid interval unit.");
    }
    return interval_seconds;
  }

  public static enum PartitionType{
    none("none"),roundrobin("roundrobin"),hash("hash"),list("list"),range("range"),interval("interval");
    private static  HashSet<String> Types  = new HashSet<String>();
    static{
      Types.add("none");
      Types.add("roundrobin");
      Types.add("hash");
      Types.add("list");
      Types.add("range");
      Types.add("interval");
    }
    String name;
    private PartitionType(String name)
    {
      this.name = name;
    }

    public String getName(){
      return this.name;
    }

    public static boolean validate(String name){
      return Types.contains(name);
    }
  }

//  public static List<SubPartitionFieldSchema> toSubPartitionFieldSchemaList(
//      List<FieldSchema> srcList){
//    List<SubPartitionFieldSchema> list = new ArrayList<SubPartitionFieldSchema>(srcList.size());
//    for(FieldSchema fs: srcList){
//      list.add(new SubPartitionFieldSchema(fs.getName(), fs.getType(), fs.getComment() ,fs.getPart_num(),
//          fs.getPart_num(), fs.getPart_type().toString(), null));
//    }
//    return list;
//  }

  /**
   *
   * PartitionInfo.存放表的分区规则信息
   * p_version：分区规则信息是按照多版本信息管理的，即一个表同时存在多种分区规则，
   * 加载仅与当前最新（最大的p_version）规则有关
   *
   */
  public static class PartitionInfo{
    String p_col = "";
    int p_level = 1;//0:no partition;1:first leve partition,2:subpartition
    int p_order = 0;
    int p_num = 0;
    int p_version = 0;
    PartitionType p_type = PartitionType.none;//default for column partition,none;
    List<String> args = new ArrayList<String>();
    PartitionInfo(){}

    PartitionInfo(String p_col, int p_level,int p_order,int p_num, PartitionType p_type, List<String> args){
      this.p_col = p_col;
      this.p_level = p_level;
      this.p_order = p_order;
      this.p_num = p_num;
      this.p_type = p_type;
      this.args = args;
    }

    PartitionInfo(PartitionInfo other){
      this.p_col = other.p_col;
      this.p_level = other.p_level;
      this.p_order = other.p_order;
      this.p_num = other.p_num;
      this.p_type = other.p_type;
      this.args = new  ArrayList<String>();
      if(other.args != null){
        for (String other_element : other.getArgs()) {
          this.getArgs().add(new String(other_element));
        };
      }
    }

    public String getP_col() {
      return p_col;
    }

    public void setP_col(String p_col) {
      this.p_col = p_col;
    }

    public int getP_level() {
      return p_level;
    }

    public void setP_level(int p_level) {
      this.p_level = p_level;
    }

    public int getP_order() {
      return p_order;
    }

    public void setP_order(int p_order) {
      this.p_order = p_order;
    }

    public int getP_num() {
      return p_num;
    }

    public void setP_num(int p_num) {
      this.p_num = p_num;
    }

    public PartitionType getP_type() {
      return p_type;
    }

    public void setP_type(PartitionType p_type) {
      this.p_type = p_type;
    }

    public List<String> getArgs() {
      return args;
    }

    public void setArgs(List<String> args) {
      this.args = args;
    }

    public int getP_version() {
      return p_version;
    }

    public void setP_version(int p_version) {
      this.p_version = p_version;
    }

    public String toJson(){
      JSONObject jsonObject = new JSONObject();
      jsonObject.put("p_col", p_col);
      jsonObject.put("p_level", p_level);
      jsonObject.put("p_order", p_order);
      jsonObject.put("p_num", p_num);
      jsonObject.put("p_version", p_version);
      jsonObject.put("p_type", p_type.toString());
      JSONArray jsonArgs = new JSONArray();
      for(String arg : args){
        jsonArgs.add(arg);
      }
      jsonObject.put("args", jsonArgs);

      String jsonData = jsonObject.toString();
      LOG.warn("---zjw--json:"+jsonData);
      return jsonData;
    }

    @SuppressWarnings({"unchecked", "deprecation"})
    public static PartitionInfo fromJson(String jsonData){
      JSONObject json = (JSONObject)JSONSerializer.toJSON(jsonData);
      PartitionInfo pi = new PartitionInfo();
      pi.p_col = json.getString("p_col");
      pi.p_level = Integer.parseInt(json.getString("p_level"));
      pi.p_order = Integer.parseInt(json.getString("p_order"));
      pi.p_num = Integer.parseInt(json.getString("p_num"));
      pi.p_version = Integer.parseInt(json.getString("p_version"));
      pi.p_type = PartitionType.valueOf(json.getString("p_type"));
      pi.args = (List<String>)JSONArray.toList(json.getJSONArray("args"));
      return pi;
    }



    public static List<PartitionInfo> getPartitionInfo(List<FieldSchema> partitionKeys) {
      List<PartitionInfo> pis = new ArrayList<PartitionInfo>();
      int i = 0;
      for(FieldSchema fs : partitionKeys){
        PartitionInfo pi = PartitionInfo.fromJson(fs.getComment());
        pis.add(pi);
      }
      return pis;
    }
  }

  public static class PartitionDefinition{
    String dbName;
    String tableName;

    PartitionInfo pi = new PartitionInfo();
    String part_name = "";
    List<String> values = new ArrayList<String>();
    List<PartitionDefinition> partitions = new ArrayList<PartitionDefinition>();


    public PartitionDefinition(){}

    public PartitionDefinition(PartitionDefinition other){
      this.pi  = new PartitionInfo(other.getPi());
      this.part_name = other.getPart_name();
      this.dbName = other.getDbName();
      this.tableName = other.getTableName();
      for(String v:other.getValues()){
        this.values.add(new String(v));
      }
      if(partitions != null && !partitions.isEmpty()){
        for(PartitionDefinition v:other.getPartitions()){
          this.partitions.add(new PartitionDefinition(v));
        }
      }
    }


    public String getPart_name() {
      return part_name;
    }
    public void setPart_name(String part_name) {
      this.part_name = part_name;
    }

    public PartitionInfo getPi() {
      return pi;
    }
    public void setPi(PartitionInfo pi) {
      this.pi = pi;
    }
    public List<String> getValues() {
      return values;
    }
    public void setValues(List<String> values) {
      this.values = values;
    }
    public List<PartitionDefinition> getPartitions() {
      return partitions;
    }
    public void setPartitions(List<PartitionDefinition> partitions) {
      this.partitions = partitions;
    }

    public void cloneToSubpartitions(List<PartitionDefinition> partitions){
      for(PartitionDefinition p : partitions){
        PartitionDefinition clone = new PartitionDefinition(p);
        clone.setPart_name(this.getPart_name() + "_" + p.getPart_name());
        LOG.warn("---zjw-- subparname:"+clone.getPart_name());
        this.partitions.add(clone);
      }
    }

    public FieldSchema toFieldSchema(){
      List<FieldSchema> subFields = null;
      if(partitions != null && !partitions.isEmpty()){
        subFields = new ArrayList<FieldSchema>();

        for(PartitionDefinition p : partitions){
          FieldSchema subField =  p.toFieldSchema();
          subFields.add(subField);
        }
      }
      FieldSchema field =  new FieldSchema(pi.getP_col(), pi.getP_type().toString(), pi.toJson()) ;
      return field;
    }

    public FieldSchema fromFieldSchema(){
      List<FieldSchema> subFields = null;
      if(partitions != null && !partitions.isEmpty()){
        subFields = new ArrayList<FieldSchema>();

        for(PartitionDefinition p : partitions){
          FieldSchema subField =  p.toFieldSchema();
          subFields.add(subField);
        }
      }
      FieldSchema field =  new FieldSchema(pi.getP_col(), pi.getP_type().toString(), pi.toJson()) ;
      return field;
    }
//    public SubPartitionFieldSchema toSubPartitionFieldSchema(){
//      SubPartitionFieldSchema field =  new SubPartitionFieldSchema(partCol, "", "" ,partitionNum,
//          partitionLevel, part_type.toString(), this.argsToString(args)) ;
//      return field;
//    }
//
//    public MSubPartitionFieldSchema toMSubPartitionFieldSchema(){
//      MSubPartitionFieldSchema field =  new MSubPartitionFieldSchema(partCol, "", "" ,partitionNum,
//          partitionLevel, part_type.toString(), this.argsToString(args)) ;
//      return field;
//    }
//
//    public MFieldSchema toMFieldSchema(){
//      List<MSubPartitionFieldSchema> subFields = null;
//      if(partitions != null && !partitions.isEmpty()){
//        subFields = new ArrayList<MSubPartitionFieldSchema>();
//
//        for(PartitionDefinition p : partitions){
//          MSubPartitionFieldSchema subField =  p.toMSubPartitionFieldSchema();
//          subFields.add(subField);
//        }
//      }
//      MFieldSchema field =  new MFieldSchema(partCol, part_type.toString(), this.argsToString(args),subFields) ;
//      return field;
//    }

    public List<Partition> toPartitionList(){
      List<Partition> parts = null;

      if(partitions != null && !partitions.isEmpty()){
        int now = (int)(System.currentTimeMillis()/1000);
        parts = new ArrayList<Partition>();
        for(PartitionDefinition pd : partitions){
          List<Subpartition> subParts =  pd.toSubpartitionList();
          Partition part =  new Partition(pd.getValues(), dbName,tableName,now,now,null,null,null) ;
          part.setPartitionName(pd.getPart_name());
          part.setSubpartitions(subParts);
          parts.add(part);
        }
      }

      return parts;
    }


    public List<Subpartition> toSubpartitionList(){
      List<Subpartition> parts = null;
      if(partitions != null && !partitions.isEmpty()){
        int now = (int)(System.currentTimeMillis()/1000);
        parts = new ArrayList<Subpartition>();
        for(PartitionDefinition pd : partitions){
          Subpartition p =  new Subpartition(pd.getValues(), dbName,tableName,now,now,null,null,null) ;
          p.setPartitionName(pd.getPart_name());
          parts.add(p);
        }
      }
      return parts;
    }
    public String getDbName() {
      return dbName;
    }
    public void setDbName(String dbName) {
      this.dbName = dbName;
    }
    public String getTableName() {
      return tableName;
    }
    public void setTableName(String tableName) {
      this.tableName = tableName;
    }



  }

  public static String arrayToJson(List<String> values){
    JSONArray jsonVals = new JSONArray();
    for(String value : values){
      jsonVals.add(value);
    }
    return jsonVals.toString();
  }

  /**
  *
  * @param colList
  * @param global_sub_pd
  * @param isAuto 表明在 createtable的时候自动建立的分区
  */
 public static void createSubPartition(List<FieldSchema> colList,
     PartitionDefinition global_sub_pd,boolean isAuto,String part_name) {
   if(global_sub_pd.getPartitions() != null && !global_sub_pd.getPartitions().isEmpty()){
     return;
   }
   assert(colList.size() == 2);
   FieldSchema part = colList.get(0);
   FieldSchema subpart = colList.get(1);
   switch(PartitionFactory.PartitionType.valueOf(part.getType())){
     case none:
     case roundrobin:
       return;//no need to creat subpartitions
     case list:
     case range:
     case interval:
       if(isAuto){
         return;
       }
     case hash:
       switch(PartitionFactory.PartitionType.valueOf(subpart.getType())){
         case none:
         case interval:
         case roundrobin:
         case list:
         case range:
           return;//no need to creat subpartitions
         case hash://creat hash subpartitions
           List<PartitionDefinition> global_sub_parts = new ArrayList<PartitionDefinition>();
           for(int i=1;i<=global_sub_pd.getPi().getP_num();i++){
             PartitionDefinition partition = new PartitionDefinition();
             if(isAuto){
               partition.setPart_name("p"+i);
             }else{
               partition.setPart_name(part_name+"_p"+i);
             }
             List<String> values = new ArrayList<String>();
             values.add(new String(""+i));
             partition.setValues(values);
             if(global_sub_pd != null){
               partition.setTableName(global_sub_pd.getTableName());
               partition.getPi().setP_col(global_sub_pd.getPi().getP_col());
               partition.getPi().setP_type(global_sub_pd.getPi().getP_type());
               partition.getPi().setP_level(global_sub_pd.getPi().getP_level());
               partition.getPi().setP_num(global_sub_pd.getPi().getP_num());
               partition.getPi().setP_order(global_sub_pd.getPi().getP_order());
               partition.getPi().setP_version(global_sub_pd.getPi().getP_version());
               partition.getPi().setArgs(global_sub_pd.getPi().getArgs());
             }
             global_sub_parts.add(partition);
           }
           global_sub_pd.setPartitions(global_sub_parts);
           break;
       }
       break;

   }

 }

}
