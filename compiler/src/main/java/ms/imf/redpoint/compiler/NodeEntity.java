package ms.imf.redpoint.compiler;

import java.util.List;

class NodeEntity {
    String type;
    List<String> args;
    List<NodeEntity> sub;
    PathEntity subRef;
}
