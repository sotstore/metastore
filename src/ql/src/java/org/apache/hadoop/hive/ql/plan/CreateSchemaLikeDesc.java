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

/**
 * CreateSchemaLikeDesc.
 *
 */
@Explain(displayName = "Create Schema")
public class CreateSchemaLikeDesc extends DDLDesc implements Serializable {
  private static final long serialVersionUID = 1L;
  String schemaName;
  boolean ifNotExists;
  String likeSchemaName;

  public CreateSchemaLikeDesc() {
  }

  public CreateSchemaLikeDesc(String schemaName,  boolean ifNotExists,
      String likeSchemaName) {
    this.schemaName = schemaName;
    this.ifNotExists = ifNotExists;
    this.likeSchemaName = likeSchemaName;
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

  public String getLikeSchemaName() {
    return likeSchemaName;
  }

  public void setLikeSchemaName(String likeSchemaName) {
    this.likeSchemaName = likeSchemaName;
  }



}
