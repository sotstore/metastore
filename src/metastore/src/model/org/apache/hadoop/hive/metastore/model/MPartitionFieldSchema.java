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

/**
 *
 */
package org.apache.hadoop.hive.metastore.model;


/**
 * Represent a column or a type of a table or object
 */
public class MPartitionFieldSchema {
  private String name;
  private String type;
  private String comment;

  private int part_num;
  private int part_level;
  private String part_type;
  private String part_type_param;
//  private List<MSubPartitionFieldSchema> subPartitionKeys;
  public MPartitionFieldSchema() {}

  /**
   * @param comment
   * @param name
   * @param type
   */
  public MPartitionFieldSchema(String name, String type, String comment) {
    this.comment = comment;
    this.name = name;
    this.type = type;
  }

  public MPartitionFieldSchema(String name, String type, String comment,int part_num,
      int part_level,String part_type,String part_type_param
//      ,List<MSubPartitionFieldSchema> subPartitionKeys
      ) {
    this.comment = comment;
    this.name = name;
    this.type = type;
    this.part_num = part_num;
    this.part_level = part_level;
    this.part_type = part_type;
    this.part_type_param = part_type_param;
//    this.subPartitionKeys = subPartitionKeys;
  }
  /**
   * @return the name
   */
  public String getName() {
    return name;
  }
  /**
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
  }
  /**
   * @return the comment
   */
  public String getComment() {
    return comment;
  }
  /**
   * @param comment the comment to set
   */
  public void setComment(String comment) {
    this.comment = comment;
  }
  /**
   * @return the type
   */
  public String getType() {
    return type;
  }
  /**
   * @param field the type to set
   */
  public void setType(String field) {
    this.type = field;
  }

  public int getPart_num() {
    return part_num;
  }

  public void setPart_num(int part_num) {
    this.part_num = part_num;
  }

  public int getPart_level() {
    return part_level;
  }

  public void setPart_level(int part_level) {
    this.part_level = part_level;
  }

  public String getPart_type() {
    return part_type;
  }

  public void setPart_type(String part_type) {
    this.part_type = part_type;
  }

  public String getPart_type_param() {
    return part_type_param;
  }

  public void setPart_type_param(String part_type_param) {
    this.part_type_param = part_type_param;
  }

//  public List<MSubPartitionFieldSchema> getSubPartitionKeys() {
//    return subPartitionKeys;
//  }
//
//  public void setSubPartitionKeys(List<MSubPartitionFieldSchema> subPartitionKeys) {
//    this.subPartitionKeys = subPartitionKeys;
//  }


}
