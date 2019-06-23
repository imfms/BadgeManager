
package ms.imf.redpoint.compiler.plugin.node.helper.hardcode.generator;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;

import ms.imf.redpoint.compiler.plugin.AptProcessException;
import ms.imf.redpoint.compiler.plugin.PathEntity;

/**
 * @see NodeHelperHardcodeGeneratorCompilerPlugin
 */
class Generator {

    private final Filer aptFiler;

    Generator(Filer aptFiler) {
        if (aptFiler == null) {
            throw new IllegalArgumentException("aptFiler can't be null");
        }
        this.aptFiler = aptFiler;
    }

    void generate(List<PathEntity> paths) throws AptProcessException {
        for (PathEntity path : paths) {
            generatePath(path);
        }
    }

    private void generatePath(PathEntity path) throws AptProcessException {

        final String packageName = getElementPackage(path.host).getQualifiedName().toString();
        final String className = path.host.getSimpleName().toString() + "_Path";

        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(className)
                .addModifiers(Modifier.PUBLIC);

        for (PathEntity.Node node : path.nodes) {
            builder.addType(
                    generateNode(node, new HashSet<>(Collections.singletonList(className)))
            );
        }

        try {
            JavaFile
                    .builder(packageName, builder.build())
                    .build()
                    .writeTo(aptFiler);
        } catch (IOException e) {
            throw new AptProcessException(
                    String.format("found error on generate helper code: %s", e.getMessage()),
                    e,
                    path.host
            );
        }
    }

    private TypeSpec generateNode(PathEntity.Node node, Set<String> parentLockedClassNames) {

        // node.type
        final String className = generateStandardIdentifier(parentLockedClassNames, node.type);
        TypeSpec.Builder typeBuilder = TypeSpec.interfaceBuilder(className)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

        typeBuilder.addField(
                FieldSpec.builder(String.class, "name$", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$S", node.type)
                        .build()
        );

        // node.args
        if (node.args != null) {
            for (PathEntity.NodeArg arg : node.args) {
                String argFieldName = generateStandardIdentifier(String.format("arg$%s", arg.name));
                typeBuilder.addField(
                        FieldSpec.builder(String.class, argFieldName, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                                .initializer("$S", arg.name)
                                .build()
                );
                if (arg.valueLimits != null) {
                    Set<String> lockedNames = new HashSet<>(
                            Collections.singleton(argFieldName)
                    );
                    for (String valueLimit : arg.valueLimits) {

                        String argValueFieldName = generateStandardIdentifier(lockedNames, String.format("%s$%s", argFieldName, valueLimit));
                        typeBuilder.addField(
                                FieldSpec.builder(String.class, argValueFieldName, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                                        .initializer("$S", valueLimit)
                                        .build()
                        );

                        lockedNames.add(argValueFieldName);
                    }
                }


            }
        }

        // node.sub
        if (node.sub != null) {
            for (PathEntity.Node subNode : node.sub) {
                HashSet<String> lockedClassNames = new HashSet<>(parentLockedClassNames);
                lockedClassNames.add(className);
                typeBuilder.addType(generateNode(subNode, lockedClassNames));
            }
        }

        return typeBuilder.build();
    }

    private PackageElement getElementPackage(Element element) {

        Element result = element;

        while (!(result instanceof PackageElement)) {
            result = result.getEnclosingElement();
        }

        return (PackageElement) result;
    }

    /**
     * https://docs.oracle.com/javase/specs/jls/se12/html/jls-3.html#jls-3.9
     */
    private static final String[] JAVA_KEYWORDS = {
            "abstract", "continue", "for", "new", "switch", "assert", "default", "if", "package",
            "synchronized", "boolean", "do", "goto", "private", "this", "break", "double", "implements",
            "protected", "throw", "byte", "else", "import", "public", "throws", "case", "enum",
            "instanceof", "return", "transient", "catch", "extends", "int", "short", "try", "char",
            "final", "interface", "static", "void", "class", "finally", "long", "strictfp", "volatile",
            "const", "float", "native", "super", "while", "_",
            "null", "true", "false"
    };

    private static String generateStandardIdentifier(String name) {
        return generateStandardIdentifier(Collections.<String>emptySet(), name);
    }

    /**
     * https://docs.oracle.com/javase/specs/jls/se12/html/jls-3.html#jls-IdentifierChars
     */
    private static String generateStandardIdentifier(Set<String> lockedNames, String typeName) {
        final StringBuilder typeNameBuffer = new StringBuilder(typeName);

        if (Arrays.asList(JAVA_KEYWORDS).contains(typeName)) {
            typeNameBuffer.insert(0, '_');
        }

        if (!Character.isJavaIdentifierStart(typeNameBuffer.charAt(0))) {
            typeNameBuffer.insert(0, '_');
        }

        for (int i = 0; i < typeNameBuffer.length(); i++) {
            if (!Character.isJavaIdentifierPart(typeNameBuffer.charAt(i))) {
                typeNameBuffer.replace(i, i + 1, "_");
            }
        }

        if (!lockedNames.contains(typeNameBuffer.toString())) {
            return typeNameBuffer.toString();
        }

        for (int i = 2; ; i++) {
            String finalName = String.format("%s_%s", typeNameBuffer, i);
            if (!lockedNames.contains(finalName)) {
                return finalName;
            }
        }
    }
}