/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.hive.ql.metadata;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.metastore.MetaStoreUtils;
import org.apache.hadoop.hive.metastore.ProtectMode;
import org.apache.hadoop.hive.metastore.TableType;
import org.apache.hadoop.hive.metastore.api.Constants;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.Order;
import org.apache.hadoop.hive.metastore.api.SerDeInfo;
import org.apache.hadoop.hive.metastore.api.SkewedInfo;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.ql.io.HiveSequenceFileOutputFormat;
import org.apache.hadoop.hive.serde.serdeConstants;
import org.apache.hadoop.hive.serde2.Deserializer;
import org.apache.hadoop.hive.serde2.MetadataTypedColumnsetSerDe;
import org.apache.hadoop.hive.serde2.SerDeUtils;
import org.apache.hadoop.mapred.SequenceFileInputFormat;

/**
 * A Hive Schema: is a fundamental unit of data in Hive that shares a common schema/DDL.
 *
 * Please note that the ql code should always go through methods of this class to access the
 * metadata, instead of directly accessing org.apache.hadoop.hive.metastore.api.GlobalSchema.  This
 * helps to isolate the metastore code and the ql code.
 */
public class GlobalSchema implements Serializable {

  private static final long serialVersionUID = 1L;

  static final private Log LOG = LogFactory.getLog("hive.ql.metadata.Schema");

  private org.apache.hadoop.hive.metastore.api.GlobalSchema tschema;

  /**
   * These fields are all cached fields.  The information comes from tSchema.
   */
  private Deserializer deserializer;
  private URI uri;
  private HiveStorageHandler storageHandler;
  private boolean isHeterView = false;//异构试图

  /**
   * Used only for serialization.
   */
  public GlobalSchema() {
  }

  public GlobalSchema(org.apache.hadoop.hive.metastore.api.GlobalSchema table) {
    tschema = table;
    if (!isView()) {

    }
    for(Entry<String,String> param :table.getParameters().entrySet()){
      LOG.info("---zjw--getParameters:"+param.getKey()+"--"+param.getValue());
    }
    if(table.getParameters().containsKey(Constants.META_HETER_VIEW)){
      isHeterView = true;
    }
  }

  public GlobalSchema(String tableName) {
    this(getEmptySchema( tableName));
  }

  /**
   * This function should only be used in serialization.
   * We should never call this function to modify the fields, because
   * the cached fields will become outdated.
   */
  public org.apache.hadoop.hive.metastore.api.GlobalSchema getTSchema() {
    return tschema;
  }

  /**
   * This function should only be called by Java serialization.
   */
  public void setTSchema(org.apache.hadoop.hive.metastore.api.GlobalSchema tSchema) {
    this.tschema = tSchema;
  }

  /**
   * Initialize an emtpy table.
   */
  static org.apache.hadoop.hive.metastore.api.GlobalSchema
  getEmptySchema(String tableName) {
    StorageDescriptor sd = new StorageDescriptor();
    {
      sd.setSerdeInfo(new SerDeInfo());
      sd.setNumBuckets(-1);
      sd.setBucketCols(new ArrayList<String>());
      sd.setCols(new ArrayList<FieldSchema>());
      sd.setParameters(new HashMap<String, String>());
      sd.setSortCols(new ArrayList<Order>());
      sd.getSerdeInfo().setParameters(new HashMap<String, String>());
      // We have to use MetadataTypedColumnsetSerDe because LazySimpleSerDe does
      // not support a table with no columns.
      sd.getSerdeInfo().setSerializationLib(MetadataTypedColumnsetSerDe.class.getName());
      sd.getSerdeInfo().getParameters().put(serdeConstants.SERIALIZATION_FORMAT, "1");
      sd.setInputFormat(SequenceFileInputFormat.class.getName());
      sd.setOutputFormat(HiveSequenceFileOutputFormat.class.getName());
      SkewedInfo skewInfo = new SkewedInfo();
      skewInfo.setSkewedColNames(new ArrayList<String>());
      skewInfo.setSkewedColValues(new ArrayList<List<String>>());
      skewInfo.setSkewedColValueLocationMaps(new HashMap<List<String>, String>());
      sd.setSkewedInfo(skewInfo);
    }

    org.apache.hadoop.hive.metastore.api.GlobalSchema t = new org.apache.hadoop.hive.metastore.api.GlobalSchema();
    {
      t.setSd(sd);
      t.setParameters(new HashMap<String, String>());
      t.setSchemaType(TableType.MANAGED_TABLE.toString());

    }
    return t;
  }

