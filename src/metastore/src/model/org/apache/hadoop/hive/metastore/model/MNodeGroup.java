package org.apache.hadoop.hive.metastore.model;

import java.util.Set;


public class MNodeGroup {
  private String node_group_name;
  private String comment;
  private int status;
  private Set<MNode> nodes;
  private Set<MTable> attachedtables;
  private Set<MDatabase> dbs;

  public MNodeGroup(String node_group_name,String comment, int status,Set<MNode> nodes) {

    this.node_group_name = node_group_name;
    this.comment = comment;
    this.status = status;
    this.nodes = nodes;
  }

  public MNodeGroup(String node_group_name,String comment, int status,Set<MNode> nodes,Set<MDatabase> dbs,Set<MTable> attachedtables) {

    this(node_group_name, comment, status,nodes);
    this.nodes = nodes;
    this.attachedtables = attachedtables;
    this.dbs = dbs;
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

  public int getStatus() {
    return status;
  }
  public void setStatus(int status) {
    this.status = status;
  }

  public Set<MNode> getNodes() {
    return nodes;
  }

  public void setNodes(Set<MNode> nodes) {
    this.nodes = nodes;
  }

  public Set<MTable> getAttachedtables() {
    return attachedtables;
  }

  public void setAttachedtables(Set<MTable> attachedtables) {
    this.attachedtables = attachedtables;
  }

  public Set<MDatabase> getDbs() {
    return dbs;
  }

  public void setDbs(Set<MDatabase> dbs) {
    this.dbs = dbs;
  }



}
