package ms.imf.redpoint.entity;

import java.util.List;

public class NodeSchema {

    public NodeSchema() {
    }

    public NodeSchema(String type, List<NodeArg> args, List<NodeSchema> sub) {
        this.type = type;
        this.args = args;
        this.sub = sub;
    }

    public String type;
    public List<NodeArg> args;
    public List<NodeSchema> sub;

    public static class NodeArg {
        public String name;
        public List<String> valueLimits;

        @Override
        public String toString() {
            return "NodeArg{" +
                    "name='" + name + '\'' +
                    ", valueLimits=" + valueLimits +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "NodeSchema{" +
                "type='" + type + '\'' +
                ", args=" + args +
                ", sub=" + sub +
                '}';
    }
}