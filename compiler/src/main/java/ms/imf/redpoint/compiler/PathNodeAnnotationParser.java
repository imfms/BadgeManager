package ms.imf.redpoint.compiler;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

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


/**
 * todo node 和 jsonNode 的解析过程可以重用
 * todo subNodeRefMapppers 添加key重复校验
 */
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

        final List<NodeEntity> nodeEntities = new LinkedList<>();

        if (nodeMode) {
            nodeEntities.addAll(pathNodeToNodeEntity(annotatedPathTypeElement, path, pathMirror));
        }
        if (jsonMode) {
            nodeEntities.addAll(pathNodeJsonToNodeEntity(annotatedPathTypeElement, path, pathMirror));
        }

        // check repeat type
        final Set<String> repeatElements = new HashSet<>();
        for (NodeEntity nodeEntity : nodeEntities) {
            if (!repeatElements.add(nodeEntity.type)) {
                throw new CompilerException("find repeat type: " + nodeEntity.type, annotatedPathTypeElement, pathMirror);
            }
        }

        PathEntity pathEntity = new PathEntity();
        pathEntity.nodes = nodeEntities;
        pathEntity.host = annotatedPathTypeElement;
        return pathEntity;
    }

    private List<NodeEntity> pathNodeJsonToNodeEntity(TypeElement annotatedPathTypeElement, Path path, AnnotationMirror pathMirror) throws CompilerException {
        final List<NodeEntity> results = new LinkedList<>();

        final Map<String, TypeElement> nodeJsonRefTypeMapper = new HashMap<>();
        for (AnnotationMirror mapperMirror : PathNodeAnnotationParser.<List<AnnotationMirror>>getAnnotionMirrorValue(pathMirror, "nodesJsonRefClassMapper")) {
            String key = PathNodeAnnotationParser.getAnnotionMirrorValue(mapperMirror, "key");
            TypeElement value = (TypeElement) PathNodeAnnotationParser.<DeclaredType>getAnnotionMirrorValue(mapperMirror, "value").asElement();
            nodeJsonRefTypeMapper.put(key, value);
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

    private List<NodeEntity> pathNodeToNodeEntity(TypeElement annotatedPathTypeElement, Path path, AnnotationMirror pathMirror) throws CompilerException {
        final LinkedList<NodeEntity> results = new LinkedList<>();

        final List<AnnotationMirror> nodeMirrors = PathNodeAnnotationParser.getAnnotionMirrorValue(pathMirror, "value");

        for (int i = 0; i < path.value().length; i++) {
            Node node = path.value()[i];
            AnnotationMirror nodeMirror = nodeMirrors.get(i);
            try {
                results.add(nodeAnnotationToNodeEntity(annotatedPathTypeElement, node, nodeMirror));
            } catch (CompilerException e) {
                throw new CompilerException(
                        String.format("found error in parse value[%s]: %s", i, e.getMessage()),
                        e
                );
            }
        }

        return results;
    }

    private NodeEntity nodeAnnotationToNodeEntity(TypeElement annotatedPathTypeElement, Node nodeAnnotation, AnnotationMirror nodeMirror) throws CompilerException {
        final NodeEntity nodeEntity = new NodeEntity();

        // nodeAnnotation.type
        if (nodeAnnotation.type().isEmpty()) {
            throw new CompilerException("type can't be empty", annotatedPathTypeElement, nodeMirror);
        }
        nodeEntity.type = nodeAnnotation.type();

        // nodeAnnotation.args
        // check empty arg
        List<String> argList = Arrays.asList(nodeAnnotation.args());
        int argNullIndex = argList.indexOf("");
        if (argNullIndex >= 0) {
            throw new CompilerException(String.format("arg can't contains empty value but found in index [%s]", argNullIndex), annotatedPathTypeElement, nodeMirror);
        }
        // check  repeat arg
        final Set<String> repeatElements = new HashSet<>();
        for (String arg : argList) {
            if (!repeatElements.add(arg)) {
                throw new CompilerException("find repeat arg: " + arg, annotatedPathTypeElement, nodeMirror);
            }
        }

        nodeEntity.args = argList;

        // nodeAnnotation.subRef
        TypeElement subRefTypeElement = (TypeElement) PathNodeAnnotationParser.<DeclaredType>getAnnotionMirrorValue(nodeMirror, "subRef").asElement();
        boolean isExistSubRef = !subRefTypeElement.getQualifiedName().toString().equals(Void.class.getCanonicalName());
        if (isExistSubRef) {
            nodeEntity.subRef = parsePathWrapper(subRefTypeElement);
        }

        return nodeEntity;
    }

    private NodeEntity nodeJsonToNodeEntity(TypeElement annotatedPathTypeElement, String nodeJson, Map<String, TypeElement> nodeJsonRefTypeMapper) throws CompilerException {
        // parse json
        JsonNode jsonNodeObj;
        try {
            jsonNodeObj = gson.fromJson(nodeJson, JsonNode.class);
        } catch (Exception e) {
            throw new CompilerException("nodeJson parse error: " + e.getMessage(), e, annotatedPathTypeElement);
        }

        return nodeJsonObjToNodeEntity(annotatedPathTypeElement, jsonNodeObj, nodeJsonRefTypeMapper);
    }

    private NodeEntity nodeJsonObjToNodeEntity(TypeElement annotatedPathTypeElement, JsonNode nodeJsonObj, Map<String, TypeElement> nodeJsonRefTypeMapper) throws CompilerException {
        final NodeEntity resultNodeEntity = new NodeEntity();

        // parse type
        if (nodeJsonObj.type == null
                || nodeJsonObj.type.isEmpty()) {
            throw new CompilerException("nodeJson's type can't be null", annotatedPathTypeElement);
        }
        resultNodeEntity.type = nodeJsonObj.type;

        // parse args
        if (nodeJsonObj.args == null) {
            resultNodeEntity.args = Collections.emptyList();
        } else {

            // check empty arg
            for (int nullIndex : new int[]{
                    Arrays.asList(nodeJsonObj.args).indexOf(null),
                    Arrays.asList(nodeJsonObj.args).indexOf("")}) {
                if (nullIndex >= 0) {
                    throw new CompilerException(
                            String.format("nodeJson's args can't contains null or empty value, but found in index '%d'", nullIndex),
                            annotatedPathTypeElement
                    );
                }
            }

            // check repeat arg
            final Set<String> repeatElements = new HashSet<>();
            for (String arg : nodeJsonObj.args) {
                if (!repeatElements.add(arg)) {
                    throw new CompilerException("find repeat arg: " + arg, annotatedPathTypeElement);
                }
            }

            resultNodeEntity.args = Arrays.asList(nodeJsonObj.args);
        }

        // check subNode
        boolean existSubNodes = nodeJsonObj.subNodes != null && !nodeJsonObj.subNodes.isEmpty();
        boolean existSubNodeRef = nodeJsonObj.subNodeRef != null;
        if (existSubNodes && existSubNodeRef) {
            throw new CompilerException("nodeJson's subNodes and subNodeRef only can exist one", annotatedPathTypeElement);
        }

        // parse subNodes
        if (existSubNodes) {
            final List<NodeEntity> subNodes = new LinkedList<>();
            for (int i = 0; i < nodeJsonObj.subNodes.size(); i++) {
                JsonNode subJsonNodeObj = nodeJsonObj.subNodes.get(i);
                try {
                    subNodes.add(
                            nodeJsonObjToNodeEntity(annotatedPathTypeElement, subJsonNodeObj, nodeJsonRefTypeMapper)
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

            // get subNodeRefType
            TypeElement subNodeRefType = nodeJsonRefTypeMapper.get(nodeJsonObj.subNodeRef);
            if (subNodeRefType == null) {
                throw new CompilerException(
                        String.format("can't find nodeJson's subNodeRef's '%s's refClass in nodeJsonRefTypeMapper", nodeJsonObj.subNodeRef),
                        annotatedPathTypeElement
                );
            }
            boolean isExistSubRef = !subNodeRefType.getQualifiedName().toString().equals(Void.class.getCanonicalName());
            if (isExistSubRef) {
                resultNodeEntity.subRef = parsePathWrapper(subNodeRefType);
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

    private static <T> T getAnnotionMirrorValue(AnnotationMirror annotationMirror, String key) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationMirror.getElementValues().entrySet()) {
            if (entry.getKey().getSimpleName().toString().equals(key)) {
                return (T) entry.getValue().getValue();
            }
        }
        return null;
    }
}
