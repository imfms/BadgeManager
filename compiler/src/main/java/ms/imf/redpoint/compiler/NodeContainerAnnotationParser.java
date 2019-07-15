package ms.imf.redpoint.compiler;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;

import ms.imf.redpoint.annotation.Arg;
import ms.imf.redpoint.annotation.NodeContainer;
import ms.imf.redpoint.annotation.SubNode;
import ms.imf.redpoint.compiler.plugin.AptProcessException;
import ms.imf.redpoint.compiler.plugin.NodeContainerAnnotationEntity;
import ms.imf.redpoint.entity.NodeTree;

class NodeContainerAnnotationParser {

    private final Gson gson = new Gson();
    private final Elements elementUtil;

    /**
     * link sub node and reusing node
     */
    private final Map<TypeElement, NodeContainerAnnotationEntity> pathEntityPoll = new HashMap<>();

    /**
     * check circular reference
     */
    private final List<TypeElement> parsingTypeElements = new LinkedList<>();

    NodeContainerAnnotationParser(Elements elementUtil) {
        if (elementUtil == null) {
            throw new IllegalArgumentException("elementUtil can't be null");
        }
        this.elementUtil = elementUtil;
    }

    synchronized List<NodeContainerAnnotationEntity> parsePaths(Set<TypeElement> annotatedPathTypeElements) throws AptProcessException {
        
        pathEntityPoll.clear();
        parsingTypeElements.clear();

        final List<NodeContainerAnnotationEntity> results = new LinkedList<>();

        try {
            for (TypeElement typeElement : annotatedPathTypeElements) {
                try {
                    results.add(
                            parsePath(typeElement)
                    );
                } catch (AptProcessException e) {
                    throw new AptProcessException(
                            String.format(
                                    "%s: %s",
                                    typeElement.getQualifiedName(), e.getMessage()
                            ),
                            e
                    );
                }
            }
        } finally {
            pathEntityPoll.clear();
            parsingTypeElements.clear();
        }

        return results;
    }

    private NodeContainerAnnotationEntity parsePath(TypeElement annotatedPathTypeElement) throws AptProcessException {

        NodeContainer nodeContainer = annotatedPathTypeElement.getAnnotation(NodeContainer.class);
        if (nodeContainer == null) {
            throw new AptProcessException("can't find NodeContainer Annotation", annotatedPathTypeElement);
        }
        AnnotationMirror pathMirror = null;
        for (AnnotationMirror annotationMirror : annotatedPathTypeElement.getAnnotationMirrors()) {
            if (annotationMirror.getAnnotationType().equals(elementUtil.getTypeElement(NodeContainer.class.getCanonicalName()).asType())) {
                pathMirror = annotationMirror;
            }
        }
        if (pathMirror == null) {
            throw new AptProcessException("can't find NodeContainer Annotation", annotatedPathTypeElement);
        }
        return parsePath(annotatedPathTypeElement, nodeContainer, pathMirror);
    }

    private synchronized NodeContainerAnnotationEntity parsePath(TypeElement annotatedPathTypeElement, NodeContainer nodeContainer, AnnotationMirror pathMirror) throws AptProcessException {

        // check circular reference
        if (parsingTypeElements.contains(annotatedPathTypeElement)) {
            List<TypeElement> copyParsingTypeElements = new LinkedList<>(parsingTypeElements);
            copyParsingTypeElements.add(annotatedPathTypeElement);

            // A>B>C>B: [A,#B,C,#B]
            final StringBuilder stack = new StringBuilder();
            stack.append('[');
            Iterator<TypeElement> iterator = copyParsingTypeElements.iterator();
            while (iterator.hasNext()) {
                TypeElement stackElement = iterator.next();

                boolean isCircle = stackElement == annotatedPathTypeElement;
                if (isCircle) { stack.append('*'); }

                stack.append(stackElement.getQualifiedName());

                if (isCircle) { stack.append('*'); }

                if (iterator.hasNext()) {
                    stack.append(',').append(' ');
                }
            }
            stack.append(']');

            throw new AptProcessException(
                    String.format(
                            "circular reference, this is stack: %s",
                            stack
                    ),
                    annotatedPathTypeElement,
                    pathMirror
            );
        }

        // reuse parsed nodeContainer entity
        final NodeContainerAnnotationEntity pollNodeTreeEntity = pathEntityPoll.get(annotatedPathTypeElement);
        if (pollNodeTreeEntity != null) {
            return pollNodeTreeEntity;
        }

        // use for check circular reference
        parsingTypeElements.add(annotatedPathTypeElement);
        final NodeContainerAnnotationEntity resultNodeTreeEntity;
        try {
            // raw parse
            resultNodeTreeEntity = parsePathRaw(annotatedPathTypeElement, nodeContainer, pathMirror);
        } finally {
            // use for check circular reference
            parsingTypeElements.remove(annotatedPathTypeElement);
        }

        // use for reuse parsed nodeContainer entity
        pathEntityPoll.put(annotatedPathTypeElement, resultNodeTreeEntity);

        return resultNodeTreeEntity;
    }

