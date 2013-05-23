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
import java.util.Map;

/**
 * CreateNodeDesc.
 *
 */
@Explain(displayName = "Create Node")
public class AddNodeDesc extends DDLDesc implements Serializable {

  private static final long serialVersionUID = 1L;

  String nodeName;
  Integer status;
  String ip;
  Map<String, String> nodeProperties;

  /**
   * For serialization only.
   */
  public AddNodeDesc() {
  }

  public AddNodeDesc(String nodeName, Integer status,
      String ip) {
    super();
    this.nodeName = nodeName;
    this.status = status;
    this.ip = ip;
    this.nodeProperties = null;
  }


  public Map<String, String> getNodeProperties() {
    return nodeProperties;
  }

  public void setNodeProperties(Map<String, String> nodeProps) {
    this.nodeProperties = nodeProps;
  }

  @Explain(displayName="name")
  public String getName() {
    return nodeName;
  }

  public void setName(String nodeName) {
    this.nodeName = nodeName;
  }

  @Explain(displayName="status")
  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  @Explain(displayName="ip")
  public String getIp() {
    return ip;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }
}