  public void checkValidity() throws HiveException {
    // check for validity
    String name = tschema.getSchemaName();
    if (null == name || name.length() == 0
        || !MetaStoreUtils.validateName(name)) {
      throw new HiveException("[" + name + "]: is not a valid table name");
    }
    if (!isHeterView && 0 == getCols().size()) {
      throw new HiveException(
          "at least one column must be specified for the table");
    }
    if (!isView()) {

    }

    if (isView()) {
      assert(getViewOriginalText() != null);
      assert(getViewExpandedText() != null);
    } else {
      assert(getViewOriginalText() == null);
      assert(getViewExpandedText() == null);
    }

    if(isHeterView){
      return;
    }

    Iterator<FieldSchema> iterCols = getCols().iterator();
    List<String> colNames = new ArrayList<String>();
    while (iterCols.hasNext()) {
      String colName = iterCols.next().getName();
      Iterator<String> iter = colNames.iterator();
      while (iter.hasNext()) {
        String oldColName = iter.next();
        if (colName.equalsIgnoreCase(oldColName)) {
          throw new HiveException("Duplicate column name " + colName
              + " in the table definition.");
        }
      }
      colNames.add(colName.toLowerCase());
    }


    return;
  }

  public boolean isHeterView() {
    return isHeterView;
  }

  public void setHeterView(boolean isHeterView) {
    this.isHeterView = isHeterView;
  }


  public org.apache.hadoop.hive.metastore.api.GlobalSchema getTschema() {
    return tschema;
  }

  public void setTschema(org.apache.hadoop.hive.metastore.api.GlobalSchema tschema) {
    this.tschema = tschema;
  }

  final public Path getPath() {
    String location = tschema.getSd().getLocation();
    if (location == null) {
      return null;
    }
    return new Path(location);
  }

  final public String getSchemaName() {
    return tschema.getSchemaName();
  }

  final public URI getDataLocation() {
    if (uri == null) {
      Path path = getPath();
      if (path != null) {
        uri = path.toUri();
      }
    }
    return uri;
  }


  public HiveStorageHandler getStorageHandler() {
    if (storageHandler != null) {
      return storageHandler;
    }
    try {
      storageHandler = HiveUtils.getStorageHandler(
        Hive.get().getConf(),
        getProperty(
          org.apache.hadoop.hive.metastore.api.hive_metastoreConstants.META_TABLE_STORAGE));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return storageHandler;
  }



  public void setProperty(String name, String value) {
    tschema.getParameters().put(name, value);
  }

  public String getProperty(String name) {
    return tschema.getParameters().get(name);
  }

  public void setTableType(TableType tableType) {
     tschema.setSchemaType(tableType.toString());
   }

  public TableType getTableType() {
     return Enum.valueOf(TableType.class, tschema.getSchemaType());
   }


   @Override
  public String toString() {
    return tschema.getSchemaName();
  }

   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
     final int prime = 31;
     int result = 1;
     result = prime * result + ((tschema == null) ? 0 : tschema.hashCode());
     return result;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj) {
     if (this == obj) {
       return true;
     }
     if (obj == null) {
       return false;
     }
     if (getClass() != obj.getClass()) {
       return false;
     }
     GlobalSchema other = (GlobalSchema) obj;
     if (tschema == null) {
       if (other.tschema != null) {
         return false;
       }
     } else if (!tschema.equals(other.tschema)) {
       return false;
     }
     return true;
   }



  // TODO merge this with getBucketCols function
  public String getBucketingDimensionId() {
    List<String> bcols = tschema.getSd().getBucketCols();
    if (bcols == null || bcols.size() == 0) {
      return null;
    }

    if (bcols.size() > 1) {
      LOG.warn(this
          + " table has more than one dimensions which aren't supported yet");
    }

    return bcols.get(0);
  }

  public void setDataLocation(URI uri) {
    this.uri = uri;
    tschema.getSd().setLocation(uri.toString());
  }

  public void unsetDataLocation() {
    this.uri = null;
    tschema.getSd().unsetLocation();
  }

  public void setBucketCols(List<String> bucketCols) throws HiveException {
    if (bucketCols == null) {
      return;
    }

    for (String col : bucketCols) {
      if (!isField(col)) {
        throw new HiveException("Bucket columns " + col
            + " is not part of the table columns (" + getCols() );
      }
    }
    tschema.getSd().setBucketCols(bucketCols);
  }

  public void setSortCols(List<Order> sortOrder) throws HiveException {
    tschema.getSd().setSortCols(sortOrder);
  }