    private NodeContainerAnnotationEntity parsePathRaw(TypeElement annotatedPathTypeElement, NodeContainer nodeContainer, AnnotationMirror pathMirror) throws AptProcessException {

        final boolean nodeMode = nodeContainer.value().length > 0;
        final boolean jsonMode = nodeContainer.nodeJson().length > 0;

        final List<NodeContainerAnnotationEntity.Node> nodeEntities = new LinkedList<>();

        if (nodeMode) {
            try {
                nodeEntities.addAll(pathNodeToNodeEntity(annotatedPathTypeElement, nodeContainer, pathMirror));
            } catch (AptProcessException e) {
                throw new AptProcessException(String.format("value(nodes): %s", e.getMessage()), e);
            }
        }
        if (jsonMode) {
            try {
                nodeEntities.addAll(pathNodeJsonToNodeEntity(annotatedPathTypeElement, nodeContainer, pathMirror));
            } catch (AptProcessException e) {
                throw new AptProcessException(String.format("nodeJson: %s", e.getMessage()), e);
            }
        }

        // check repeat value
        final Set<String> repeatElements = new HashSet<>();
        for (NodeContainerAnnotationEntity.Node nodeEntity : nodeEntities) {
            if (!repeatElements.add(nodeEntity.name)) {
                throw new AptProcessException(
                        String.format("repeat node name in nodes and nodeJson: %s", nodeEntity.name),
                        annotatedPathTypeElement, pathMirror
                );
            }
        }

        NodeContainerAnnotationEntity nodeTreeEntity = new NodeContainerAnnotationEntity();
        nodeTreeEntity.nodes = nodeEntities;
        nodeTreeEntity.host = annotatedPathTypeElement;
        return nodeTreeEntity;
    }

    private List<NodeContainerAnnotationEntity.Node> pathNodeJsonToNodeEntity(TypeElement annotatedPathTypeElement, NodeContainer nodeContainer, AnnotationMirror pathMirror) throws AptProcessException {
        final List<NodeContainerAnnotationEntity.Node> results = new LinkedList<>();

        final Map<String, TypeElement> nodeJsonRefTypeMapper = new HashMap<>();
        List<AnnotationMirror> nodesJsonRefClassMappers = NodeContainerAnnotationParser.<List<AnnotationMirror>>getAnnotionMirrorValue(pathMirror, "nodeJsonRefContainerMapper"/* todo runtime check */);
        if (nodesJsonRefClassMappers == null) {
            nodesJsonRefClassMappers = Collections.emptyList();
        }

        ListIterator<AnnotationMirror> mapperIterator = nodesJsonRefClassMappers.listIterator();
        while (mapperIterator.hasNext()) {
            int index = mapperIterator.nextIndex();
            AnnotationMirror mapperMirror = mapperIterator.next();

            String key = NodeContainerAnnotationParser.getAnnotionMirrorValue(mapperMirror, "key" /* todo runtime check */);
            TypeElement value = (TypeElement) NodeContainerAnnotationParser.<DeclaredType>getAnnotionMirrorValue(mapperMirror, "value" /* todo runtime check */).asElement();
            if (nodeJsonRefTypeMapper.put(key, value) != null) {
                throw new AptProcessException(
                        String.format("subNodeRef[%d]: key(%s): repeat key", index, key),
                        annotatedPathTypeElement,
                        pathMirror
                );
            }
        }

        for (int i = 0; i < nodeContainer.nodeJson().length; i++) {
            String nodeJson = nodeContainer.nodeJson()[i];
            try {
                results.add(
                        nodeJsonToNodeEntity(annotatedPathTypeElement, nodeJson, nodeJsonRefTypeMapper)
                );
            } catch (AptProcessException e) {
                throw new AptProcessException(
                        String.format("[%d]: %s", i, e.getMessage()),
                        e
                );
            }
        }

        return results;
    }

