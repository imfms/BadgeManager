package ms.imf.badge.entity;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * 节点路径，包含一条节点链
 *
 * @author f_ms
 */
public class NodePath implements Serializable {

    public static final NodePath EMPTY = new NodePath(Collections.<Node>emptyList());

    public static NodePath instance(List<Node> nodes) {
        return new NodePath(nodes);
    }
    public static NodePath instance(Node... nodes) {
        return instance(Arrays.asList(nodes));
    }
    public static NodePath instance(String... nodeTypes) {
        Node[] newNodes = new Node[nodeTypes.length];
        for (int i = 0; i < nodeTypes.length; i++) {
            newNodes[i] = Node.instance(nodeTypes[i]);
        }
        return instance(newNodes);
    }
    public static NodePath instance(NodePath parentPath, String nodeType) {
        return instance(parentPath, Node.instance(nodeType));
    }
    public static NodePath instance(NodePath parentPath, Node node) {
        LinkedList<Node> nodes = new LinkedList<>();
        if (parentPath != null) {
            nodes.addAll(parentPath.nodes);
        }
        if (node != null) {
            nodes.add(node);
        }
        return new NodePath(nodes);
    }

    /**
     * 包含的节点列表
     */
    private final List<Node> nodes;

    public NodePath(List<Node> nodes) {
        if (nodes == null) {
            throw new IllegalArgumentException("nodes can't be null");
        }
        int nullIndex = nodes.indexOf(null);
        if (nullIndex >= 0) {
            throw new IllegalArgumentException("nodes can't contain null value, but found in index '" + nullIndex + "'");
        }
        this.nodes = Collections.unmodifiableList(new LinkedList<>(nodes));
    }

    public List<Node> nodes() {
        return nodes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NodePath path = (NodePath) o;

        return nodes != null ? nodes.equals(path.nodes) : path.nodes == null;
    }

    @Override
    public int hashCode() {
        return nodes != null ? nodes.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "NodePath{" +
                "nodes=" + nodes +
                '}';
    }
}