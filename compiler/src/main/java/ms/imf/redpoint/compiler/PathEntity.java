package ms.imf.redpoint.compiler;

import java.util.List;

import javax.lang.model.element.TypeElement;

class PathEntity {

    List<Node> nodes;
    TypeElement host;

    static class Node {
        String type;
        List<String> args;
        List<Node> sub;
        PathEntity subRef;
    }

}
