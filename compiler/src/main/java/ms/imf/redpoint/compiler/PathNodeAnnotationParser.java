package ms.imf.redpoint.compiler;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;

import ms.imf.redpoint.annotation.NodeArg;
import ms.imf.redpoint.annotation.Path;
import ms.imf.redpoint.annotation.SubNode;
import ms.imf.redpoint.compiler.plugin.AptProcessException;
import ms.imf.redpoint.compiler.plugin.PathEntity;
import ms.imf.redpoint.entity.NodeSchema;

class PathNodeAnnotationParser {

    private final Gson gson = new Gson();
    private final Elements elementUtil;

    /**
     * link sub node and reusing node
     */
    private final Map<TypeElement, PathEntity> pathEntityPoll = new HashMap<>();

    /**
     * check circular reference
     */
    private final List<TypeElement> parsingTypeElements = new LinkedList<>();

    PathNodeAnnotationParser(Elements elementUtil) {
        if (elementUtil == null) {
            throw new IllegalArgumentException("elementUtil can't be null");
        }
        this.elementUtil = elementUtil;
    }

    synchronized List<PathEntity> parsePaths(Set<TypeElement> annotatedPathTypeElements) throws AptProcessException {
        
        pathEntityPoll.clear();
        parsingTypeElements.clear();

        final List<PathEntity> results = new LinkedList<>();

        try {
            for (TypeElement typeElement : annotatedPathTypeElements) {
                results.add(
                        parsePathWrapper(typeElement)
                );
            }
        } finally {
            pathEntityPoll.clear();
            parsingTypeElements.clear();
        }

        return results;
    }

    private PathEntity parsePathWrapper(TypeElement annotatedPathTypeElement) throws AptProcessException {
        try {
            return parsePath(annotatedPathTypeElement);
        } catch (AptProcessException e) {
            throw new AptProcessException(
                    String.format("found error on parse type %s's PathAnnotation: %s", annotatedPathTypeElement.getQualifiedName(), e.getMessage()),
                    e
            );
        }
    }

    private PathEntity parsePath(TypeElement annotatedPathTypeElement) throws AptProcessException {

        Path path = annotatedPathTypeElement.getAnnotation(Path.class);
        if (path == null) {
            throw new AptProcessException("can't find Path Annotation", annotatedPathTypeElement);
        }
        AnnotationMirror pathMirror = null;
        for (AnnotationMirror annotationMirror : annotatedPathTypeElement.getAnnotationMirrors()) {
            if (annotationMirror.getAnnotationType().equals(elementUtil.getTypeElement(Path.class.getCanonicalName()).asType())) {
                pathMirror = annotationMirror;
            }
        }
        if (pathMirror == null) {
            throw new AptProcessException("can't find Path Annotation", annotatedPathTypeElement);
        }
        return parsePath(annotatedPathTypeElement, path, pathMirror);
    }

    private synchronized PathEntity parsePath(TypeElement annotatedPathTypeElement, Path path, AnnotationMirror pathMirror) throws AptProcessException {

        // check circular reference
        if (parsingTypeElements.contains(annotatedPathTypeElement)) {
            final StringBuilder stack = new StringBuilder();
            List<TypeElement> copyParsingTypeElements = new LinkedList<>(parsingTypeElements);
            copyParsingTypeElements.add(annotatedPathTypeElement);
            for (TypeElement parsingTypeElement : copyParsingTypeElements) {
                stack.append('\n')
                        .append(parsingTypeElement.getQualifiedName());
            }
            throw new AptProcessException(
                    String.format(
                            "found Path circular reference on '%s', this is circular reference stack: %s",
                            annotatedPathTypeElement.getQualifiedName(),
                            stack
                    ),
                    annotatedPathTypeElement,
                    pathMirror
            );
        }

        final PathEntity pollPathEntity = pathEntityPoll.get(annotatedPathTypeElement);
        if (pollPathEntity != null) {
            return pollPathEntity;
        }

        // for check circular reference
        parsingTypeElements.add(annotatedPathTypeElement);
        final PathEntity resultPathEntity;
        try {
            // raw parse
            resultPathEntity = parsePathRaw(annotatedPathTypeElement, path, pathMirror);
        } finally {
            // for check circular reference
            parsingTypeElements.remove(annotatedPathTypeElement);
        }

        pathEntityPoll.put(annotatedPathTypeElement, resultPathEntity);
        return resultPathEntity;
    }

