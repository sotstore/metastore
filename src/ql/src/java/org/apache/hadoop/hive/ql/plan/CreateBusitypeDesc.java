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
 * CreateBusitypeDesc.
 *
 */
@Explain(displayName = "Create busitype")
public class CreateBusitypeDesc extends DDLDesc implements Serializable {

  private static final long serialVersionUID = 1L;

  String busitypeName;
  String comment;

  /**
   * For serialization only.
   */
  public CreateBusitypeDesc() {
  }

  public CreateBusitypeDesc(String busitypeName, String comment) {
    super();
    this.busitypeName = busitypeName;
    this.comment = comment;
  }

  public CreateBusitypeDesc(String busitypeName) {
    this(busitypeName, null);
  }


  @Explain(displayName="name")
  public String getName() {
    return busitypeName;
  }

  public void setName(String busitypeName) {
    this.busitypeName = busitypeName;
  }

  @Explain(displayName="comment")
  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }
}
