package ms.imf.badge.compiler;

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

import ms.imf.badge.annotation.Arg;
import ms.imf.badge.annotation.NodeContainer;
import ms.imf.badge.annotation.SubNode;
import ms.imf.badge.compiler.plugin.AptProcessException;
import ms.imf.badge.compiler.plugin.NodeContainerEntity;
import ms.imf.badge.entity.NodeTree;

/**
 * 节点容器{@link NodeContainer}解析器
 */
class NodeContainerAnnotationParser {

    private final Gson gson = new Gson();
    private final Elements elementUtil;

    NodeContainerAnnotationParser(Elements elementUtil) {
        if (elementUtil == null) {
            throw new IllegalArgumentException("elementUtil can't be null");
        }
        this.elementUtil = elementUtil;
    }

    /**
     * 解析节点容器注解宿主类型为节点容器实体
     * 如果节点容器之间有链接关系，解析时将根据链接关系在实体中对目标实体持有引用
     *
     * @param nodeContainerHosts 节点容器注解宿主类型
     * @return 转换后的节点容器实体
     * @throws AptProcessException 转换时异常，详细信息将在message中描述
     */
    List<NodeContainerEntity> parseNodeContainerHostsToEntities(Set<TypeElement> nodeContainerHosts) throws AptProcessException {
        return new Parser().nodeContainerHostsToEntities(nodeContainerHosts);
    }

    /**
     * 节点树是以深度优先、递归形式解析的，为了建立节点间链接关系及节点树循环引用问题的排查，需要一些与单次解析行为相同生命周期的的'集合'来存储相关数据
     * 将'集合'作为方法入参层层传递可以达到此目的，但参数的层层传递下去会使代码会很难看，因为大多子解析方法并不实际使用该参数却还要接收，只是为了传递给下一层调用
     * 但如果直接将集合抽取为成员变量的话, 成员变量的生命周期又会大于单次解析行为的的生命周期，会造成同一对象情况下的解析行为线程不安全
     * 虽然可以使用同步阻塞功能规避该问题，但对调用者来说直观看来会更觉得解析行为是线程安全且非阻塞的，代码的效果最好就是开发者想的那样
     * 所以对解析方法进行封装，抽取解析行为到内部类并在其内部创建成员变量，而对调用者提供的解析方法的每次调用都会创建一份该内部类的实例并调用真正的解析方法
     * 这样就保证了每次解析行为都拥有与其相同生命周期的'集合', 最终达到解析行为非阻塞式线程安全的目的
     *
     * @see Parser#nodeContainerEntityPoll
     * @see Parser#parsingNodeContainerHosts
     */
    private class Parser {

        /**
         * use for link sub node and reusing node
         */
        private final Map<TypeElement, NodeContainerEntity> nodeContainerEntityPoll = new HashMap<>();

        /**
         * use for check circular reference
         */
        private final List<TypeElement> parsingNodeContainerHosts = new LinkedList<>();

        List<NodeContainerEntity> nodeContainerHostsToEntities(Set<TypeElement> nodeContainerHosts) throws AptProcessException {
            final List<NodeContainerEntity> results = new LinkedList<>();

            for (TypeElement nodeContainerHost : nodeContainerHosts) {
                try {
                    results.add(
                            nodeContainerHostToEntity(nodeContainerHost)
                    );
                } catch (AptProcessException e) {
                    throw new AptProcessException(
                            String.format(
                                    "%s: %s",
                                    nodeContainerHost.getQualifiedName(), e.getMessage()
                            ),
                            e
                    );
                }
            }

            return results;
        }

        private NodeContainerEntity nodeContainerHostToEntity(TypeElement nodeContainerHost) throws AptProcessException {

            NodeContainer nodeContainer = nodeContainerHost.getAnnotation(NodeContainer.class);
            if (nodeContainer == null) {
                throw new AptProcessException("can't find NodeContainer Annotation", nodeContainerHost);
            }
            AnnotationMirror nodeContainerMirror = null;
            for (AnnotationMirror annotationMirror : nodeContainerHost.getAnnotationMirrors()) {
                if (annotationMirror.getAnnotationType().equals(elementUtil.getTypeElement(NodeContainer.class.getCanonicalName()).asType())) {
                    nodeContainerMirror = annotationMirror;
                }
            }
            if (nodeContainerMirror == null) {
                throw new AptProcessException("can't find NodeContainer Annotation", nodeContainerHost);
            }
            return nodeContainerToEntity(nodeContainerHost, nodeContainer, nodeContainerMirror);
        }