    private PathEntity parsePathRaw(TypeElement annotatedPathTypeElement, Path path, AnnotationMirror pathMirror) throws AptProcessException {

        final boolean nodeMode = path.value().length > 0;
        final boolean jsonMode = path.nodesJson().length > 0;

        final List<PathEntity.Node> nodeEntities = new LinkedList<>();

        if (nodeMode) {
            try {
                nodeEntities.addAll(pathNodeToNodeEntity(annotatedPathTypeElement, path, pathMirror));
            } catch (AptProcessException e) {
                throw new AptProcessException(String.format("found error on parse nodes: %s", e.getMessage()), e);
            }
        }
        if (jsonMode) {
            try {
                nodeEntities.addAll(pathNodeJsonToNodeEntity(annotatedPathTypeElement, path, pathMirror));
            } catch (AptProcessException e) {
                throw new AptProcessException(String.format("found error on parse nodesJson: %s", e.getMessage()), e);
            }
        }

        // check repeat type
        final Set<String> repeatElements = new HashSet<>();
        for (PathEntity.Node nodeEntity : nodeEntities) {
            if (!repeatElements.add(nodeEntity.type)) {
                throw new AptProcessException("find repeat type in nodes and nodesJson: " + nodeEntity.type, annotatedPathTypeElement, pathMirror);
            }
        }

        PathEntity pathEntity = new PathEntity();
        pathEntity.nodes = nodeEntities;
        pathEntity.host = annotatedPathTypeElement;
        return pathEntity;
    }

    private List<PathEntity.Node> pathNodeJsonToNodeEntity(TypeElement annotatedPathTypeElement, Path path, AnnotationMirror pathMirror) throws AptProcessException {
        final List<PathEntity.Node> results = new LinkedList<>();

        final Map<String, TypeElement> nodeJsonRefTypeMapper = new HashMap<>();
        List<AnnotationMirror> nodesJsonRefClassMappers = PathNodeAnnotationParser.<List<AnnotationMirror>>getAnnotionMirrorValue(pathMirror, "nodesJsonRefClassMapper");
        if (nodesJsonRefClassMappers == null) {
            nodesJsonRefClassMappers = Collections.emptyList();
        }
        for (AnnotationMirror mapperMirror : nodesJsonRefClassMappers) {
            String key = PathNodeAnnotationParser.getAnnotionMirrorValue(mapperMirror, "key");
            TypeElement value = (TypeElement) PathNodeAnnotationParser.<DeclaredType>getAnnotionMirrorValue(mapperMirror, "value").asElement();
            if (nodeJsonRefTypeMapper.put(key, value) != null) {
                throw new AptProcessException(
                        String.format("found repeat subNodeRef key: '%s'", key),
                        annotatedPathTypeElement,
                        pathMirror
                );
            }
        }

        for (int i = 0; i < path.nodesJson().length; i++) {
            String nodeJson = path.nodesJson()[i];
            try {
                results.add(nodeJsonToNodeEntity(annotatedPathTypeElement, nodeJson, nodeJsonRefTypeMapper));
            } catch (AptProcessException e) {
                throw new AptProcessException(
                        String.format("found error in parse nodeJson[%s]: %s", i, e.getMessage()),
                        e
                );
            }
        }

        return results;
    }