    private List<NodeContainerAnnotationEntity.Node> pathNodeToNodeEntity(TypeElement annotatedPathTypeElement, NodeContainer nodeContainer, AnnotationMirror pathMirror) throws AptProcessException {
        final LinkedList<NodeContainerAnnotationEntity.Node> results = new LinkedList<>();

        final List<AnnotationMirror> nodeMirrors = NodeContainerAnnotationParser.getAnnotionMirrorValue(pathMirror, "value" /* todo runtime check */);

        for (int i = 0; i < nodeContainer.value().length; i++) {
            SubNode node = nodeContainer.value()[i];
            AnnotationMirror nodeMirror = nodeMirrors.get(i);
            try {
                NodeParseEntity nodeParseEntity = subNodeWrapperConvertToNodeParseEntity(SubNodeWrapperType.instance(node, nodeMirror));
                results.add(
                        nodeParseEntityToPathNodeEntity(annotatedPathTypeElement, nodeParseEntity)
                );
            } catch (AptProcessException e) {
                throw new AptProcessException(
                        String.format("[%s]: %s", i, e.getMessage()),
                        e, annotatedPathTypeElement, nodeMirror
                );
            }
        }

        return results;
    }

    private <Source extends Annotation, Sub extends Annotation> NodeParseEntity subNodeWrapperConvertToNodeParseEntity(SubNodeWrapperType<Source, Sub> subNodeWrapperType) {

        NodeParseEntity nodeParseEntity = new NodeParseEntity();

        nodeParseEntity.name = subNodeWrapperType.name();

        if (subNodeWrapperType.args() != null) {
            nodeParseEntity.args = new ArrayList<>(subNodeWrapperType.args().length);

            for (Arg sourceArg : subNodeWrapperType.args()) {

                NodeParseEntity.NodeArg targetArg = new NodeParseEntity.NodeArg();
                targetArg.name = sourceArg.value();
                targetArg.valueLimits = Arrays.asList(sourceArg.valueLimits());

                nodeParseEntity.args.add(targetArg);
            }
        }

        nodeParseEntity.subNodeRef = subNodeWrapperType.subRef();

        Sub[] subNodes = subNodeWrapperType.subNodes();

        if (subNodes != null) {
            nodeParseEntity.subNodes = new ArrayList<>(subNodes.length);
            List<AnnotationMirror> subNodeMirrors = subNodeWrapperType.subNodeMirrors();
            for (int i = 0; i < subNodes.length; i++) {
                Sub subNode = subNodes[i];
                AnnotationMirror subNodeMirror = subNodeMirrors.get(i);

                nodeParseEntity.subNodes.add(
                        subNodeWrapperConvertToNodeParseEntity(
                                subNodeWrapperType.subNodeWrapper(subNode, subNodeMirror)
                        )
                );
            }
        }

        return nodeParseEntity;
    }

    private NodeContainerAnnotationEntity.Node nodeJsonToNodeEntity(TypeElement annotatedPathTypeElement, String nodeJson, Map<String, TypeElement> nodeJsonRefTypeMapper) throws AptProcessException {
        // parse json
        JsonNode jsonNodeObj;
        try {
            jsonNodeObj = gson.fromJson(nodeJson, JsonNode.class);
        } catch (Exception e) {
            throw new AptProcessException(String.format("error on parsing json: %s", e.getMessage()), e, annotatedPathTypeElement);
        }

        // convert to parseEntity
        NodeParseEntity nodeParseEntity;
        try {
            nodeParseEntity = jsonNodeConvertToNodeParseEntity(jsonNodeObj, nodeJsonRefTypeMapper);
        } catch (AptProcessException e) {
            throw new AptProcessException(e.getMessage(), e, annotatedPathTypeElement);
        }

        return nodeParseEntityToPathNodeEntity(annotatedPathTypeElement, nodeParseEntity);
    }

