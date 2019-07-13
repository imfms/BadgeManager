package ms.imf.redpoint.manager;

import java.io.Serializable;

import ms.imf.redpoint.entity.NodePath;

public class Remind implements Serializable {

    public final int num;
    public final NodePath path;

    public Remind(int num, NodePath path) {
        this.num = num;
        if (path == null) { throw new IllegalArgumentException("path can't be null"); }
        this.path = path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Remind remind = (Remind) o;

        if (num != remind.num) return false;
        return path != null ? path.equals(remind.path) : remind.path == null;
    }

    @Override
    public int hashCode() {
        int result = num;
        result = 31 * result + (path != null ? path.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Remind{" +
                "num=" + num +
                ", path=" + path +
                '}';
    }
}
