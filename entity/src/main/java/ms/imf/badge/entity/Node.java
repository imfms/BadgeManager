package ms.imf.badge.entity;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 节点
 */
public class Node implements Serializable {

    public static Node instance(String type, String... argValues) {
        HashMap<String, String> args = null;
        if (argValues != null
                && argValues.length > 0) {
            args = new HashMap<>(argValues.length / 2);

            for (int i = 0; i < argValues.length; i += 2) {
                String key = argValues[i];
                String value = null;
                if (i + 1 < argValues.length) {
                    value = argValues[i + 1];
                }
                args.put(key, value);
            }
        }
        return instance(type, args);
    }

    public static Node instance(String type, Map<String, String> args) {
        return new Node(type, args);
    }

    public static Node instance(String type) {
        return instance(type, (String[]) null);
    }

    /**
     * 节点名
     */
    public final String name;
    /**
     * 节点参数/参数值
     */
    public final Map<String, String> args;

    public Node(String name) {
        this(name, null);
    }

    public Node(String name, Map<String, String> args) {
        if (name == null) { throw new IllegalArgumentException("name can't be null"); }

        if (args != null) {
            int index = -1;
            for (Map.Entry<String, String> entry : args.entrySet()) {
                index++;
                if (entry.getKey() == null) {
                    throw new IllegalArgumentException(String.format("args' key can't be null, but found null key on index '%d'", index));
                }
            }
        }

        this.name = name;
        this.args = args == null
                ? Collections.<String, String>emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(args));
    }

    @Override
    public String toString() {
        return "Node{" +
                "name='" + name + '\'' +
                ", args=" + args +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Node node = (Node) o;

        if (name != null ? !name.equals(node.name) : node.name != null) return false;
        return args != null ? args.equals(node.args) : node.args == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (args != null ? args.hashCode() : 0);
        return result;
    }
}