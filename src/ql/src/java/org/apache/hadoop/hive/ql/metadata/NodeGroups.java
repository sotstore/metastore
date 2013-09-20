package org.apache.hadoop.hive.ql.metadata;

import java.io.Serializable;
import java.util.Set;

public class NodeGroups implements Serializable {

  private static final long serialVersionUID = 1L;

  String node_group_name;
  String comment;
  String status;
  Set<String> nodes;

  public NodeGroups(){

  }

  public NodeGroups(String node_group_name) {
    super();
    this.node_group_name = node_group_name;
  }

  public NodeGroups(String node_group_name, String comment, String status, Set<String> nodes) {
    super();
    this.node_group_name = node_group_name;
    this.comment = comment;
    this.status = status;
    this.nodes = nodes;
  }



  public String getNode_group_name() {
    return node_group_name;
  }

  public void setNode_group_name(String node_group_name) {
    this.node_group_name = node_group_name;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Set<String> getNodes() {
    return nodes;
  }

  public void setNodes(Set<String> nodes) {
    this.nodes = nodes;
  }



}