    private NodeParseEntity jsonNodeConvertToNodeParseEntity(JsonNode jsonNode, Map<String, TypeElement> nodeJsonRefTypeMapper) throws AptProcessException {

        NodeParseEntity nodeParseEntity = new NodeParseEntity();

        nodeParseEntity.name = jsonNode.name;

        if (jsonNode.args != null
                && jsonNode.args.length > 0) {

            int nullIndex = Arrays.asList(jsonNode.args).indexOf(null);
            if (nullIndex >= 0) {
                throw new AptProcessException(
                        String.format(
                                "args[%d]: null value",
                                nullIndex
                        )
                );
            }

            nodeParseEntity.args = new ArrayList<>(jsonNode.args.length);

            for (JsonNode.Arg sourceArg : jsonNode.args) {
                NodeParseEntity.NodeArg targetArg = new NodeParseEntity.NodeArg();
                targetArg.name = sourceArg.name;
                if (sourceArg.limits != null) {
                    targetArg.valueLimits = Arrays.asList(sourceArg.limits);
                }

                nodeParseEntity.args.add(targetArg);
            }
        }

        if (jsonNode.subNodes != null
                && !jsonNode.subNodes.isEmpty()) {
            nodeParseEntity.subNodes = new LinkedList<>();
            for (int i = 0; i < jsonNode.subNodes.size(); i++) {
                JsonNode subJsonNode = jsonNode.subNodes.get(i);

                NodeParseEntity subNodeParseEntity;
                try {
                    subNodeParseEntity = jsonNodeConvertToNodeParseEntity(subJsonNode, nodeJsonRefTypeMapper);
                } catch (AptProcessException e) {
                    throw new AptProcessException(
                            String.format(
                                    "subNodes[%d](%s): %s",
                                    i, subJsonNode.name, e.getMessage()
                            ),
                            e
                    );
                }

                nodeParseEntity.subNodes.add(subNodeParseEntity);
            }
        }

        if (jsonNode.subNodeRef != null) {
            TypeElement typeElement = nodeJsonRefTypeMapper.get(jsonNode.subNodeRef);
            if (typeElement == null) {
                throw new AptProcessException(
                        String.format(
                                "subNodeRef(%s): can't find refClass in nodeJsonRefTypeMapper",
                                jsonNode.subNodeRef
                        )
                );
            }
            nodeParseEntity.subNodeRef = typeElement;
        }

        return nodeParseEntity;
    }

