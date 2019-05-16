package ms.imf.redpoint.compiler;


import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

import ms.imf.redpoint.annotation.Path;

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

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        TypeElement pathAnnotationTypeElement = processingEnv.getElementUtils().getTypeElement(Path.class.getCanonicalName());
        if (!annotations.contains(pathAnnotationTypeElement)) {
            return false;
        }

        Set<TypeElement> pathAnnotationTypes = ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(Path.class));

        PathNodeAnnotationParser pathNodeAnnotationParser = new PathNodeAnnotationParser(processingEnv.getElementUtils());
        final List<PathEntity> pathEntities;
        try {
            pathEntities = pathNodeAnnotationParser.parsePaths(pathAnnotationTypes);
        } catch (CompilerException e) {
            showErrorTip(e);
            return false;
        }

        PathNodeHelperCodeGenerator pathNodeHelperCodeGenerator = new PathNodeHelperCodeGenerator(processingEnv.getFiler());
        try {
            pathNodeHelperCodeGenerator.generate(pathEntities);
        } catch (CompilerException e) {
            showErrorTip(e);
            return false;
        }

        return true;
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
