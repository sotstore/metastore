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

package org.apache.hadoop.hive.ql.plan;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.ql.exec.Utilities;
import org.apache.hadoop.hive.ql.parse.ParseUtils;
import org.apache.hadoop.hive.ql.parse.SemanticException;

/**
 * CreateSchemaDesc.
 *
 */
@Explain(displayName = "Create Schema")
public class CreateSchemaDesc extends DDLDesc implements Serializable {
  private static final long serialVersionUID = 1L;
  String schemaName;
  ArrayList<FieldSchema> cols;
  String fieldDelim;
  String fieldEscape;
  String collItemDelim;
  String mapKeyDelim;
  String lineDelim;
  String comment;
  Map<String, String> serdeProps;
  Map<String, String> tblProps;
  boolean ifNotExists;

  public CreateSchemaDesc() {
  }

  //add partition columns
  public CreateSchemaDesc(String schemaName, List<FieldSchema> cols,
      String fieldDelim, String fieldEscape, String collItemDelim,
      String mapKeyDelim, String lineDelim, String comment,
      Map<String, String> serdeProps,
      Map<String, String> tblProps,
      boolean ifNotExists) {
    this.schemaName = schemaName;

    this.collItemDelim = collItemDelim;
    this.cols = new ArrayList<FieldSchema>(cols);
    this.comment = comment;
    this.fieldDelim = fieldDelim;
    this.fieldEscape = fieldEscape;
    this.lineDelim = lineDelim;
    this.mapKeyDelim = mapKeyDelim;
    this.serdeProps = serdeProps;
    this.tblProps = tblProps;
    this.ifNotExists = ifNotExists;
  }

  @Explain(displayName = "columns")
  public List<String> getColsString() {
    return Utilities.getFieldSchemaString(getCols());
  }

  @Explain(displayName = "if not exists")
  public boolean getIfNotExists() {
    return ifNotExists;
  }

  public void setIfNotExists(boolean ifNotExists) {
    this.ifNotExists = ifNotExists;
  }

  @Explain(displayName = "name")
  public String getSchemaName() {
    return schemaName;
  }

  public void setSchemaName(String schemaName) {
    this.schemaName = schemaName;
  }

  public ArrayList<FieldSchema> getCols() {
    return cols;
  }

  public void setCols(ArrayList<FieldSchema> cols) {
    this.cols = cols;
  }

  @Explain(displayName = "field delimiter")
  public String getFieldDelim() {
    return fieldDelim;
  }

  public void setFieldDelim(String fieldDelim) {
    this.fieldDelim = fieldDelim;
  }

  @Explain(displayName = "field escape")
  public String getFieldEscape() {
    return fieldEscape;
  }

  public void setFieldEscape(String fieldEscape) {
    this.fieldEscape = fieldEscape;
  }

  @Explain(displayName = "collection delimiter")
  public String getCollItemDelim() {
    return collItemDelim;
  }

  public void setCollItemDelim(String collItemDelim) {
    this.collItemDelim = collItemDelim;
  }

  @Explain(displayName = "map key delimiter")
  public String getMapKeyDelim() {
    return mapKeyDelim;
  }

  public void setMapKeyDelim(String mapKeyDelim) {
    this.mapKeyDelim = mapKeyDelim;
  }

  @Explain(displayName = "line delimiter")
  public String getLineDelim() {
    return lineDelim;
  }

  public void setLineDelim(String lineDelim) {
    this.lineDelim = lineDelim;
  }

  @Explain(displayName = "comment")
  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }


  /**
   * @return the serDe properties
   */
  @Explain(displayName = "serde properties")
  public Map<String, String> getSerdeProps() {
    return serdeProps;
  }

  /**
   * @param serdeProps
   *          the serde properties to set
   */
  public void setSerdeProps(Map<String, String> serdeProps) {
    this.serdeProps = serdeProps;
  }

  /**
   * @return the schema properties
   */
  @Explain(displayName = "schema properties")
  public Map<String, String> getTblProps() {
    return tblProps;
  }

  /**
   * @param tblProps
   *          the schema properties to set
   */
  public void setTblProps(Map<String, String> tblProps) {
    this.tblProps = tblProps;
  }


  public void validate()
      throws SemanticException {

    List<String> colNames = ParseUtils.validateColumnNameUniqueness(this.getCols());

  }


}
