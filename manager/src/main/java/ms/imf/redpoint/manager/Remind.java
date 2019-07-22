package ms.imf.redpoint.manager;

import java.io.Serializable;

import ms.imf.redpoint.entity.NodePath;

/**
 * 消息提醒，代表一条消息
 */
public class Remind implements Serializable {

    /**
     * 消息节点路径
     */
    private final NodePath path;

    public Remind(NodePath path) {
        if (path == null) { throw new IllegalArgumentException("path can't be null"); }
        this.path = path;
    }

    public NodePath path() {
        return path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Remind remind = (Remind) o;

        return path != null ? path.equals(remind.path) : remind.path == null;
    }

    @Override
    public int hashCode() {
        return path != null ? path.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Remind{" +
                "path=" + path +
                '}';
    }
}
