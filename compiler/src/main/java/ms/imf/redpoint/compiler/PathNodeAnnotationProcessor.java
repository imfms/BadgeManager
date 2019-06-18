package ms.imf.redpoint.compiler;


import com.google.auto.service.AutoService;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

import ms.imf.redpoint.annotation.Path;
import ms.imf.redpoint.annotation.PathAptGlobalConfig;
import ms.imf.redpoint.annotation.Plugin;
import ms.imf.redpoint.compiler.plugin.ParsedNodeSchemaHandlePlugin;
import ms.imf.redpoint.compiler.plugin.PathEntity;
import ms.imf.redpoint.entity.NodeSchema;

@AutoService(Processor.class)
public class PathNodeAnnotationProcessor extends AbstractProcessor {

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        final LinkedHashSet<String> supportedTypes = new LinkedHashSet<>();

        for (Class annotationClass : new Class[]{ Path.class, PathAptGlobalConfig.class }) {
            supportedTypes.add(annotationClass.getCanonicalName());
        }

        return supportedTypes;
    }

    private final List<PathEntity> allPathEntities = new LinkedList<>();
    private final Gson gson = new Gson();

    private TypeElement lastPathAptGlobalConfigAnnotationHost;
    private PathAptGlobalConfig pathAptGlobalConfig;
    private AnnotationMirror pathAptGlobalConfigMirror;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            return processRaw(roundEnv);
        } catch (Exception e) {
            showErrorTip(new CompilerException(String.format("unexpected exception on processor: %s", e.getMessage()), e));
            return false;
        }
    }

    private boolean processRaw(RoundEnvironment roundEnv) {
        // check config
        try {
            checkPathAptGlobalConfig(roundEnv);
        } catch (CompilerException e) {
            showErrorTip(e);
            return false;
        }

        final Set<TypeElement> pathAnnotationTypes = ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(Path.class));

        // parse paths
        final PathNodeAnnotationParser pathNodeAnnotationParser = new PathNodeAnnotationParser(processingEnv.getElementUtils());
        final List<PathEntity> pathEntities;
        try {
            pathEntities = pathNodeAnnotationParser.parsePaths(pathAnnotationTypes);
        } catch (CompilerException e) {
            showErrorTip(e);
            return false;
        }

        // generate node helper code
        PathNodeHelperCodeGenerator pathNodeHelperCodeGenerator = new PathNodeHelperCodeGenerator(processingEnv.getFiler());
        try {
            pathNodeHelperCodeGenerator.generate(pathEntities);
        } catch (CompilerException e) {
            showErrorTip(e);
            return false;
        }

        // compose each round data
        allPathEntities.addAll(pathEntities);

        if (!roundEnv.processingOver()) {
            return true;
        }
        // process finish task

        final List<PathEntity> treePathEntities = PathNodeAnnotationParser.convertPathTree(allPathEntities);

        // root node repeat check
        try {
            pathNodeTypeRepeatCheck(treePathEntities);
        } catch (CompilerException e) {
            showErrorTip(e);
            return false;
        }

        if (pathAptGlobalConfig == null) {
            return true;
        }

        final List<NodeSchema> treeNodeSchemas = PathNodeAnnotationParser.generateNodeSchemaTree(treePathEntities);

        // process plugin
        List<AnnotationValue> pluginAnnotationValues = PathNodeAnnotationParser.getAnnotionMirrorValue(pathAptGlobalConfigMirror, "plugins");
        if (pluginAnnotationValues == null) {
            pluginAnnotationValues = Collections.emptyList();
        }
        Plugin[] plugins = pathAptGlobalConfig.plugins();

        for (int i = 0; i < plugins.length; i++) {
            Plugin pluginAnnotation = plugins[i];
            AnnotationValue pluginAnnotationValue = pluginAnnotationValues.get(i);

            String pluginClassName = ((TypeElement) ((DeclaredType) PathNodeAnnotationParser.getAnnotionMirrorValue((AnnotationMirror) pluginAnnotationValue.getValue(), "value")).asElement()).getQualifiedName().toString();
            String[] pluginClassArguments = pluginAnnotation.args();

            final ParsedNodeSchemaHandlePlugin plugin;
            try {
                plugin = getPluginInstance(pluginClassName);
            } catch (Exception e) {
                showErrorTip(
                        new CompilerException(
                                String.format("found error on create plugin instance: %s", e.getMessage()),
                                lastPathAptGlobalConfigAnnotationHost, pathAptGlobalConfigMirror, pluginAnnotationValue
                        )
                );
                return false;
            }

            try {
                plugin.onParsed(processingEnv, pluginClassArguments, treePathEntities, treeNodeSchemas);
            } catch (Exception e) {
                showErrorTip(
                        new CompilerException(
                                String.format("found error on process plugin '%s': %s", plugin.getClass().getCanonicalName(), e.getMessage()),
                                e, lastPathAptGlobalConfigAnnotationHost, pathAptGlobalConfigMirror, pluginAnnotationValue
                        )
                );
                return false;
            }
        }

        return true;
    }

    private ParsedNodeSchemaHandlePlugin getPluginInstance(String pluginClassName) {

        final Class<?> pluginClass;
        try {
            pluginClass = Class.forName(pluginClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(String.format("can't find class '%s', please check classpath", pluginClassName), e);
        }

        if (!ParsedNodeSchemaHandlePlugin.class.isAssignableFrom(pluginClass)) {
            throw new IllegalArgumentException(String.format("plugin class '%s' is not %s's instance, please check plugin's type", pluginClassName, ParsedNodeSchemaHandlePlugin.class.getCanonicalName()));
        }

        try {
            return (ParsedNodeSchemaHandlePlugin) pluginClass.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException(String.format("can't create '%s's instance: %s", pluginClassName, e.getMessage()), e);
        }
    }

    private void pathNodeTypeRepeatCheck(List<PathEntity> treePathEntities) throws CompilerException {
        Map<String, PathEntity> repeatCheckContainer = new HashMap<>();
        for (PathEntity pathEntity : treePathEntities) {
            for (PathEntity.Node node : pathEntity.nodes) {
                PathEntity repeatPathEntity = repeatCheckContainer.put(node.type, pathEntity);
                if (repeatPathEntity != null) {
                    throw new CompilerException(
                            String.format(
                                    "found repeat root node type '%s' on %s and %s, please check root type or path's link",
                                    node.type,
                                    repeatPathEntity.host.getQualifiedName(),
                                    pathEntity.host.getQualifiedName()
                            ),
                            pathEntity.host
                    );
                }
            }
        }
    }

    private void checkPathAptGlobalConfig(RoundEnvironment roundEnv) throws CompilerException {
        final Set<TypeElement> annotationElements = ElementFilter.typesIn(
                roundEnv.getElementsAnnotatedWith(PathAptGlobalConfig.class)
        );

        if (annotationElements.isEmpty()) {
            return;
        }

        boolean isRepeat = pathAptGlobalConfig != null || annotationElements.size() > 1;

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

            throw new CompilerException(String.format("PathAptGlobalConfig only can exist one, but found these: %s", elementsTip), repeatElements.get(0));
        }

        final TypeElement host = annotationElements.iterator().next();
        final PathAptGlobalConfig config = host.getAnnotation(PathAptGlobalConfig.class);

        AnnotationMirror configMirror = null;
        for (AnnotationMirror annotationMirror : host.getAnnotationMirrors()) {
            if (annotationMirror.getAnnotationType().equals(processingEnv.getElementUtils().getTypeElement(PathAptGlobalConfig.class.getCanonicalName()).asType())) {
                configMirror = annotationMirror;
                break;
            }
        }
        assert configMirror != null;

        lastPathAptGlobalConfigAnnotationHost = host;
        pathAptGlobalConfigMirror = configMirror;
        pathAptGlobalConfig = config;
    }

    private void showErrorTip(CompilerException e) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        e.printStackTrace(new PrintStream(os));
        processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                os.toString(),
                e.e, e.a, e.v
        );
    }
}