        private NodeContainerEntity nodeContainerToEntity(TypeElement nodeContainerHost, NodeContainer nodeContainer, AnnotationMirror nodeContainerMirror) throws AptProcessException {

            // check circular reference
            if (parsingNodeContainerHosts.contains(nodeContainerHost)) {
                List<TypeElement> circularRefHostList = new LinkedList<>(parsingNodeContainerHosts);
                circularRefHostList.add(nodeContainerHost);

                // A>B>C>B: [A,*B*,C,*B*]
                final StringBuilder stack = new StringBuilder();
                stack.append('[');
                Iterator<TypeElement> iterator = circularRefHostList.iterator();
                while (iterator.hasNext()) {
                    TypeElement stackElement = iterator.next();

                    boolean isCircle = stackElement == nodeContainerHost;
                    if (isCircle) {
                        stack.append('*');
                    }

                    stack.append(stackElement.getQualifiedName());

                    if (isCircle) {
                        stack.append('*');
                    }

                    if (iterator.hasNext()) {
                        stack.append(',');
                    }
                }
                stack.append(']');

                throw new AptProcessException(
                        String.format(
                                "circular reference, this is stack: %s",
                                stack
                        ),
                        nodeContainerHost,
                        nodeContainerMirror
                );
            }

            // reuse parsed nodeContainer entity
            final NodeContainerEntity pollNodeContainerEntity = nodeContainerEntityPoll.get(nodeContainerHost);
            if (pollNodeContainerEntity != null) {
                return pollNodeContainerEntity;
            }

            // use for check circular reference
            parsingNodeContainerHosts.add(nodeContainerHost);
            final NodeContainerEntity resultNodeTreeEntity;
            try {
                // do raw parse
                resultNodeTreeEntity = nodeContainerToEntityRaw(nodeContainerHost, nodeContainer, nodeContainerMirror);
            } finally {
                // use for check circular reference
                parsingNodeContainerHosts.remove(nodeContainerHost);
            }

            // use for reuse parsed nodeContainer entity
            nodeContainerEntityPoll.put(nodeContainerHost, resultNodeTreeEntity);

            return resultNodeTreeEntity;
        }

        private NodeContainerEntity nodeContainerToEntityRaw(TypeElement nodeContainerHost, NodeContainer nodeContainer, AnnotationMirror nodeContainerMirror) throws AptProcessException {

            final boolean annotationMode = nodeContainer.value().length > 0;
            final boolean jsonMode = nodeContainer.nodeJson().length > 0;

            final List<NodeContainerEntity.Node> nodeEntities = new LinkedList<>();

            if (annotationMode) {
                try {
                    nodeEntities.addAll(
                            nodeAnnotationToNodeEntity(nodeContainer, nodeContainerMirror)
                    );
                } catch (AptProcessException e) {
                    throw new AptProcessException(
                            String.format("value(nodes): %s", e.getMessage()),
                            e, nodeContainerHost, nodeContainerMirror
                    );
                }
            }
            if (jsonMode) {
                try {
                    nodeEntities.addAll(
                            nodeJsonToNodeEntity(nodeContainer, nodeContainerMirror)
                    );
                } catch (AptProcessException e) {
                    throw new AptProcessException(
                            String.format("nodeJson: %s", e.getMessage()),
                            e, nodeContainerHost, nodeContainerMirror
                    );
                }
            }

            // check repeat node name
            final Set<String> repeatElements = new HashSet<>();
            for (NodeContainerEntity.Node nodeEntity : nodeEntities) {
                if (!repeatElements.add(nodeEntity.name)) {
                    throw new AptProcessException(
                            String.format("repeat node name in nodes and nodeJson: %s", nodeEntity.name),
                            nodeContainerHost, nodeContainerMirror
                    );
                }
            }

            NodeContainerEntity nodeTreeEntity = new NodeContainerEntity();
            nodeTreeEntity.nodes = nodeEntities;
            nodeTreeEntity.host = nodeContainerHost;
            return nodeTreeEntity;
        }

