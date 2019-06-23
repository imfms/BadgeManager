package ms.imf.redpoint.compiler.plugin;

import java.util.List;

import javax.lang.model.element.TypeElement;

public class PathEntity {

    public List<Node> nodes;
    public TypeElement host;

    public static class Node {
        public String type;
        public List<NodeArg> args;
        public List<Node> sub;
        public PathEntity subRef;
    }

    public static class NodeArg {
        public String name;
        public List<String> valueLimits;
    }

}
