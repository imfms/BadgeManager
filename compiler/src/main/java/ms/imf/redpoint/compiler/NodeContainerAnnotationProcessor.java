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
import ms.imf.redpoint.compiler.plugin.NodeTreeParsedHandlerPlugin;
import ms.imf.redpoint.compiler.plugin.NodeContainerAnnotationEntity;
import ms.imf.redpoint.entity.NodeTree;

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

    private final List<NodeContainerAnnotationEntity> allNodeTreeEntities = new LinkedList<>();

    private TypeElement lastPathAptGlobalConfigAnnotationHost;
    private NodeParserGlobalConfig nodeParserGlobalConfig;
    private AnnotationMirror pathAptGlobalConfigMirror;

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
            checkPathAptGlobalConfig(roundEnv);
        } catch (AptProcessException e) {
            showErrorTip(e);
            return false;
        }

        final Set<TypeElement> pathAnnotationTypes = ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(NodeContainer.class));

        // parse paths
        final NodeContainerAnnotationParser nodeContainerAnnotationParser = new NodeContainerAnnotationParser(processingEnv.getElementUtils());
        final List<NodeContainerAnnotationEntity> pathEntities;
        try {
            pathEntities = nodeContainerAnnotationParser.parsePaths(pathAnnotationTypes);
        } catch (AptProcessException e) {
            showErrorTip(new AptProcessException(String.format("found error on parsing @NodeContainer: %s", e.getMessage()), e));
            return false;
        }

        if (nodeParserGlobalConfig != null) {
            // process each apt round plugin
            try {
                processPlugins(
                        "each apt round plugin",
                        NodeContainerAnnotationParser.<List<AnnotationValue>>getAnnotionMirrorValue(pathAptGlobalConfigMirror, "eachAptRoundNodeTreeParsedPlugins" /* todo runtime check */),
                        nodeParserGlobalConfig.eachAptRoundNodeTreeParsedPlugins(),
                        pathEntities
                );
            } catch (AptProcessException e) {
                showErrorTip(e);
                return false;
            }
        }

        // compose each round data
        allNodeTreeEntities.addAll(pathEntities);

        if (!roundEnv.processingOver()) {
            return true;
        }
        // process finish task

        final List<NodeContainerAnnotationEntity> treePathEntities = NodeContainerAnnotationParser.convertPathTree(allNodeTreeEntities);

        // root node repeat check
        try {
            pathNodeTypeRepeatCheck(treePathEntities);
        } catch (AptProcessException e) {
            showErrorTip(e);
            return false;
        }

        // path node value check
        try {
            checkPathNodeType(allNodeTreeEntities, treePathEntities);
        } catch (AptProcessException e) {
            showErrorTip(e);
            return false;
        }

        if (nodeParserGlobalConfig != null) {
            // process last apt round plugin
            try {
                processPlugins(
                        "last apt round plugin",
                        NodeContainerAnnotationParser.<List<AnnotationValue>>getAnnotionMirrorValue(pathAptGlobalConfigMirror, "lastAptRoundNodeTreeParsedPlugins" /* todo runtime check */),
                        nodeParserGlobalConfig.lastAptRoundNodeTreeParsedPlugins(),
                        allNodeTreeEntities
                );
            } catch (AptProcessException e) {
                showErrorTip(e);
                return false;
            }
        }

        return true;
    }

    private void checkPathNodeType(List<NodeContainerAnnotationEntity> allPathEntities, List<NodeContainerAnnotationEntity> treePathEntities) throws AptProcessException {
        for (NodeContainerAnnotationEntity nodeTreeEntity : allPathEntities) {
            boolean isRootNode = treePathEntities.contains(nodeTreeEntity);
            switch (nodeTreeEntity.host.getAnnotation(NodeContainer.class).type()) {
                case ROOT_NODE:
                    if (!isRootNode) {
                        NodeContainerAnnotationEntity parentPathEntiity = findParentPathEntity(nodeTreeEntity, treePathEntities);
                        throw new AptProcessException(
                                String.format(
                                        "'%s's PathNode should be a root node, but it's a sub node, it has a parent '%s', please check path's link relations",
                                        nodeTreeEntity.host.getQualifiedName(),
                                        parentPathEntiity.host.getQualifiedName()
                                ),
                                nodeTreeEntity.host
                        );
                    }
                    break;
                case SUB_NODE:
                    if (isRootNode) {
                        throw new AptProcessException(
                                String.format(
                                        "'%s's PathNode should be a sub node, but it's a root node, please check path's link relations",
                                        nodeTreeEntity.host.getQualifiedName()
                                ),
                                nodeTreeEntity.host
                        );
                    }
                    break;
                case UNLIMIT:
                default:
                    break;
            }
        }
    }

    private NodeContainerAnnotationEntity findParentPathEntity(NodeContainerAnnotationEntity sourceNodeTreeEntity, List<NodeContainerAnnotationEntity> treePathEntities) {
        for (NodeContainerAnnotationEntity nodeTreeEntity : treePathEntities) {
            NodeContainerAnnotationEntity result = findParentPathEntity(sourceNodeTreeEntity, nodeTreeEntity);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private NodeContainerAnnotationEntity findParentPathEntity(NodeContainerAnnotationEntity sourceNodeTreeEntity, NodeContainerAnnotationEntity nodeTreeEntity) {
        for (NodeContainerAnnotationEntity.Node node : nodeTreeEntity.nodes) {
            if (node.subRef == sourceNodeTreeEntity) {
                return nodeTreeEntity;
            } else {
                return findParentPathEntity(sourceNodeTreeEntity, node.subRef);
            }
        }
        return null;
    }

    private void processPlugins(String pluginProcessDesc, List<AnnotationValue> pluginAnnotationValues, Plugin[] plugins, final List<NodeContainerAnnotationEntity> allPathEntities) throws AptProcessException {

        if (pluginAnnotationValues == null
                || pluginAnnotationValues.isEmpty()
                || plugins == null
                || plugins.length <= 0) {
            return;
        }

        for (int i = 0; i < plugins.length; i++) {
            Plugin pluginAnnotation = plugins[i];
            AnnotationValue pluginAnnotationValue = pluginAnnotationValues.get(i);

            final String pluginClassName = ((TypeElement) ((DeclaredType) NodeContainerAnnotationParser.getAnnotionMirrorValue((AnnotationMirror) pluginAnnotationValue.getValue(), "value" /* todo runtime check */)).asElement()).getQualifiedName().toString();
            final String[] pluginClassArguments = pluginAnnotation.args();

            final NodeTreeParsedHandlerPlugin plugin;
            try {
                plugin = getPluginInstance(pluginClassName);
            } catch (Exception e) {
                showErrorTip(
                        new AptProcessException(
                                String.format("found error on create plugin instance: %s", e.getMessage()),
                                lastPathAptGlobalConfigAnnotationHost, pathAptGlobalConfigMirror, pluginAnnotationValue
                        )
                );
                return;
            }

            try {

                @SuppressWarnings("unchecked") final List<NodeContainerAnnotationEntity>[] treePathEntities = new List[1];
                @SuppressWarnings("unchecked") final List<NodeTree>[] treeNodeSchemas = new List[1];

                plugin.onNodeTreeParsed(new NodeTreeParsedHandlerPlugin.PluginContext() {
                    @Override
                    public ProcessingEnvironment processingEnvironment() {
                        return processingEnv;
                    }

                    @Override
                    public String[] args() {
                        return pluginClassArguments;
                    }

                    @Override
                    public List<NodeContainerAnnotationEntity> flatNodeContainerEntities() {
                        return allPathEntities;
                    }

                    @Override
                    public List<NodeContainerAnnotationEntity> treeNodeContainerEntities() {
                        if (treePathEntities[0] == null) {
                            treePathEntities[0] = NodeContainerAnnotationParser.convertPathTree(flatNodeContainerEntities());
                        }
                        return treePathEntities[0];
                    }

                    @Override
                    public List<NodeTree> nodeTree() {
                        if (treeNodeSchemas[0] == null) {
                            treeNodeSchemas[0] = NodeContainerAnnotationParser.generateNodeSchemaTree(treeNodeContainerEntities());
                        }
                        return treeNodeSchemas[0];
                    }
                });
            } catch (Exception e) {
                throw new AptProcessException(
                        String.format("found error on process %s '%s': %s", pluginProcessDesc, plugin.getClass().getCanonicalName(), e.getMessage()),
                        e, lastPathAptGlobalConfigAnnotationHost, pathAptGlobalConfigMirror, pluginAnnotationValue
                );
            }
        }
    }

    private NodeTreeParsedHandlerPlugin getPluginInstance(String pluginClassName) {

        final Class<?> pluginClass;
        try {
            pluginClass = Class.forName(pluginClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(String.format("can't find plugin's class '%s', please check classpath", pluginClassName), e);
        }

        if (!NodeTreeParsedHandlerPlugin.class.isAssignableFrom(pluginClass)) {
            throw new IllegalArgumentException(String.format("class '%s' is not %s's subtype, please check plugin's type", pluginClassName, NodeTreeParsedHandlerPlugin.class.getCanonicalName()));
        }

        try {
            return (NodeTreeParsedHandlerPlugin) pluginClass.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(String.format("can't create '%s's instance: %s", pluginClassName, e.getMessage()), e);
        }
    }

    private void pathNodeTypeRepeatCheck(List<NodeContainerAnnotationEntity> treePathEntities) throws AptProcessException {
        Map<String, NodeContainerAnnotationEntity> repeatCheckContainer = new HashMap<>();
        for (NodeContainerAnnotationEntity nodeTreeEntity : treePathEntities) {
            for (NodeContainerAnnotationEntity.Node node : nodeTreeEntity.nodes) {
                NodeContainerAnnotationEntity repeatNodeTreeEntity = repeatCheckContainer.put(node.name, nodeTreeEntity);
                if (repeatNodeTreeEntity != null) {
                    throw new AptProcessException(
                            String.format(
                                    "found repeat root node name '%s' on %s and %s, please check root node or path's link relations",
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

    private void checkPathAptGlobalConfig(RoundEnvironment roundEnv) throws AptProcessException {
        final Set<TypeElement> annotationElements = ElementFilter.typesIn(
                roundEnv.getElementsAnnotatedWith(NodeParserGlobalConfig.class)
        );

        if (annotationElements.isEmpty()) {
            return;
        }

        boolean isRepeat = nodeParserGlobalConfig != null || annotationElements.size() > 1;

        if (isRepeat) {

            final LinkedList<TypeElement> repeatElements = new LinkedList<>(annotationElements);
            if (lastPathAptGlobalConfigAnnotationHost != null) {
                repeatElements.add(lastPathAptGlobalConfigAnnotationHost);
            }

            final Iterator<TypeElement> elementIterator = repeatElements.iterator();
            final StringBuilder elementsTip = new StringBuilder();
            while (elementIterator.hasNext()) {
                elementsTip.append(
                        elementIterator.next().getQualifiedName().toString()
                );
                if (elementIterator.hasNext()) {
                    elementsTip.append(",").append(' ');
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

        final TypeElement host = annotationElements.iterator().next();
        final NodeParserGlobalConfig config = host.getAnnotation(NodeParserGlobalConfig.class);

        AnnotationMirror configMirror = null;
        for (AnnotationMirror annotationMirror : host.getAnnotationMirrors()) {
            if (annotationMirror.getAnnotationType().equals(processingEnv.getElementUtils().getTypeElement(NodeParserGlobalConfig.class.getCanonicalName()).asType())) {
                configMirror = annotationMirror;
                break;
            }
        }
        assert configMirror != null;

        lastPathAptGlobalConfigAnnotationHost = host;
        pathAptGlobalConfigMirror = configMirror;
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
