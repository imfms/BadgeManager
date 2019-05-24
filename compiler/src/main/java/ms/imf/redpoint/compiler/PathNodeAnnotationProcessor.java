package ms.imf.redpoint.compiler;


import com.google.auto.service.AutoService;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintStream;
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
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

import ms.imf.redpoint.annotation.Path;
import ms.imf.redpoint.annotation.PathAptGlobalConfig;
import ms.imf.redpoint.converter.ArgCheckUtil;
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

        for (Class annotationClass : new Class[]{ Path.class }) {
            supportedTypes.add(annotationClass.getCanonicalName());
        }

        return supportedTypes;
    }

    private final List<PathEntity> allPathEntities = new LinkedList<>();

    private TypeElement lastPathAptGlobalConfigAnnotationHost;
    private PathAptGlobalConfig pathAptGlobalConfig;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        // check config
        try {
            checkPathAptGlobalConfig(roundEnv);
        } catch (CompilerException e) {
            showErrorTip(e);
            return false;
        }

        // parse paths
        final Set<TypeElement> pathAnnotationTypes = ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(Path.class));

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

        final List<NodeSchema> nodeSchemas = PathNodeAnnotationParser.generateNodeSchemaTree(treePathEntities);

        // convert config check
        if (!pathAptGlobalConfig.convertCheckConfigFilePath().isEmpty()) {

            InputStream convertCheckFileInputStream;
            try {
                convertCheckFileInputStream = new FileInputStream(pathAptGlobalConfig.convertCheckConfigFilePath());
            } catch (FileNotFoundException e) {
                showErrorTip(new CompilerException(
                        String.format("PathAptGlobalConfig's convertCheckConfigFilePath(%s) not exist", pathAptGlobalConfig.convertCheckConfigFilePath()),
                        e,
                        lastPathAptGlobalConfigAnnotationHost
                ));
                return false;
            }

            try {
                ArgCheckUtil.checkArg(convertCheckFileInputStream, nodeSchemas);
            } catch (IllegalArgumentException e) {
                showErrorTip(new CompilerException(
                        String.format("found error on convert config check: %s", e.getMessage()),
                        e
                ));
            }
        }

        // TODO: 19-5-22 node schema output

        return true;
    }

    private void pathNodeTypeRepeatCheck(List<PathEntity> treePathEntities) throws CompilerException {
        Map<String, PathEntity> repeatCheckContainer = new HashMap<>();
        for (PathEntity pathEntity : treePathEntities) {
            for (int i = 0; i < pathEntity.nodes.size(); i++) {
                PathEntity.Node node = pathEntity.nodes.get(i);
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

        lastPathAptGlobalConfigAnnotationHost = host;
        pathAptGlobalConfig = config;
    }

    private void showErrorTip(CompilerException e) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        e.printStackTrace(new PrintStream(os));
        processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                os.toString(),
                e.e,
                e.a,
                e.v
        );
    }
}
