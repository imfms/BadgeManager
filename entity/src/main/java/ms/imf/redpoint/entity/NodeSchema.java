package ms.imf.redpoint.entity;

import java.util.List;

public class NodeSchema {

    public NodeSchema() {
    }

    public NodeSchema(String type, List<String> args, List<NodeSchema> sub) {
        this.type = type;
        this.args = args;
        this.sub = sub;
    }

    public String type;
    public List<String> args;
    public List<NodeSchema> sub;
}