    private List<PathEntity.Node> pathNodeToNodeEntity(TypeElement annotatedPathTypeElement, Path path, AnnotationMirror pathMirror) throws AptProcessException {
        final LinkedList<PathEntity.Node> results = new LinkedList<>();

        final List<AnnotationMirror> nodeMirrors = PathNodeAnnotationParser.getAnnotionMirrorValue(pathMirror, "value");

        for (int i = 0; i < path.value().length; i++) {
            SubNode node = path.value()[i];
            AnnotationMirror nodeMirror = nodeMirrors.get(i);
            try {
                NodeParseEntity nodeParseEntity = subNodeWrapperConvertToNodeParseEntity(SubNodeWrapper.instance(node, nodeMirror));
                results.add(
                        nodeParseEntityToPathNodeEntity(annotatedPathTypeElement, nodeParseEntity)
                );
            } catch (AptProcessException e) {
                throw new AptProcessException(
                        String.format("found error in parse value[%s]: %s", i, e.getMessage()),
                        e, annotatedPathTypeElement, nodeMirror
                );
            }
        }

        return results;
    }

    private <Source extends Annotation, Sub extends Annotation> NodeParseEntity subNodeWrapperConvertToNodeParseEntity(SubNodeWrapper<Source, Sub> subNodeWrapper) {

        NodeParseEntity nodeParseEntity = new NodeParseEntity();

        nodeParseEntity.type = subNodeWrapper.type();

        if (subNodeWrapper.args() != null) {
            nodeParseEntity.args = new ArrayList<>(subNodeWrapper.args().length);

            for (NodeArg sourceArg : subNodeWrapper.args()) {

                NodeParseEntity.NodeArg targetArg = new NodeParseEntity.NodeArg();
                targetArg.name = sourceArg.value();
                targetArg.valueLimits = Arrays.asList(sourceArg.valueLimits());

                nodeParseEntity.args.add(targetArg);
            }
        }

        nodeParseEntity.subNodeRef = subNodeWrapper.subRef();

        Sub[] subNodes = subNodeWrapper.subNodes();

        if (subNodes != null) {
            nodeParseEntity.subNodes = new ArrayList<>(subNodes.length);
            List<AnnotationMirror> subNodeMirrors = subNodeWrapper.subNodeMirrors();
            for (int i = 0; i < subNodes.length; i++) {
                Sub subNode = subNodes[i];
                AnnotationMirror subNodeMirror = subNodeMirrors.get(i);

                nodeParseEntity.subNodes.add(
                        subNodeWrapperConvertToNodeParseEntity(
                                subNodeWrapper.subNodeWrapper(subNode, subNodeMirror)
                        )
                );
            }
        }

        return nodeParseEntity;
    }

    private PathEntity.Node nodeJsonToNodeEntity(TypeElement annotatedPathTypeElement, String nodeJson, Map<String, TypeElement> nodeJsonRefTypeMapper) throws AptProcessException {
        // parse json
        JsonNode jsonNodeObj;
        try {
            jsonNodeObj = gson.fromJson(nodeJson, JsonNode.class);
        } catch (Exception e) {
            throw new AptProcessException(String.format("nodeJson parse error: %s", e.getMessage()), e, annotatedPathTypeElement);
        }

        // convert to parseEntity
        NodeParseEntity nodeParseEntity;
        try {
            nodeParseEntity = jsonNodeConvertToNodeParseEntity(jsonNodeObj, nodeJsonRefTypeMapper);
        } catch (AptProcessException e) {
            throw new AptProcessException(String.format("nodeJson parse error: %s", e.getMessage()), e, annotatedPathTypeElement);
        }

        return nodeParseEntityToPathNodeEntity(annotatedPathTypeElement, nodeParseEntity);
    }

    private NodeParseEntity jsonNodeConvertToNodeParseEntity(JsonNode jsonNode, Map<String, TypeElement> nodeJsonRefTypeMapper) throws AptProcessException {

        NodeParseEntity nodeParseEntity = new NodeParseEntity();

        nodeParseEntity.type = jsonNode.type;

        if (jsonNode.args != null
                && jsonNode.args.length > 0) {

            int nullIndex = Arrays.asList(jsonNode.args).indexOf(null);
            if (nullIndex >= 0) {
                throw new AptProcessException(String.format("args can't contain null value, but found on index '%d'", nullIndex));
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
                    throw new AptProcessException(String.format("found error on convert subNodes[%d]: %s", i, e.getMessage()), e);
                }

                nodeParseEntity.subNodes.add(subNodeParseEntity);
            }
        }

