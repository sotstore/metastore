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
 * CreateDatacenterDesc.
 *
 */
@Explain(displayName = "Create Datacenter")
public class CreateDatacenterDesc extends DDLDesc implements Serializable {

  private static final long serialVersionUID = 1L;

  String datacenterName;
  String locationUri;
  String comment;
  boolean ifNotExists;
  Map<String, String> dcProperties;

  /**
   * For serialization only.
   */
  public CreateDatacenterDesc() {
  }

  public CreateDatacenterDesc(String datacenterName, String comment,
      String locationUri, boolean ifNotExists) {
    super();
    this.datacenterName = datacenterName;
    this.comment = comment;
    this.locationUri = locationUri;
    this.ifNotExists = ifNotExists;
    this.dcProperties = null;
  }

  public CreateDatacenterDesc(String datacenterName, boolean ifNotExists) {
    this(datacenterName, null, null, ifNotExists);
  }



  @Explain(displayName="if not exists")
  public boolean getIfNotExists() {
    return ifNotExists;
  }

  public void setIfNotExists(boolean ifNotExists) {
    this.ifNotExists = ifNotExists;
  }

  public Map<String, String> getDatacenterProperties() {
    return dcProperties;
  }

  public void setDatacenterProperties(Map<String, String> dcProps) {
    this.dcProperties = dcProps;
  }

  @Explain(displayName="name")
  public String getName() {
    return datacenterName;
  }

  public void setName(String datacenterName) {
    this.datacenterName = datacenterName;
  }

  @Explain(displayName="comment")
  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  @Explain(displayName="locationUri")
  public String getLocationUri() {
    return locationUri;
  }

  public void setLocationUri(String locationUri) {
    this.locationUri = locationUri;
  }
}
