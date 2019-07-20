package ms.imf.redpoint.entity;

import java.util.List;

/**
 * 节点树，用于描述支持的节点结构
 */
public class NodeTree {

    public NodeTree() {
    }

    public NodeTree(String name, List<Arg> args, List<NodeTree> sub) {
        this.name = name;
        this.args = args;
        this.sub = sub;
    }

    /**
     * 节点名
     */
    public String name;
    /**
     * 支持参数
     */
    public List<Arg> args;
    /**
     * 子节点列表
     */
    public List<NodeTree> sub;

    /**
     * 节点参数
     */
    public static class Arg {
        /**
         * 参数名
         */
        public String name;
        /**
         * 参数支持的值，如果为null或empty则代表对支持值没有限制
         */
        public List<String> valueLimits;

        @Override
        public String toString() {
            return "Arg{" +
                    "name='" + name + '\'' +
                    ", valueLimits=" + valueLimits +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "NodeTree{" +
                "name='" + name + '\'' +
                ", args=" + args +
                ", sub=" + sub +
                '}';
    }
}