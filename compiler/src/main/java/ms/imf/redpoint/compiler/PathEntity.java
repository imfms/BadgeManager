package ms.imf.redpoint.compiler;

import java.util.List;

import javax.lang.model.element.TypeElement;

class PathEntity {

    List<NodeEntity> nodes;
    TypeElement host;

    static class NodeEntity {
        String type;
        List<String> args;
        List<NodeEntity> sub;
        PathEntity subRef;
    }

}