  public void setSkewedValueLocationMap(List<String> valList, String dirName)
      throws HiveException {
    Map<List<String>, String> mappings = tschema.getSd().getSkewedInfo()
        .getSkewedColValueLocationMaps();
    if (null == mappings) {
      mappings = new HashMap<List<String>, String>();
      tschema.getSd().getSkewedInfo().setSkewedColValueLocationMaps(mappings);
    }

    // Add or update new mapping
    mappings.put(valList, dirName);
  }

  public Map<List<String>,String> getSkewedColValueLocationMaps() {
    return (tschema.getSd().getSkewedInfo() != null) ? tschema.getSd().getSkewedInfo()
        .getSkewedColValueLocationMaps() : new HashMap<List<String>, String>();
  }

  public void setSkewedColValues(List<List<String>> skewedValues) throws HiveException {
    tschema.getSd().getSkewedInfo().setSkewedColValues(skewedValues);
  }

  public List<List<String>> getSkewedColValues(){
    return (tschema.getSd().getSkewedInfo() != null) ? tschema.getSd().getSkewedInfo()
        .getSkewedColValues() : new ArrayList<List<String>>();
  }

  public void setSkewedColNames(List<String> skewedColNames) throws HiveException {
    tschema.getSd().getSkewedInfo().setSkewedColNames(skewedColNames);
  }

  public List<String> getSkewedColNames() {
    return (tschema.getSd().getSkewedInfo() != null) ? tschema.getSd().getSkewedInfo()
        .getSkewedColNames() : new ArrayList<String>();
  }

  public SkewedInfo getSkewedInfo() {
    return tschema.getSd().getSkewedInfo();
  }

  public void setSkewedInfo(SkewedInfo skewedInfo) throws HiveException {
    tschema.getSd().setSkewedInfo(skewedInfo);
  }

  public boolean isStoredAsSubDirectories() {
    return tschema.getSd().isStoredAsSubDirectories();
  }

  public void setStoredAsSubDirectories(boolean storedAsSubDirectories) throws HiveException {
    tschema.getSd().setStoredAsSubDirectories(storedAsSubDirectories);
  }

  private boolean isField(String col) {
    for (FieldSchema field : getCols()) {
      if (field.getName().equals(col)) {
        return true;
      }
    }
    return false;
  }

  public List<FieldSchema> getCols() {
    boolean getColsFromSerDe = SerDeUtils.shouldGetColsFromSerDe(
      getSerializationLib());
    if (!getColsFromSerDe) {
      return tschema.getSd().getCols();
    } else {
//      try {
//        return Hive.getFieldsFromDeserializer(getSchemaName(), getDeserializer());
//      } catch (HiveException e) {
//        LOG.error("Unable to get field from serde: " + getSerializationLib(), e);
//      }
//      return new ArrayList<FieldSchema>();
      LOG.error("Unable to get field from serde,getColsFromSerDe:" + getColsFromSerDe);
      return new ArrayList<FieldSchema>();
    }
  }

  /**
   * Returns a list of all the columns of the table (data columns + partition
   * columns in that order.
   *
   * @return List<FieldSchema>
   */
  public List<FieldSchema> getAllCols() {
    ArrayList<FieldSchema> f_list = new ArrayList<FieldSchema>();
    f_list.addAll(getCols());
    return f_list;
  }


  public int getNumBuckets() {
    return tschema.getSd().getNumBuckets();
  }

  /**
   * Replaces the directory corresponding to the table by srcf. Works by
   * deleting the table directory and renaming the source directory.
   *
   * @param srcf
   *          Source directory
   */
  protected void replaceFiles(Path srcf) throws HiveException {
    Path tableDest =  new Path(getDataLocation().getPath());
    Hive.replaceFiles(srcf, tableDest, tableDest, Hive.get().getConf());
  }

  /**
   * Inserts files specified into the partition. Works by moving files
   *
   * @param srcf
   *          Files to be moved. Leaf directories or globbed file paths
   */
  protected void copyFiles(Path srcf) throws HiveException {
    FileSystem fs;
    try {
      fs = FileSystem.get(getDataLocation(), Hive.get().getConf());
      Hive.copyFiles(Hive.get().getConf(), srcf, new Path(getDataLocation().getPath()), fs);
    } catch (IOException e) {
      throw new HiveException("addFiles: filesystem error in check phase", e);
    }
  }


  public void setFields(List<FieldSchema> fields) {
    tschema.getSd().setCols(fields);
  }


  /**
   * @return The owner of the table.
   * @see org.apache.hadoop.hive.metastore.api.GlobalSchema#getOwner()
   */
  public String getOwner() {
    return tschema.getOwner();
  }

  /**
   * @return The table parameters.
   * @see org.apache.hadoop.hive.metastore.api.GlobalSchema#getParameters()
   */
  public Map<String, String> getParameters() {
    return tschema.getParameters();
  }

