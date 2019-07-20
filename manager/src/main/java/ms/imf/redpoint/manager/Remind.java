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
    private final NodePath nodePath;

    public Remind(NodePath nodePath) {
        if (nodePath == null) { throw new IllegalArgumentException("nodePath can't be null"); }
        this.nodePath = nodePath;
    }

    public NodePath nodePath() {
        return nodePath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Remind remind = (Remind) o;

        return nodePath != null ? nodePath.equals(remind.nodePath) : remind.nodePath == null;
    }

    @Override
    public int hashCode() {
        return nodePath != null ? nodePath.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Remind{" +
                "nodePath=" + nodePath +
                '}';
    }
}