        private List<NodeContainerEntity.Node> nodeJsonToNodeEntity(NodeContainer nodeContainer, AnnotationMirror nodeContainerMirror) throws AptProcessException {
            final List<NodeContainerEntity.Node> results = new LinkedList<>();

            final Map<String, TypeElement> nodeJsonRefTypeMapper = new HashMap<>();
            List<AnnotationMirror> nodesJsonRefClassMappers = NodeContainerAnnotationParser.getAnnotationMirrorValue(nodeContainerMirror, Constants.NodeContainer_nodeJsonRefContainerMapper);
            if (nodesJsonRefClassMappers == null) {
                nodesJsonRefClassMappers = Collections.emptyList();
            }

            ListIterator<AnnotationMirror> mapperIterator = nodesJsonRefClassMappers.listIterator();
            while (mapperIterator.hasNext()) {
                int index = mapperIterator.nextIndex();
                AnnotationMirror mapperMirror = mapperIterator.next();

                String key = NodeContainerAnnotationParser.getAnnotationMirrorValue(mapperMirror, Constants.Mapper_key);
                TypeElement value = (TypeElement) NodeContainerAnnotationParser.<DeclaredType>getAnnotationMirrorValue(mapperMirror, Constants.Mapper_value).asElement();
                if (nodeJsonRefTypeMapper.put(key, value) != null) {
                    throw new AptProcessException(
                            String.format("subNodeRef[%d]: key(%s): repeat key", index, key),
                            null,
                            nodeContainerMirror
                    );
                }
            }

            for (int i = 0; i < nodeContainer.nodeJson().length; i++) {
                String nodeJson = nodeContainer.nodeJson()[i];
                try {
                    NodeContainerEntity.Node nodeEntity = nodeJsonToNodeEntity(nodeJson, nodeJsonRefTypeMapper);
                    results.add(nodeEntity);
                } catch (AptProcessException e) {
                    throw new AptProcessException(
                            String.format("[%d]: %s", i, e.getMessage()),
                            e
                    );
                }
            }

            return results;
        }

        private List<NodeContainerEntity.Node> nodeAnnotationToNodeEntity(NodeContainer nodeContainer, AnnotationMirror nodeContainerMirror) throws AptProcessException {
            final LinkedList<NodeContainerEntity.Node> results = new LinkedList<>();

            final List<AnnotationMirror> nodeMirrors = NodeContainerAnnotationParser.getAnnotationMirrorValue(nodeContainerMirror, Constants.NodeContainer_value);

            for (int i = 0; i < nodeContainer.value().length; i++) {
                SubNode node = nodeContainer.value()[i];
                AnnotationMirror nodeMirror = nodeMirrors.get(i);

                try {

                    GeneralNode generalNode = nodeContainerNodeToGeneralNode(AnnotationNodeWrapper.instance(node, nodeMirror));
                    NodeContainerEntity.Node nodeEntity = generalNodeToNodeEntity(generalNode);

                    results.add(nodeEntity);

                } catch (AptProcessException e) {
                    throw new AptProcessException(
                            String.format(
                                    "[%d](%s): %s",
                                    i, node.value(), e.getMessage()
                            ),
                            e, null, nodeMirror
                    );
                }
            }

            return results;
        }

        private <Source extends Annotation, Sub extends Annotation> GeneralNode nodeContainerNodeToGeneralNode(AnnotationNodeWrapper<Source, Sub> source) {

            GeneralNode target = new GeneralNode();

            target.name = source.name();

            if (source.args() != null) {
                target.args = new ArrayList<>(source.args().length);

                for (Arg sourceArg : source.args()) {

                    GeneralNode.NodeArg targetArg = new GeneralNode.NodeArg();
                    targetArg.name = sourceArg.value();
                    targetArg.valueLimits = Arrays.asList(sourceArg.valueLimits());

                    target.args.add(targetArg);
                }
            }

            target.subNodeRef = source.subRef();

            Sub[] subNodes = source.subNodes();

            if (subNodes != null) {
                target.subNodes = new ArrayList<>(subNodes.length);
                List<AnnotationMirror> subNodeMirrors = source.subNodeMirrors();
                for (int i = 0; i < subNodes.length; i++) {
                    Sub subNode = subNodes[i];
                    AnnotationMirror subNodeMirror = subNodeMirrors.get(i);

                    target.subNodes.add(
                            nodeContainerNodeToGeneralNode(
                                    source.subNodeWrapper(subNode, subNodeMirror)
                            )
                    );
                }
            }

            return target;
        }

        private NodeContainerEntity.Node nodeJsonToNodeEntity(String nodeJson, Map<String, TypeElement> nodeJsonRefTypeMapper) throws AptProcessException {
            // parse json
            JsonNode jsonNode;
            try {
                jsonNode = gson.fromJson(nodeJson, JsonNode.class);
            } catch (Exception e) {
                throw new AptProcessException(String.format("error on parsing json: %s", e.getMessage()), e);
            }

            // convert to parseEntity
            GeneralNode generalNode;
            try {
                generalNode = jsonNodeToGeneralNode(jsonNode, nodeJsonRefTypeMapper);
            } catch (AptProcessException e) {
                throw new AptProcessException(e.getMessage(), e);
            }

            return generalNodeToNodeEntity(generalNode);
        }

