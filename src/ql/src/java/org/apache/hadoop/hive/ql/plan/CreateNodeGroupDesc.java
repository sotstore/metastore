package org.apache.hadoop.hive.ql.plan;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * CreateNodeGroupDesc.
 *
 */
@Explain(displayName = "Create NodeGroupDesc")
public class CreateNodeGroupDesc extends DDLDesc implements Serializable {

    private static final long serialVersionUID = 1L;
    String nodeGroupName;
    String comment;
    boolean ifNotExists;
    Map<String, String> nodeGroupProps;
    Set<String> nodes;

    /**
     * For serialization only.
     */

    public CreateNodeGroupDesc() {
      super();
    }

    public CreateNodeGroupDesc(String nodeGroupName, String comment, boolean ifNotExists,
        Set<String> nodes) {
      super();
      this.nodeGroupName = nodeGroupName;
      this.comment = comment;
      this.ifNotExists = ifNotExists;
      this.nodes = nodes;
    }

    public CreateNodeGroupDesc(String nodeGroupName, boolean ifNotExists) {
      this(nodeGroupName, null, ifNotExists,null);
    }



    @Explain(displayName="if not exists")
    public boolean getIfNotExists() {
      return ifNotExists;
    }

    public void setIfNotExists(boolean ifNotExists) {
      this.ifNotExists = ifNotExists;
    }

    public Map<String, String> getNodeGroupProps() {
      return nodeGroupProps;
    }

    public void setNodeGroupProps(Map<String, String> nodeGroupProps) {
      this.nodeGroupProps = nodeGroupProps;
    }

    @Explain(displayName="name")
    public String getNodeGroupName() {
      return nodeGroupName;
    }

    public void setNodeGroupName(String nodeGroupName) {
      this.nodeGroupName = nodeGroupName;
    }

    @Explain(displayName="comment")
    public String getComment() {
      return comment;
    }

    public void setComment(String comment) {
      this.comment = comment;
    }

    @Explain(displayName="nodes")
    public Set<String> getNodes() {
      return nodes;
    }

    public void setNodes(Set<String> nodes) {
      this.nodes = nodes;
    }

}
