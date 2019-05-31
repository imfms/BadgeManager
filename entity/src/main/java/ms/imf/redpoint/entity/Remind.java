package ms.imf.redpoint.entity;

import java.io.Serializable;

public class Remind implements Serializable {

    public static Remind instance(long id, int num, NodePath path) {
        return new Remind(id, num, path);
    }

    public final long id;
    public final int num;
    public final NodePath path;

    public Remind(long id, int num, NodePath path) {
        this.id = id;
        this.num = num;
        if (path == null) { throw new IllegalArgumentException("path can't be null"); }
        this.path = path;
    }

    @Override
    public String toString() {
        return "Remind{" +
                "id=" + id +
                ", num=" + num +
                ", path=" + path +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Remind remind = (Remind) o;

        if (id != remind.id) return false;
        if (num != remind.num) return false;
        return path != null ? path.equals(remind.path) : remind.path == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + num;
        result = 31 * result + (path != null ? path.hashCode() : 0);
        return result;
    }
}
