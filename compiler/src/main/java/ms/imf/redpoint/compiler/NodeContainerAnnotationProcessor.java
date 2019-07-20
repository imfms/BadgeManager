package ms.imf.redpoint.compiler;

import com.google.auto.service.AutoService;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

import ms.imf.redpoint.annotation.NodeContainer;
import ms.imf.redpoint.annotation.NodeParserGlobalConfig;
import ms.imf.redpoint.annotation.Plugin;
import ms.imf.redpoint.compiler.plugin.AptProcessException;
import ms.imf.redpoint.compiler.plugin.NodeTreeHandlePlugin;
import ms.imf.redpoint.compiler.plugin.NodeContainerEntity;
import ms.imf.redpoint.entity.NodeTree;

/**
 * 节点容器{@link NodeContainer}注解处理器，根据不同类标注的节点容器注解解析出一张完整的节点树，并对外提供插件机制用于定制编译期节点树处理行为
 *
 * @see NodeContainer
 * @see NodeParserGlobalConfig
 */
@AutoService(Processor.class)
public class NodeContainerAnnotationProcessor extends AbstractProcessor {

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        final LinkedHashSet<String> supportedTypes = new LinkedHashSet<>();

        for (Class annotationClass : new Class[]{NodeContainer.class, NodeParserGlobalConfig.class}) {
            supportedTypes.add(annotationClass.getCanonicalName());
        }

