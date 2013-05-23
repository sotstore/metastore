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
 * DropNodeDesc.
 *
 */
@Explain(displayName = "Drop Node")
public class DropNodeDesc extends DDLDesc implements Serializable {
  private static final long serialVersionUID = 1L;

  String nodeName;
  boolean ifExists;

  public DropNodeDesc(String nodeName ) {
    this(nodeName,false);
  }

  public DropNodeDesc(String nodeName, boolean ifExists ) {
    super();
    this.nodeName = nodeName;
    this.ifExists = ifExists;
  }

  @Explain(displayName = "node")
  public String getNodeName() {
    return nodeName;
  }

  public void setNodeName(String nodeName) {
    this.nodeName = nodeName;
  }

  @Explain(displayName = "if exists")
  public boolean getIfExists() {
    return ifExists;
  }

  public void setIfExists(boolean ifExists) {
    this.ifExists = ifExists;
  }

}
