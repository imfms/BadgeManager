package ms.imf.redpoint.compiler.plugin;

import java.util.List;

import javax.lang.model.element.TypeElement;

public class PathEntity {

    public List<Node> nodes;
    public TypeElement host;

    public static class Node {
        public String type;
        public List<String> args;
        public List<Node> sub;
        public PathEntity subRef;
    }

}