        private GeneralNode jsonNodeToGeneralNode(JsonNode jsonNode, Map<String, TypeElement> nodeJsonRefTypeMapper) throws AptProcessException {

            GeneralNode generalNode = new GeneralNode();

            generalNode.name = jsonNode.name;

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

                generalNode.args = new ArrayList<>(jsonNode.args.length);

                for (JsonNode.Arg sourceArg : jsonNode.args) {
                    GeneralNode.NodeArg targetArg = new GeneralNode.NodeArg();
                    targetArg.name = sourceArg.name;
                    if (sourceArg.limits != null) {
                        targetArg.valueLimits = Arrays.asList(sourceArg.limits);
                    }

                    generalNode.args.add(targetArg);
                }
            }

            if (jsonNode.subNodes != null
                    && !jsonNode.subNodes.isEmpty()) {
                generalNode.subNodes = new LinkedList<>();
                for (int i = 0; i < jsonNode.subNodes.size(); i++) {
                    JsonNode subJsonNode = jsonNode.subNodes.get(i);

                    GeneralNode subGeneralNode;
                    try {
                        subGeneralNode = jsonNodeToGeneralNode(subJsonNode, nodeJsonRefTypeMapper);
                    } catch (AptProcessException e) {
                        throw new AptProcessException(
                                String.format(
                                        "subNodes[%d](%s): %s",
                                        i, subJsonNode.name, e.getMessage()
                                ),
                                e
                        );
                    }

                    generalNode.subNodes.add(subGeneralNode);
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
                generalNode.subNodeRef = typeElement;
            }

            return generalNode;
        }

        private NodeContainerEntity.Node generalNodeToNodeEntity(GeneralNode source) throws AptProcessException {
            final NodeContainerEntity.Node target = new NodeContainerEntity.Node();

            // convert name
            if (source.name == null
                    || source.name.isEmpty()) {
                throw new AptProcessException("name can't be null");
            }
            target.name = source.name;

            // convert args
            if (source.args != null) {

                // check null arg
                int nullIndex = source.args.indexOf(null);
                if (nullIndex >= 0) {
                    throw new AptProcessException(
                            String.format("args[%d]: null value", nullIndex)
                    );
                }

                // check repeat
                final Set<String> repeatArgNames = new HashSet<>();
                ListIterator<GeneralNode.NodeArg> argIterator = source.args.listIterator();
                while (argIterator.hasNext()) {
                    int index = argIterator.nextIndex();
                    GeneralNode.NodeArg arg = argIterator.next();

                    // check arg name repeat
                    if (!repeatArgNames.add(arg.name)) {
                        throw new AptProcessException(String.format("args[%d](%s): repeat arg", index, arg.name));
                    }

                    // arg valueLimits
                    if (arg.valueLimits != null) {

                        int valueLimitNullIndex = arg.valueLimits.indexOf(null);
                        if (valueLimitNullIndex >= 0) {
                            throw new AptProcessException(
                                    String.format(
                                            "args[%d](%s): valueLimits[%d]: null value",
                                            index, arg.name, valueLimitNullIndex
                                    )
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
                                        )
                                );
                            }

                        }
                    }
                }

                // convert arg
                target.args = new ArrayList<>(source.args.size());
                for (GeneralNode.NodeArg sourceArg : source.args) {
                    NodeContainerEntity.Node.Arg targetArg = new NodeContainerEntity.Node.Arg();
                    targetArg.name = sourceArg.name;
                    targetArg.valueLimits = sourceArg.valueLimits;

                    target.args.add(targetArg);
                }
            }

            // check subNode
            boolean existSubNodes = source.subNodes != null && !source.subNodes.isEmpty();
            boolean existSubNodeRef = source.subNodeRef != null && !source.subNodeRef.getQualifiedName().toString().equals(Void.class.getCanonicalName());
            if (existSubNodes && existSubNodeRef) {
                throw new AptProcessException("subNodes and subNodeContainerRef only can exist one");
            }

