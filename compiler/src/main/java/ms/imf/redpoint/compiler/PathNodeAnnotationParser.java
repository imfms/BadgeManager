package ms.imf.redpoint.compiler;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

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

import ms.imf.redpoint.annotation.Node;
import ms.imf.redpoint.annotation.Path;
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

    synchronized List<PathEntity> parsePaths(Set<TypeElement> annotatedPathTypeElements) throws CompilerException {

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

    private PathEntity parsePathWrapper(TypeElement annotatedPathTypeElement) throws CompilerException {
        try {
            return parsePath(annotatedPathTypeElement);
        } catch (CompilerException e) {
            throw new CompilerException(
                    String.format("found error on parse type %s's PathAnnotation: %s", annotatedPathTypeElement.getQualifiedName(), e.getMessage()),
                    e
            );
        }
    }

    private PathEntity parsePath(TypeElement annotatedPathTypeElement) throws CompilerException {

        Path path = annotatedPathTypeElement.getAnnotation(Path.class);
        if (path == null) {
            throw new CompilerException("can't find Path Annotation", annotatedPathTypeElement);
        }
        AnnotationMirror pathMirror = null;
        for (AnnotationMirror annotationMirror : annotatedPathTypeElement.getAnnotationMirrors()) {
            if (annotationMirror.getAnnotationType().equals(elementUtil.getTypeElement(Path.class.getCanonicalName()).asType())) {
                pathMirror = annotationMirror;
            }
        }
        if (pathMirror == null) {
            throw new CompilerException("can't find Path Annotation", annotatedPathTypeElement);
        }
        return parsePath(annotatedPathTypeElement, path, pathMirror);
    }

    private synchronized PathEntity parsePath(TypeElement annotatedPathTypeElement, Path path, AnnotationMirror pathMirror) throws CompilerException {

        // check circular reference
        if (parsingTypeElements.contains(annotatedPathTypeElement)) {
            final StringBuilder stack = new StringBuilder();
            List<TypeElement> copyParsingTypeElements = new LinkedList<>(parsingTypeElements);
            copyParsingTypeElements.add(annotatedPathTypeElement);
            for (TypeElement parsingTypeElement : copyParsingTypeElements) {
                stack.append('\n')
                        .append(parsingTypeElement.getQualifiedName());
            }
            throw new CompilerException(
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

    private PathEntity parsePathRaw(TypeElement annotatedPathTypeElement, Path path, AnnotationMirror pathMirror) throws CompilerException {

        final boolean nodeMode = path.value().length > 0;
        final boolean jsonMode = path.nodesJson().length > 0;

        final List<PathEntity.Node> nodeEntities = new LinkedList<>();

        if (nodeMode) {
            try {
                nodeEntities.addAll(pathNodeToNodeEntity(annotatedPathTypeElement, path, pathMirror));
            } catch (CompilerException e) {
                throw new CompilerException(String.format("found error on parse nodes: %s", e.getMessage()), e);
            }
        }
        if (jsonMode) {
            try {
                nodeEntities.addAll(pathNodeJsonToNodeEntity(annotatedPathTypeElement, path, pathMirror));
            } catch (CompilerException e) {
                throw new CompilerException(String.format("found error on parse nodesJson: %s", e.getMessage()), e);
            }
        }

        // check repeat type
        final Set<String> repeatElements = new HashSet<>();
        for (PathEntity.Node nodeEntity : nodeEntities) {
            if (!repeatElements.add(nodeEntity.type)) {
                throw new CompilerException("find repeat type in nodes and nodesJson: " + nodeEntity.type, annotatedPathTypeElement, pathMirror);
            }
        }

        PathEntity pathEntity = new PathEntity();
        pathEntity.nodes = nodeEntities;
        pathEntity.host = annotatedPathTypeElement;
        return pathEntity;
    }

    private List<PathEntity.Node> pathNodeJsonToNodeEntity(TypeElement annotatedPathTypeElement, Path path, AnnotationMirror pathMirror) throws CompilerException {
        final List<PathEntity.Node> results = new LinkedList<>();

        final Map<String, TypeElement> nodeJsonRefTypeMapper = new HashMap<>();
        for (AnnotationMirror mapperMirror : PathNodeAnnotationParser.<List<AnnotationMirror>>getAnnotionMirrorValue(pathMirror, "nodesJsonRefClassMapper")) {
            String key = PathNodeAnnotationParser.getAnnotionMirrorValue(mapperMirror, "key");
            TypeElement value = (TypeElement) PathNodeAnnotationParser.<DeclaredType>getAnnotionMirrorValue(mapperMirror, "value").asElement();
            if (nodeJsonRefTypeMapper.put(key, value) != null) {
                throw new CompilerException(
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
            } catch (CompilerException e) {
                throw new CompilerException(
                        String.format("found error in parse nodeJson[%s]: %s", i, e.getMessage()),
                        e
                );
            }
        }

        return results;
    }

    private List<PathEntity.Node> pathNodeToNodeEntity(TypeElement annotatedPathTypeElement, Path path, AnnotationMirror pathMirror) throws CompilerException {
        final LinkedList<PathEntity.Node> results = new LinkedList<>();

        final List<AnnotationMirror> nodeMirrors = PathNodeAnnotationParser.getAnnotionMirrorValue(pathMirror, "value");

        for (int i = 0; i < path.value().length; i++) {
            Node node = path.value()[i];
            AnnotationMirror nodeMirror = nodeMirrors.get(i);
            try {
                NodeParseEntity nodeParseEntity = new NodeParseEntity();

                nodeParseEntity.type = node.type();
                nodeParseEntity.args = node.args();
                nodeParseEntity.subNodeRef = (TypeElement) PathNodeAnnotationParser.<DeclaredType>getAnnotionMirrorValue(nodeMirror, "subRef").asElement();

                results.add(nodeParseEntityToPathNodeEntity(annotatedPathTypeElement, nodeParseEntity));
            } catch (CompilerException e) {
                throw new CompilerException(
                        String.format("found error in parse value[%s]: %s", i, e.getMessage()),
                        e, annotatedPathTypeElement, nodeMirror
                );
            }
        }

        return results;
    }


    private PathEntity.Node nodeJsonToNodeEntity(TypeElement annotatedPathTypeElement, String nodeJson, Map<String, TypeElement> nodeJsonRefTypeMapper) throws CompilerException {
        // parse json
        JsonNode jsonNodeObj;
        try {
            jsonNodeObj = gson.fromJson(nodeJson, JsonNode.class);
        } catch (Exception e) {
            throw new CompilerException(String.format("nodeJson parse error: %s", e.getMessage()), e, annotatedPathTypeElement);
        }

        // convert to parseEntity
        NodeParseEntity nodeParseEntity;
        try {
            nodeParseEntity = jsonNodeConvertToNodeParseEntity(jsonNodeObj, nodeJsonRefTypeMapper);
        } catch (CompilerException e) {
            throw new CompilerException(String.format("nodeJson parse error: %s", e.getMessage()), e, annotatedPathTypeElement);
        }

        return nodeParseEntityToPathNodeEntity(annotatedPathTypeElement, nodeParseEntity);
    }

    private NodeParseEntity jsonNodeConvertToNodeParseEntity(JsonNode jsonNode, Map<String, TypeElement> nodeJsonRefTypeMapper) throws CompilerException {

        NodeParseEntity nodeParseEntity = new NodeParseEntity();

        nodeParseEntity.type = jsonNode.type;

        nodeParseEntity.args = jsonNode.args;

        if (jsonNode.subNodes != null
                && !jsonNode.subNodes.isEmpty()) {
            nodeParseEntity.subNodes = new LinkedList<>();
            for (int i = 0; i < jsonNode.subNodes.size(); i++) {
                JsonNode subJsonNode = jsonNode.subNodes.get(i);

                NodeParseEntity subNodeParseEntity;
                try {
                    subNodeParseEntity = jsonNodeConvertToNodeParseEntity(subJsonNode, nodeJsonRefTypeMapper);
                } catch (CompilerException e) {
                    throw new CompilerException(String.format("found error on convert subNodes[%d]: %s", i, e.getMessage()), e);
                }

                nodeParseEntity.subNodes.add(subNodeParseEntity);
            }
        }

        if (jsonNode.subNodeRef != null) {
            TypeElement typeElement = nodeJsonRefTypeMapper.get(jsonNode.subNodeRef);
            if (typeElement == null) {
                throw new CompilerException(String.format("can't find nodeJson's subNodeRef's '%s's refClass in nodeJsonRefTypeMapper", jsonNode.subNodeRef));
            }
            nodeParseEntity.subNodeRef = typeElement;
        }

        return nodeParseEntity;
    }

    private PathEntity.Node nodeParseEntityToPathNodeEntity(TypeElement annotatedPathTypeElement, NodeParseEntity nodeParseEntity) throws CompilerException {
        final PathEntity.Node resultNodeEntity = new PathEntity.Node();

        // parse type
        if (nodeParseEntity.type == null
                || nodeParseEntity.type.isEmpty()) {
            throw new CompilerException("nodeJson's type can't be null", annotatedPathTypeElement);
        }
        resultNodeEntity.type = nodeParseEntity.type;

        // parse args
        if (nodeParseEntity.args == null) {
            resultNodeEntity.args = Collections.emptyList();
        } else {

            // check empty arg
            for (int nullIndex : new int[]{
                    Arrays.asList(nodeParseEntity.args).indexOf(null),
                    Arrays.asList(nodeParseEntity.args).indexOf("")}) {
                if (nullIndex >= 0) {
                    throw new CompilerException(
                            String.format("nodeJson's args can't contains null or empty value, but found in index '%d'", nullIndex),
                            annotatedPathTypeElement
                    );
                }
            }

            // check repeat arg
            final Set<String> repeatElements = new HashSet<>();
            for (String arg : nodeParseEntity.args) {
                if (!repeatElements.add(arg)) {
                    throw new CompilerException("find repeat arg: " + arg, annotatedPathTypeElement);
                }
            }

            resultNodeEntity.args = Arrays.asList(nodeParseEntity.args);
        }

        // check subNode
        boolean existSubNodes = nodeParseEntity.subNodes != null && !nodeParseEntity.subNodes.isEmpty();
        boolean existSubNodeRef = nodeParseEntity.subNodeRef != null;
        if (existSubNodes && existSubNodeRef) {
            throw new CompilerException("nodeJson's subNodes and subNodeRef only can exist one", annotatedPathTypeElement);
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
                } catch (CompilerException e) {
                    throw new CompilerException(
                            String.format("found error in nodeJson's subNodes[%s]: %s", i, e.getMessage()),
                            e
                    );
                }
            }
            resultNodeEntity.sub = subNodes;
        }

        // parse subNodeRef
        if (existSubNodeRef) {
            boolean isExistSubRef = !nodeParseEntity.subNodeRef.getQualifiedName().toString().equals(Void.class.getCanonicalName());
            if (isExistSubRef) {
                resultNodeEntity.subRef = parsePathWrapper(nodeParseEntity.subNodeRef);
            }
        }

        return resultNodeEntity;
    }

    private static class JsonNode {
        @SerializedName("type")
        public String type;
        @SerializedName("args")
        public String[] args;
        @SerializedName("subNodes")
        public List<JsonNode> subNodes;
        @SerializedName("subNodeRef")
        public String subNodeRef;
    }

    private static class NodeParseEntity {
        public String type;
        public String[] args;
        public List<NodeParseEntity> subNodes;
        public TypeElement subNodeRef;
    }

    private static <T> T getAnnotionMirrorValue(AnnotationMirror annotationMirror, String key) {
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
        nodeSchema.args = nodeEntity.args;

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