    private NodeContainerAnnotationEntity.Node nodeParseEntityToPathNodeEntity(TypeElement annotatedPathTypeElement, NodeParseEntity nodeParseEntity) throws AptProcessException {
        final NodeContainerAnnotationEntity.Node resultNodeEntity = new NodeContainerAnnotationEntity.Node();

        // name convert
        if (nodeParseEntity.name == null
                || nodeParseEntity.name.isEmpty()) {
            throw new AptProcessException("name can't be null", annotatedPathTypeElement);
        }
        resultNodeEntity.name = nodeParseEntity.name;

        // args convert
        if (nodeParseEntity.args != null) {

            // check null arg
            int nullIndex = nodeParseEntity.args.indexOf(null);
            if (nullIndex >= 0) {
                throw new AptProcessException(
                        String.format("args[%d]: null value", nullIndex),
                        annotatedPathTypeElement
                );
            }

            // check repeat
            final Set<String> repeatArgNames = new HashSet<>();
            ListIterator<NodeParseEntity.NodeArg> argIterator = nodeParseEntity.args.listIterator();
            while (argIterator.hasNext()) {
                int index = argIterator.nextIndex();
                NodeParseEntity.NodeArg arg = argIterator.next();

                // check arg name repeat
                if (!repeatArgNames.add(arg.name)) {
                    throw new AptProcessException(String.format("args[%d](%s): repeat arg", index, arg.name), annotatedPathTypeElement);
                }

                // arg valueLimits
                if (arg.valueLimits != null) {

                    int valueLimitNullIndex = arg.valueLimits.indexOf(null);
                    if (valueLimitNullIndex >= 0) {
                        throw new AptProcessException(
                                String.format(
                                        "args[%d](%s): valueLimits[%d]: null value",
                                        index, arg.name, valueLimitNullIndex
                                ),
                                annotatedPathTypeElement
                        );
                    }

                    final Set<String> repeatValueLimits = new HashSet<>();

                    ListIterator<String> valueLimitIterator = arg.valueLimits.listIterator();

                    while (valueLimitIterator.hasNext()) {
                        int valueLimitIndex = valueLimitIterator.nextIndex();
                        String valueLimit = valueLimitIterator.next();

                        if (!repeatValueLimits.add(valueLimit)) {
                            throw new AptProcessException(
                                    String.format("args[%d](%s): valueLimits[%d](%s): repeat valueLimit value",
                                            index,
                                            arg.name,
                                            valueLimitIndex,
                                            valueLimit
                                    ),
                                    annotatedPathTypeElement
                            );
                        }

                    }
                }
            }

            // convert arg
            resultNodeEntity.args = new ArrayList<>(nodeParseEntity.args.size());
            for (NodeParseEntity.NodeArg sourceArg : nodeParseEntity.args) {
                NodeContainerAnnotationEntity.Node.Arg targetArg = new NodeContainerAnnotationEntity.Node.Arg();
                targetArg.name = sourceArg.name;
                targetArg.valueLimits = sourceArg.valueLimits;

                resultNodeEntity.args.add(targetArg);
            }
        }

        // check subNode
        boolean existSubNodes = nodeParseEntity.subNodes != null && !nodeParseEntity.subNodes.isEmpty();
        boolean existSubNodeRef = nodeParseEntity.subNodeRef != null && !nodeParseEntity.subNodeRef.getQualifiedName().toString().equals(Void.class.getCanonicalName());
        if (existSubNodes && existSubNodeRef) {
            throw new AptProcessException("subNodes and subNodeContainerRef only can exist one", annotatedPathTypeElement);
        }

        // parse subNodes
        if (existSubNodes) {
            final List<NodeContainerAnnotationEntity.Node> subNodes = new LinkedList<>();
            for (int i = 0; i < nodeParseEntity.subNodes.size(); i++) {
                NodeParseEntity subEntity = nodeParseEntity.subNodes.get(i);
                try {
                    subNodes.add(
                            nodeParseEntityToPathNodeEntity(annotatedPathTypeElement, subEntity)
                    );
                } catch (AptProcessException e) {
                    throw new AptProcessException(
                            String.format(
                                    "subNodes[%d](%s): %s",
                                    i, subEntity.name, e.getMessage()
                            ),
                            e
                    );
                }
            }
            resultNodeEntity.sub = subNodes;
        }

        // parse subNodeRef
        if (existSubNodeRef) {
            try {
                resultNodeEntity.subRef = parsePath(nodeParseEntity.subNodeRef);
            } catch (AptProcessException e) {
                throw new AptProcessException(
                        String.format(
                                "subNodeContainerRef(%s): %s",
                                nodeParseEntity.subNodeRef.getQualifiedName(), e.getMessage()
                        ),
                        e
                );
            }
        }

        return resultNodeEntity;
    }

    /**
     * @see NodeContainer#nodeJson()
     */
    private static class JsonNode {
        @SerializedName("name")
        String name;
        @SerializedName("args")
        Arg[] args;
        @SerializedName("subNodes")
        List<JsonNode> subNodes;
        @SerializedName("subNodeRef")
        String subNodeRef;

        static class Arg {
            @SerializedName("name")
            String name;
            @SerializedName("limits")
            String[] limits;
        }
    }

    private static class NodeParseEntity {
        public String name;
        public List<NodeArg> args;
        public List<NodeParseEntity> subNodes;
        public TypeElement subNodeRef;

        public static class NodeArg {
            public String name;
            public List<String> valueLimits;
        }
    }