  /**
   * @return The retention on the table.
   * @see org.apache.hadoop.hive.metastore.api.GlobalSchema#getRetention()
   */
  public int getRetention() {
    return tschema.getRetention();
  }

  /**
   * @param owner
   * @see org.apache.hadoop.hive.metastore.api.GlobalSchema#setOwner(java.lang.String)
   */
  public void setOwner(String owner) {
    tschema.setOwner(owner);
  }

  /**
   * @param retention
   * @see org.apache.hadoop.hive.metastore.api.GlobalSchema#setRetention(int)
   */
  public void setRetention(int retention) {
    tschema.setRetention(retention);
  }

  private SerDeInfo getSerdeInfo() {
    return tschema.getSd().getSerdeInfo();
  }

  public void setSerializationLib(String lib) {
    getSerdeInfo().setSerializationLib(lib);
  }

  public String getSerializationLib() {
    return getSerdeInfo().getSerializationLib();
  }

  public String getSerdeParam(String param) {
    return getSerdeInfo().getParameters().get(param);
  }

  public String setSerdeParam(String param, String value) {
    return getSerdeInfo().getParameters().put(param, value);
  }

  public List<String> getBucketCols() {
    return tschema.getSd().getBucketCols();
  }

  public List<Order> getSortCols() {
    return tschema.getSd().getSortCols();
  }

  public void setSchemaName(String tableName) {
    tschema.setSchemaName(tableName);
  }

  /**
   * @return the original view text, or null if this table is not a view
   */
  public String getViewOriginalText() {
    return tschema.getViewOriginalText();
  }

  /**
   * @param viewOriginalText
   *          the original view text to set
   */
  public void setViewOriginalText(String viewOriginalText) {
    tschema.setViewOriginalText(viewOriginalText);
  }

  /**
   * @return the expanded view text, or null if this table is not a view
   */
  public String getViewExpandedText() {
    return tschema.getViewExpandedText();
  }

  public void clearSerDeInfo() {
    tschema.getSd().getSerdeInfo().getParameters().clear();
  }
  /**
   * @param viewExpandedText
   *          the expanded view text to set
   */
  public void setViewExpandedText(String viewExpandedText) {
    tschema.setViewExpandedText(viewExpandedText);
  }

  /**
   * @return whether this table is actually a view
   */
  public boolean isView() {
    return TableType.VIRTUAL_VIEW.equals(getTableType());
  }



  public GlobalSchema copy() throws HiveException {
    return new GlobalSchema(tschema.deepCopy());
  }

  public void setCreateTime(int createTime) {
    tschema.setCreateTime(createTime);
  }

  public int getLastAccessTime() {
    return tschema.getLastAccessTime();
  }

  public void setLastAccessTime(int lastAccessTime) {
    tschema.setLastAccessTime(lastAccessTime);
  }

  public boolean isNonNative() {
    return getProperty(
      org.apache.hadoop.hive.metastore.api.hive_metastoreConstants.META_TABLE_STORAGE)
      != null;
  }

  /**
   * @param protectMode
   */
  public void setProtectMode(ProtectMode protectMode){
    Map<String, String> parameters = tschema.getParameters();
    String pm = protectMode.toString();
    if (pm != null) {
      parameters.put(ProtectMode.PARAMETER_NAME, pm);
    } else {
      parameters.remove(ProtectMode.PARAMETER_NAME);
    }
    tschema.setParameters(parameters);
  }

  /**
   * @return protect mode
   */
  public ProtectMode getProtectMode(){
    Map<String, String> parameters = tschema.getParameters();

    if (!parameters.containsKey(ProtectMode.PARAMETER_NAME)) {
      return new ProtectMode();
    } else {
      return ProtectMode.getProtectModeFromString(
          parameters.get(ProtectMode.PARAMETER_NAME));
    }
  }

  /**
   * @return True protect mode indicates the table if offline.
   */
  public boolean isOffline(){
    return getProtectMode().offline;
  }

  /**
   * @return True if protect mode attribute of the partition indicate
   * that it is OK to drop the partition
   */
  public boolean canDrop() {
    ProtectMode mode = getProtectMode();
    return (!mode.noDrop && !mode.offline && !mode.readOnly && !mode.noDropCascade);
  }

  /**
   * @return True if protect mode attribute of the table indicate
   * that it is OK to write the table
   */
  public boolean canWrite() {
    ProtectMode mode = getProtectMode();
    return (!mode.offline && !mode.readOnly);
  }

  /**
   * @return include the db name
   */
  public String getCompleteName() {
    return  getSchemaName();
  }


};
