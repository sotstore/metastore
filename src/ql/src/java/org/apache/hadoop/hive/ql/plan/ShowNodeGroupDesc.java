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

import org.apache.hadoop.fs.Path;

/**
 * ShowDatabasesDesc.
 *
 */
@Explain(displayName = "Show Databases")
public class ShowNodeGroupDesc extends DDLDesc implements Serializable {
  private static final long serialVersionUID = 1L;
  String resFile;
  String ng_name;

  private static final String schema = "ng_name#string".toString();

  public String getSchema() {
    return schema;
  }

  public ShowNodeGroupDesc() {
  }


  public ShowNodeGroupDesc(Path path) {
    super();
    this.resFile = path.toString();
  }

  public ShowNodeGroupDesc(Path path, String ng_name) {
    super();
    this.resFile = path.toString();
    this.ng_name = ng_name;
  }


  /**
   * @return the resFile
   */
  @Explain(displayName = "result file", normalExplain = false)
  public String getResFile() {
    return resFile;
  }

  /**
   * @param resFile
   *          the resFile to set
   */
  public void setResFile(String resFile) {
    this.resFile = resFile;
  }

  public String getNg_name() {
    return ng_name;
  }

  public void setNg_name(String ng_name) {
    this.ng_name = ng_name;
  }




}