        if (jsonNode.subNodeRef != null) {
            TypeElement typeElement = nodeJsonRefTypeMapper.get(jsonNode.subNodeRef);
            if (typeElement == null) {
                throw new AptProcessException(String.format("can't find nodeJson's subNodeRef's '%s's refClass in nodeJsonRefTypeMapper", jsonNode.subNodeRef));
            }
            nodeParseEntity.subNodeRef = typeElement;
        }

        return nodeParseEntity;
    }

    private PathEntity.Node nodeParseEntityToPathNodeEntity(TypeElement annotatedPathTypeElement, NodeParseEntity nodeParseEntity) throws AptProcessException {
        final PathEntity.Node resultNodeEntity = new PathEntity.Node();

        // type convert
        if (nodeParseEntity.type == null
                || nodeParseEntity.type.isEmpty()) {
            throw new AptProcessException("nodeJson's type can't be null", annotatedPathTypeElement);
        }
        resultNodeEntity.type = nodeParseEntity.type;

        // args convert
        if (nodeParseEntity.args != null) {

            // check null arg
            int nullIndex = nodeParseEntity.args.indexOf(null);
            if (nullIndex >= 0) {
                throw new AptProcessException(
                        String.format("nodeJson's args can't contains null value, but found in index '%d'", nullIndex),
                        annotatedPathTypeElement
                );
            }

            // check repeat
            final Set<String> repeatArgNames = new HashSet<>();
            for (NodeParseEntity.NodeArg arg : nodeParseEntity.args) {

                // arg name
                if (!repeatArgNames.add(arg.name)) {
                    throw new AptProcessException("find repeat arg: " + arg.name, annotatedPathTypeElement);
                }

                // arg valueLimits
                if (arg.valueLimits != null) {

                    int valueLimitNullIndex = arg.valueLimits.indexOf(null);
                    if (valueLimitNullIndex >= 0) {
                        throw new AptProcessException(String.format("find null value in arg '%s'.valueLimits[%s]", arg.name, valueLimitNullIndex), annotatedPathTypeElement);
                    }

                    final Set<String> repeatValueLimits = new HashSet<>();
                    for (String valueLimit : arg.valueLimits) {
                        if (!repeatValueLimits.add(valueLimit)) {
                            throw new AptProcessException(String.format("find repeat arg's valueLimit '%s' in arg '%s'", valueLimit, arg.name), annotatedPathTypeElement);
                        }
                    }
                }
            }

            // convert arg
            resultNodeEntity.args = new ArrayList<>(nodeParseEntity.args.size());
            for (NodeParseEntity.NodeArg sourceArg : nodeParseEntity.args) {
                PathEntity.NodeArg targetArg = new PathEntity.NodeArg();
                targetArg.name = sourceArg.name;
                targetArg.valueLimits = sourceArg.valueLimits;

                resultNodeEntity.args.add(targetArg);
            }
        }

        // check subNode
        boolean existSubNodes = nodeParseEntity.subNodes != null && !nodeParseEntity.subNodes.isEmpty();
        boolean existSubNodeRef = nodeParseEntity.subNodeRef != null && !nodeParseEntity.subNodeRef.getQualifiedName().toString().equals(Void.class.getCanonicalName());
        if (existSubNodes && existSubNodeRef) {
            throw new AptProcessException("nodeJson's subNodes and subNodeRef only can exist one", annotatedPathTypeElement);
        }

        // parse subNodes
        if (existSubNodes) {
            final List<PathEntity.Node> subNodes = new LinkedList<>();
            for (int i = 0; i < nodeParseEntity.subNodes.size(); i++) {
                NodeParseEntity subEntity = nodeParseEntity.subNodes.get(i);
                try {
                    subNodes.add(
                            nodeParseEntityToPathNodeEntity(annotatedPathTypeElement, subEntity)
                    );
                } catch (AptProcessException e) {
                    throw new AptProcessException(
                            String.format("found error in nodeJson's subNodes[%s]: %s", i, e.getMessage()),
                            e
                    );
                }
            }
            resultNodeEntity.sub = subNodes;
        }

        // parse subNodeRef
        if (existSubNodeRef) {
            resultNodeEntity.subRef = parsePathWrapper(nodeParseEntity.subNodeRef);
        }

        return resultNodeEntity;
    }

    /**
     * @see Path#nodesJson()
     */
    private static class JsonNode {
        @SerializedName("type")
        String type;
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
        public String type;
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
    public static List<PathEntity> convertPathTree(List<PathEntity> pathEntities) {
        final List<PathEntity> treePathEntities = new LinkedList<>(pathEntities);

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
    public static List<NodeSchema> generateNodeSchemaTree(List<PathEntity> pathEntities) {

        final List<PathEntity> treePathEntities = convertPathTree(pathEntities);
        final List<NodeSchema> result = new LinkedList<>();

        for (PathEntity pathEntity : treePathEntities) {
            if (pathEntity.nodes == null
                    || pathEntity.nodes.isEmpty()) {
                continue;
            }
            result.addAll(convertNodeEntitiesToNodes(pathEntity.nodes));
        }

        return result;
    }


    private static Set<PathEntity> getTreeNeedDeleteNodeEntity(List<PathEntity> pathEntities) {
        final Set<PathEntity> deleteElementContainer = new HashSet<>();

        for (PathEntity pathEntity : pathEntities) {
            getTreeNeedDeleteNodeEntity(pathEntity, deleteElementContainer);
        }

        return deleteElementContainer;
    }

    private static void getTreeNeedDeleteNodeEntity(PathEntity pathEntity, Set<PathEntity> deleteElementContainer) {
        for (PathEntity.Node node : pathEntity.nodes) {
            if (node.subRef != null) {
                deleteElementContainer.add(node.subRef);
                getTreeNeedDeleteNodeEntity(node.subRef, deleteElementContainer);
            }
            if (node.sub != null) {
                getTreeNeedDeleteNodeEntity(node.sub, deleteElementContainer);
            }
        }
    }

    private static void getTreeNeedDeleteNodeEntity(List<PathEntity.Node> nodeEntities, Set<PathEntity> deleteElementContainer) {
        for (PathEntity.Node nodeEntity : nodeEntities) {
            if (nodeEntity.subRef != null) {
                deleteElementContainer.add(nodeEntity.subRef);
                getTreeNeedDeleteNodeEntity(nodeEntity.subRef, deleteElementContainer);
            }
            if (nodeEntity.sub != null) {
                getTreeNeedDeleteNodeEntity(nodeEntity.sub, deleteElementContainer);
            }
        }
    }

    private static List<NodeSchema> convertNodeEntitiesToNodes(List<PathEntity.Node> nodeEntities) {
        List<NodeSchema> convertedSubNodeEntities = new ArrayList<>(nodeEntities.size());

        for (PathEntity.Node subNodeEntity : nodeEntities) {
            convertedSubNodeEntities.add(
                    convertNodeEntityToNode(subNodeEntity)
            );
        }

        return convertedSubNodeEntities;
    }

    private static NodeSchema convertNodeEntityToNode(PathEntity.Node nodeEntity) {

        NodeSchema nodeSchema = new NodeSchema();

        nodeSchema.type = nodeEntity.type;

        if (nodeEntity.args != null) {
            nodeSchema.args = new ArrayList<>(nodeEntity.args.size());

            for (PathEntity.NodeArg sourceArg : nodeEntity.args) {
                NodeSchema.NodeArg targetArg = new NodeSchema.NodeArg();
                targetArg.name = sourceArg.name;
                targetArg.valueLimits = sourceArg.valueLimits;

                nodeSchema.args.add(targetArg);
            }

        }

        List<PathEntity.Node> subNodeEntities = null;
        if (nodeEntity.sub != null) {
            subNodeEntities = nodeEntity.sub;
        }
        if (nodeEntity.subRef != null) {
            subNodeEntities = nodeEntity.subRef.nodes;
        }
        if (subNodeEntities != null
                && !subNodeEntities.isEmpty()) {
            nodeSchema.sub = convertNodeEntitiesToNodes(subNodeEntities);
        }

        return nodeSchema;
    }
}