            // parse subNodes
            if (existSubNodes) {
                final List<NodeContainerEntity.Node> subNodes = new LinkedList<>();
                for (int i = 0; i < source.subNodes.size(); i++) {
                    GeneralNode subEntity = source.subNodes.get(i);
                    try {
                        subNodes.add(
                                generalNodeToNodeEntity(subEntity)
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
                target.sub = subNodes;
            }

            // parse subNodeRef
            if (existSubNodeRef) {
                try {
                    target.subRef = nodeContainerHostToEntity(source.subNodeRef);
                } catch (AptProcessException e) {
                    throw new AptProcessException(
                            String.format(
                                    "subNodeContainerRef(%s): %s",
                                    source.subNodeRef.getQualifiedName(), e.getMessage()
                            ),
                            e,
                            source.subNodeRef
                    );
                }
            }

            return target;
        }

    }

    static <T> T getAnnotationMirrorValue(AnnotationMirror annotationMirror, String key) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationMirror.getElementValues().entrySet()) {
            if (entry.getKey().getSimpleName().toString().equals(key)) {
                return (T) entry.getValue().getValue();
            }
        }
        return null;
    }

    /**
     * 转换扁平型节点容器为树型，只保留根类型节点容器
     *
     * @param entities source
     * @return target
     */
    static List<NodeContainerEntity> convertNodeContainersToTree(List<NodeContainerEntity> entities) {
        final List<NodeContainerEntity> result = new LinkedList<>(entities);

        result.removeAll(
                getSubNodeTypeContainers(entities)
        );

        return result;
    }

    /**
     * 转换节点容器到节点树
     *
     * @param entities source
     * @return target
     */
    static List<NodeTree> convertNodeContainersToNodeTree(List<NodeContainerEntity> entities) {

        final List<NodeContainerEntity> treeNodeContainers = convertNodeContainersToTree(entities);
        final List<NodeTree> result = new LinkedList<>();

        for (NodeContainerEntity nodeTreeEntity : treeNodeContainers) {
            if (nodeTreeEntity.nodes == null
                    || nodeTreeEntity.nodes.isEmpty()) {
                continue;
            }
            result.addAll(nodeEntitiesToNodeTree(nodeTreeEntity.nodes));
        }

        return result;
    }


    private static Set<NodeContainerEntity> getSubNodeTypeContainers(List<NodeContainerEntity> nodeContainerEntities) {
        final Set<NodeContainerEntity> results = new HashSet<>();

        for (NodeContainerEntity entity : nodeContainerEntities) {
            getSubNodeTypeContainers(entity.nodes, results);
        }

        return results;
    }

    private static void getSubNodeTypeContainers(List<NodeContainerEntity.Node> entities, Set<NodeContainerEntity> resultsContainer) {
        if (entities == null
                || entities.isEmpty()) {
            return;
        }
        for (NodeContainerEntity.Node node : entities) {
            if (node.subRef != null) {
                resultsContainer.add(node.subRef);
                getSubNodeTypeContainers(node.subRef.nodes, resultsContainer);
            }
            if (node.sub != null) {
                getSubNodeTypeContainers(node.sub, resultsContainer);
            }
        }
    }

    private static List<NodeTree> nodeEntitiesToNodeTree(List<NodeContainerEntity.Node> nodeEntities) {
        List<NodeTree> convertedSubNodeEntities = new ArrayList<>(nodeEntities.size());

        for (NodeContainerEntity.Node entity : nodeEntities) {
            convertedSubNodeEntities.add(
                    nodeEntityToNodeTree(entity)
            );
        }

        return convertedSubNodeEntities;
    }

    private static NodeTree nodeEntityToNodeTree(NodeContainerEntity.Node nodeEntity) {

        NodeTree nodeTree = new NodeTree();

        nodeTree.name = nodeEntity.name;

        if (nodeEntity.args != null) {
            nodeTree.args = new ArrayList<>(nodeEntity.args.size());

            for (NodeContainerEntity.Node.Arg sourceArg : nodeEntity.args) {
                NodeTree.Arg targetArg = new NodeTree.Arg();
                targetArg.name = sourceArg.name;
                targetArg.valueLimits = sourceArg.valueLimits;

                nodeTree.args.add(targetArg);
            }

        }

        List<NodeContainerEntity.Node> subNodeEntities = null;
        if (nodeEntity.sub != null) {
            subNodeEntities = nodeEntity.sub;
        }
        if (nodeEntity.subRef != null) {
            subNodeEntities = nodeEntity.subRef.nodes;
        }
        if (subNodeEntities != null
                && !subNodeEntities.isEmpty()) {
            nodeTree.sub = nodeEntitiesToNodeTree(subNodeEntities);
        }

        return nodeTree;
    }


    /**
     * Node包装类型
     * 由于Node有不同的声明模式：{@link NodeContainer#value()}、{@link NodeContainer#nodeJson()}
     * 为对解析过程进行重用，故抽取出新包装类型出来，解析时先将不同模式的的Node解析为本包装类型，而后进行真正的解析
     */
    private static class GeneralNode {
        String name;
        List<NodeArg> args;
        List<GeneralNode> subNodes;
        TypeElement subNodeRef;

        static class NodeArg {
            String name;
            List<String> valueLimits;
        }
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
}
