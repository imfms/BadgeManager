package ms.imf.redpoint.entity;

import java.util.List;

public class NodeTree {

    public NodeTree() {
    }

    public NodeTree(String type, List<Arg> args, List<NodeTree> sub) {
        this.type = type;
        this.args = args;
        this.sub = sub;
    }

    public String type;
    public List<Arg> args;
    public List<NodeTree> sub;

    public static class Arg {
        public String name;
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
                "type='" + type + '\'' +
                ", args=" + args +
                ", sub=" + sub +
                '}';
    }
}