package ms.imf.redpoint.compiler.plugin;

import java.util.List;

import javax.lang.model.element.TypeElement;

public class NodeContainerAnnotationEntity {

    public List<Node> nodes;
    public TypeElement host;

    public static class Node {
        public String type;
        public List<Arg> args;
        public List<Node> sub;
        public NodeContainerAnnotationEntity subRef;

        public static class Arg {
            public String name;
            public List<String> valueLimits;
        }
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
