package ms.imf.redpoint.entity;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Node implements Serializable {

    public final String type;
    public final Map<String, String> args;

    public Node(String type) {
        this(type, null);
    }

    public Node(String type, Map<String, String> args) {

        if (type == null) {
            throw new IllegalArgumentException("type can't be null");
        }

        if (args != null) {
            int index = -1;
            for (Map.Entry<String, String> entry : args.entrySet()) {
                index++;
                if (entry.getKey() == null) {
                    throw new IllegalArgumentException("args' key can't be null, but found null key on index '" + index + "'");
                }
            }
        }

        this.type = type;
        this.args = args != null
                ? Collections.unmodifiableMap(new HashMap<>(args))
                : null;
    }

    @Override
    public String toString() {
        return "Node{" +
                "type='" + type + '\'' +
                ", args=" + args +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Node node = (Node) o;

        if (type != null ? !type.equals(node.type) : node.type != null) return false;
        return args != null ? args.equals(node.args) : node.args == null;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (args != null ? args.hashCode() : 0);
        return result;
    }
}