        return supportedTypes;
    }

    private final List<NodeContainerEntity> allNodeContainerEntities = new LinkedList<>();

    private TypeElement nodeParserGlobalConfigHost;
    private NodeParserGlobalConfig nodeParserGlobalConfig;
    private AnnotationMirror nodeParserGlobalConfigMirror;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            return processRaw(roundEnv);
        } catch (Exception e) {
            showErrorTip(new AptProcessException(String.format("unexpected exception on processor: %s", e.getMessage()), e));
            return false;
        }
    }

    private boolean processRaw(RoundEnvironment roundEnv) {
        // check config
        try {
            checkNodeParserGlobalConfig(roundEnv);
        } catch (AptProcessException e) {
            showErrorTip(e);
            return false;
        }

        final Set<TypeElement> nodeContainerHosts = ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(NodeContainer.class));

        // parse node containers
        final List<NodeContainerEntity> nodeContainerEntities;
        final NodeContainerAnnotationParser nodeContainerParser = new NodeContainerAnnotationParser(processingEnv.getElementUtils());
        try {
            nodeContainerEntities = nodeContainerParser.parseNodeContainerHostsToEntities(nodeContainerHosts);
        } catch (AptProcessException e) {
            showErrorTip(new AptProcessException(String.format("found error on parsing @NodeContainer: %s", e.getMessage()), e));
            return false;
        }

        if (nodeParserGlobalConfig != null) {
            // process each apt round plugin
            try {
                processPlugins(
                        "each apt round plugin",
                        NodeContainerAnnotationParser.<List<AnnotationValue>>getAnnotationMirrorValue(nodeParserGlobalConfigMirror, "eachAptRoundNodeTreeParsedPlugins" /* todo runtime check */),
                        nodeParserGlobalConfig.eachAptRoundNodeTreeParsedPlugins(),
                        nodeContainerEntities
                );
            } catch (AptProcessException e) {
                showErrorTip(e);
                return false;
            }
        }

        // compose each round data
        allNodeContainerEntities.addAll(nodeContainerEntities);

        if (!roundEnv.processingOver()) {
            return true;
        }
        // process finish task

        final List<NodeContainerEntity> treeNodeContainerEntities = NodeContainerAnnotationParser.convertNodeContainersToTree(allNodeContainerEntities);

        // check root node repeat
        try {
            nodeContainerNodeTypeRepeatCheck(treeNodeContainerEntities);
        } catch (AptProcessException e) {
            showErrorTip(e);
            return false;
        }

        // check node container's node type
        try {
            checkNodeContainerNodeType(allNodeContainerEntities, treeNodeContainerEntities);
        } catch (AptProcessException e) {
            showErrorTip(e);
            return false;
        }

        if (nodeParserGlobalConfig != null) {
            // process last apt round plugin
            try {
                processPlugins(
                        "last apt round plugin",
                        NodeContainerAnnotationParser.<List<AnnotationValue>>getAnnotationMirrorValue(nodeParserGlobalConfigMirror, "lastAptRoundNodeTreeParsedPlugins" /* todo runtime check */),
                        nodeParserGlobalConfig.lastAptRoundNodeTreeParsedPlugins(),
                        allNodeContainerEntities
                );
            } catch (AptProcessException e) {
                showErrorTip(e);
                return false;
            }
        }

        return true;
    }

    private void checkNodeContainerNodeType(List<NodeContainerEntity> allNodeContainerEntities, List<NodeContainerEntity> treeNodeContainerEntities) throws AptProcessException {
        for (NodeContainerEntity entity : allNodeContainerEntities) {
            boolean isRootNode = treeNodeContainerEntities.contains(entity);
            switch (entity.host.getAnnotation(NodeContainer.class).type()) {
                case ROOT_NODE:
                    if (!isRootNode) {
                        NodeContainerEntity parentEntiity = findParentNodeContainerEntity(entity, treeNodeContainerEntities);
                        throw new AptProcessException(
                                String.format(
                                        "'%s's @NodeContainer should be a root node, but it's a sub node, it has a parent '%s', please check nodeContainer's link relations",
                                        entity.host.getQualifiedName(),
                                        parentEntiity.host.getQualifiedName()
                                ),
                                entity.host
                        );
                    }
                    break;
                case SUB_NODE:
                    if (isRootNode) {
                        throw new AptProcessException(
                                String.format(
                                        "'%s's @NodeContainer should be a sub node, but it's a root node, please check nodeContainer's link relations",
                                        entity.host.getQualifiedName()
                                ),
                                entity.host
                        );
                    }
                    break;
                case UNLIMIT:
                default:
                    break;
            }
        }
    }

    private NodeContainerEntity findParentNodeContainerEntity(NodeContainerEntity searchEntity, List<NodeContainerEntity> treeEntities) {
        for (NodeContainerEntity entity : treeEntities) {
            NodeContainerEntity result = findParentNodeContainerEntity(searchEntity, entity);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private NodeContainerEntity findParentNodeContainerEntity(NodeContainerEntity sourceNodeTreeEntity, NodeContainerEntity nodeTreeEntity) {
        for (NodeContainerEntity.Node node : nodeTreeEntity.nodes) {
            if (node.subRef == sourceNodeTreeEntity) {
                return nodeTreeEntity;
            } else {
                return findParentNodeContainerEntity(sourceNodeTreeEntity, node.subRef);
            }
        }
        return null;
    }

    private void processPlugins(String pluginProcessDesc, List<AnnotationValue> pluginAnnotationValues, Plugin[] plugins, final List<NodeContainerEntity> nodeContainerEntities) throws AptProcessException {

        if (pluginAnnotationValues == null
                || pluginAnnotationValues.isEmpty()
                || plugins == null
                || plugins.length <= 0) {
            return;
        }

        for (int i = 0; i < plugins.length; i++) {
            Plugin pluginAnnotation = plugins[i];
            AnnotationValue pluginAnnotationValue = pluginAnnotationValues.get(i);

            final String pluginClassName = ((TypeElement) ((DeclaredType) NodeContainerAnnotationParser.getAnnotationMirrorValue((AnnotationMirror) pluginAnnotationValue.getValue(), "value" /* todo runtime check */)).asElement()).getQualifiedName().toString();
            final String[] pluginClassArguments = pluginAnnotation.args();

            final NodeTreeHandlePlugin plugin;
            try {
                plugin = getPluginInstance(pluginClassName);
            } catch (Exception e) {
                showErrorTip(
                        new AptProcessException(
                                String.format("found error on create plugin instance: %s", e.getMessage()),
                                nodeParserGlobalConfigHost, nodeParserGlobalConfigMirror, pluginAnnotationValue
                        )
                );
                return;
            }

            try {

                @SuppressWarnings("unchecked") final List<NodeContainerEntity>[] treeNodeContainerEntities = new List[1];
                @SuppressWarnings("unchecked") final List<NodeTree>[] nodeTrees = new List[1];

                plugin.onNodeTreeParsed(new NodeTreeHandlePlugin.PluginContext() {
                    @Override public ProcessingEnvironment processingEnvironment() { return processingEnv; }
                    @Override public String[] args() { return pluginClassArguments; }
                    @Override public List<NodeContainerEntity> flatNodeContainerEntities() { return nodeContainerEntities; }
                    @Override public List<NodeContainerEntity> treeNodeContainerEntities() {
                        if (treeNodeContainerEntities[0] == null) {
                            treeNodeContainerEntities[0] = NodeContainerAnnotationParser.convertNodeContainersToTree(flatNodeContainerEntities());
                        }
                        return treeNodeContainerEntities[0];
                    }
                    @Override public List<NodeTree> nodeTree() {
                        if (nodeTrees[0] == null) {
                            nodeTrees[0] = NodeContainerAnnotationParser.convertNodeContainersToNodeTree(treeNodeContainerEntities());
                        }
                        return nodeTrees[0];
                    }
                });
            } catch (Exception e) {
                throw new AptProcessException(
                        String.format("found error on process %s '%s': %s", pluginProcessDesc, plugin.getClass().getCanonicalName(), e.getMessage()),
                        e, nodeParserGlobalConfigHost, nodeParserGlobalConfigMirror, pluginAnnotationValue
                );
            }
        }
    }

    private NodeTreeHandlePlugin getPluginInstance(String pluginClassName) {

        final Class<?> pluginClass;
        try {
            pluginClass = Class.forName(pluginClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(String.format("can't find plugin's class '%s', please check classpath", pluginClassName), e);
        }

        if (!NodeTreeHandlePlugin.class.isAssignableFrom(pluginClass)) {
            throw new IllegalArgumentException(String.format("class '%s' is not %s's subtype, please check plugin's type", pluginClassName, NodeTreeHandlePlugin.class.getCanonicalName()));
        }

        try {
            return (NodeTreeHandlePlugin) pluginClass.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(String.format("can't create '%s's instance: %s", pluginClassName, e.getMessage()), e);
        }
    }

    private void nodeContainerNodeTypeRepeatCheck(List<NodeContainerEntity> entities) throws AptProcessException {
        Map<String, NodeContainerEntity> repeatCheckContainer = new HashMap<>();
        for (NodeContainerEntity nodeTreeEntity : entities) {
            for (NodeContainerEntity.Node node : nodeTreeEntity.nodes) {
                NodeContainerEntity repeatNodeTreeEntity = repeatCheckContainer.put(node.name, nodeTreeEntity);
                if (repeatNodeTreeEntity != null) {
                    throw new AptProcessException(
                            String.format(
                                    "found repeat root node name '%s' on %s and %s, please check root node or nodeContainer's link relations",
                                    node.name,
                                    repeatNodeTreeEntity.host.getQualifiedName(),
                                    nodeTreeEntity.host.getQualifiedName()
                            ),
                            nodeTreeEntity.host
                    );
                }
            }
        }
    }

    private void checkNodeParserGlobalConfig(RoundEnvironment roundEnv) throws AptProcessException {
        final Set<TypeElement> hosts = ElementFilter.typesIn(
                roundEnv.getElementsAnnotatedWith(NodeParserGlobalConfig.class)
        );

        if (hosts.isEmpty()) {
            return;
        }

        boolean isRepeat = nodeParserGlobalConfig != null || hosts.size() > 1;

        if (isRepeat) {

            final LinkedList<TypeElement> repeatElements = new LinkedList<>(hosts);
            if (nodeParserGlobalConfigHost != null) {
                repeatElements.add(nodeParserGlobalConfigHost);
            }

            final Iterator<TypeElement> elementIterator = repeatElements.iterator();
            final StringBuilder elementsTip = new StringBuilder();
            while (elementIterator.hasNext()) {
                elementsTip.append(
                        elementIterator.next().getQualifiedName().toString()
                );
                if (elementIterator.hasNext()) {
                    elementsTip.append(",");
                }
            }

            throw new AptProcessException(
                    String.format(
                            "%s annotation only can exist one, but found more: %s",
                            NodeParserGlobalConfig.class.getSimpleName(), elementsTip
                    ),
                    repeatElements.get(0)
            );
        }

        final TypeElement host = hosts.iterator().next();
        final NodeParserGlobalConfig config = host.getAnnotation(NodeParserGlobalConfig.class);

        AnnotationMirror mirror = null;
        for (AnnotationMirror annotationMirror : host.getAnnotationMirrors()) {
            if (annotationMirror.getAnnotationType().equals(processingEnv.getElementUtils().getTypeElement(NodeParserGlobalConfig.class.getCanonicalName()).asType())) {
                mirror = annotationMirror;
                break;
            }
        }
        assert mirror != null;

        nodeParserGlobalConfigHost = host;
        nodeParserGlobalConfigMirror = mirror;
        nodeParserGlobalConfig = config;
    }

    private void showErrorTip(AptProcessException e) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        e.printStackTrace(new PrintStream(os));
        processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                os.toString(),
                e.e, e.a, e.v
        );
    }
}