    static <T> T getAnnotionMirrorValue(AnnotationMirror annotationMirror, String key) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationMirror.getElementValues().entrySet()) {
            if (entry.getKey().getSimpleName().toString().equals(key)) {
                return (T) entry.getValue().getValue();
            }
        }
        return null;
    }

    /**
     * 转换扁平路径列表为树型路径，移除独立路径
     *
     * @param pathEntities source
     * @return target
     */
    public static List<NodeContainerAnnotationEntity> convertPathTree(List<NodeContainerAnnotationEntity> pathEntities) {
        final List<NodeContainerAnnotationEntity> treePathEntities = new LinkedList<>(pathEntities);

        treePathEntities.removeAll(
                getTreeNeedDeleteNodeEntity(pathEntities)
        );

        return treePathEntities;
    }

    /**
     * 生成树型节点规则
     *
     * @param pathEntities source
     * @return target
     */
    public static List<NodeTree> generateNodeSchemaTree(List<NodeContainerAnnotationEntity> pathEntities) {

        final List<NodeContainerAnnotationEntity> treePathEntities = convertPathTree(pathEntities);
        final List<NodeTree> result = new LinkedList<>();

        for (NodeContainerAnnotationEntity nodeTreeEntity : treePathEntities) {
            if (nodeTreeEntity.nodes == null
                    || nodeTreeEntity.nodes.isEmpty()) {
                continue;
            }
            result.addAll(convertNodeEntitiesToNodes(nodeTreeEntity.nodes));
        }

        return result;
    }


    private static Set<NodeContainerAnnotationEntity> getTreeNeedDeleteNodeEntity(List<NodeContainerAnnotationEntity> pathEntities) {
        final Set<NodeContainerAnnotationEntity> deleteElementContainer = new HashSet<>();

        for (NodeContainerAnnotationEntity nodeTreeEntity : pathEntities) {
            getTreeNeedDeleteNodeEntity(nodeTreeEntity, deleteElementContainer);
        }

        return deleteElementContainer;
    }

    private static void getTreeNeedDeleteNodeEntity(NodeContainerAnnotationEntity nodeTreeEntity, Set<NodeContainerAnnotationEntity> deleteElementContainer) {
        for (NodeContainerAnnotationEntity.Node node : nodeTreeEntity.nodes) {
            if (node.subRef != null) {
                deleteElementContainer.add(node.subRef);
                getTreeNeedDeleteNodeEntity(node.subRef, deleteElementContainer);
            }
            if (node.sub != null) {
                getTreeNeedDeleteNodeEntity(node.sub, deleteElementContainer);
            }
        }
    }

    private static void getTreeNeedDeleteNodeEntity(List<NodeContainerAnnotationEntity.Node> nodeEntities, Set<NodeContainerAnnotationEntity> deleteElementContainer) {
        for (NodeContainerAnnotationEntity.Node nodeEntity : nodeEntities) {
            if (nodeEntity.subRef != null) {
                deleteElementContainer.add(nodeEntity.subRef);
                getTreeNeedDeleteNodeEntity(nodeEntity.subRef, deleteElementContainer);
            }
            if (nodeEntity.sub != null) {
                getTreeNeedDeleteNodeEntity(nodeEntity.sub, deleteElementContainer);
            }
        }
    }

    private static List<NodeTree> convertNodeEntitiesToNodes(List<NodeContainerAnnotationEntity.Node> nodeEntities) {
        List<NodeTree> convertedSubNodeEntities = new ArrayList<>(nodeEntities.size());

        for (NodeContainerAnnotationEntity.Node subNodeEntity : nodeEntities) {
            convertedSubNodeEntities.add(
                    convertNodeEntityToNode(subNodeEntity)
            );
        }

        return convertedSubNodeEntities;
    }

    private static NodeTree convertNodeEntityToNode(NodeContainerAnnotationEntity.Node nodeEntity) {

        NodeTree nodeTree = new NodeTree();

        nodeTree.name = nodeEntity.name;

        if (nodeEntity.args != null) {
            nodeTree.args = new ArrayList<>(nodeEntity.args.size());

            for (NodeContainerAnnotationEntity.Node.Arg sourceArg : nodeEntity.args) {
                NodeTree.Arg targetArg = new NodeTree.Arg();
                targetArg.name = sourceArg.name;
                targetArg.valueLimits = sourceArg.valueLimits;

                nodeTree.args.add(targetArg);
            }

        }

        List<NodeContainerAnnotationEntity.Node> subNodeEntities = null;
        if (nodeEntity.sub != null) {
            subNodeEntities = nodeEntity.sub;
        }
        if (nodeEntity.subRef != null) {
            subNodeEntities = nodeEntity.subRef.nodes;
        }
        if (subNodeEntities != null
                && !subNodeEntities.isEmpty()) {
            nodeTree.sub = convertNodeEntitiesToNodes(subNodeEntities);
        }

        return nodeTree;
    }
}
