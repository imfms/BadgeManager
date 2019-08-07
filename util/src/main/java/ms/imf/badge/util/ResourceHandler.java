package ms.imf.badge.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.StandardLocation;

/**
 * ResourceHandler
 *
 * 支持资源类型： 其中方括号[]包裹的内容为可选项:
 * <pre>
 *  file://[filePath/]fileName: 常规文件系统绝对、相对位置
 *    例如: file:///home/user/resource.txt, file://resource.txt
 *  javaResource://[packageName/]resourceName: java-style资源位置
 *    例如: javaResource://com.foo/resource.txt, javaResource://resource.txt
 * </pre>
 *
 * @author f_ms
 * @date 2019/7/24
 */
public class ResourceHandler {

    public static final String RESOURCE_TYPE_SCHEMA_SYMBOL = "://";

    public static final String RESOURCE_TYPE_NAME_FILE = "file";
    public static final String RESOURCE_TYPE_NAME_JAVA_RESOURCE = "javaResource";

    public static final String RESOURCE_TYPE_FILE = RESOURCE_TYPE_NAME_FILE + RESOURCE_TYPE_SCHEMA_SYMBOL;
    public static final String RESOURCE_TYPE_JAVA_RESOURCE = RESOURCE_TYPE_NAME_JAVA_RESOURCE + RESOURCE_TYPE_SCHEMA_SYMBOL;

    public static String[] acceptTypes() {
        return TypeHandler.acceptTypes();
    }

    public static InputStream read(ProcessingEnvironment processingEnvironment, String resourceStr) throws Exception {
        final String[] resource = parseResourceStr(resourceStr);
        return getTypeHandler(resource[0], resource[1]).read(processingEnvironment, resource[0], resource[1]);
    }

    public static OutputStream write(ProcessingEnvironment processingEnvironment, String resourceStr) throws Exception {
        final String[] resource = parseResourceStr(resourceStr);
        return getTypeHandler(resource[0], resource[1]).write(processingEnvironment, resource[0], resource[1]);
    }

    private enum TypeHandler {
        /**
         * file
         */
        FILE(RESOURCE_TYPE_NAME_FILE) {
            @Override
            InputStream read(ProcessingEnvironment processingEnvironment, String type, String location) throws Exception {
                return new FileInputStream(location);
            }

            @Override
            OutputStream write(ProcessingEnvironment processingEnvironment, String type, String location) throws Exception {
                return new FileOutputStream(location);
            }
        },
        /**
         * java-style-resource
         */
        JAVA_RESOURCE(RESOURCE_TYPE_NAME_JAVA_RESOURCE) {
            @Override
            InputStream read(ProcessingEnvironment processingEnvironment, String type, String location) throws Exception {

                /*
                javaStylePath    package  resourceName
                -------------    -------  ------------
                'a.b.c/d.txt' -> 'a.b.c', 'd.txt'
                'd.txt'       -> ''     , 'd.txt'
                 */

                final String packageName;
                final String resourceName;

                int pathSymbolIndex = location.indexOf('/');
                if (pathSymbolIndex < 0) {
                    packageName = "";
                    resourceName = location;
                } else {
                    packageName = location.substring(0, pathSymbolIndex);
                    resourceName = location.substring(pathSymbolIndex + 1, location.length());
                }

                /*
                 'd.txt' -> '/d.txt'
                 'a.b.c/d.txt' -> '/a/b/c/d.txt'
                  */
                final String rawLocation;
                if (packageName.isEmpty()) {
                    rawLocation = "/" + resourceName;
                } else {
                    rawLocation = String.format(
                            "/%s/%s",
                            packageName.replace('.', '/'),
                            resourceName
                    );
                }

                InputStream result = ResourceHandler.class.getClassLoader().getResourceAsStream(rawLocation);

                if (result == null) {
                    throw new FileNotFoundException(String.format("can't find resource: %s://%s", type, location));
                }

                return result;
            }

            @Override
            OutputStream write(ProcessingEnvironment processingEnvironment, String type, String location) throws Exception {

                final String resourcePackage;
                final String resourceName;

                int splitIndex = location.indexOf('/');
                if (splitIndex < 0) {
                    resourcePackage = "";
                    resourceName = location;
                } else {
                    resourcePackage = location.substring(0, splitIndex);
                    resourceName = location.substring(splitIndex + 1);
                }

                return processingEnvironment.getFiler()
                        .createResource(StandardLocation.CLASS_OUTPUT, resourcePackage, resourceName)
                        .openOutputStream();
            }
        };

        private final String type;

        TypeHandler(String type) {
            this.type = type;
        }

        boolean accept(String type, String location) {
            return this.type.equals(type);
        }

        abstract InputStream read(ProcessingEnvironment processingEnvironment, String type, String location) throws Exception;

        abstract OutputStream write(ProcessingEnvironment processingEnvironment, String type, String location) throws Exception;

        public static String[] acceptTypes() {
            String[] types = new String[values().length];

            for (int i = 0; i < types.length; i++) {
                types[i] = values()[i].type;
            }

            return types;
        }
    }

    private static TypeHandler getTypeHandler(String type, String location) throws IllegalArgumentException {
        for (TypeHandler typeHandler : TypeHandler.values()) {
            if (typeHandler.accept(type, location)) {
                return typeHandler;
            }
        }

        throw new IllegalArgumentException(String.format(
                "unaccept resource type '%s', I only accept resourceType: %s",
                type,
                Arrays.toString(TypeHandler.acceptTypes())
        ));
    }

    private static String[] parseResourceStr(String arg) throws IllegalArgumentException {

        int schemaSymbolIndex = arg.indexOf(RESOURCE_TYPE_SCHEMA_SYMBOL);
        if (schemaSymbolIndex < 0) {
            throw new IllegalArgumentException(String.format(
                    "can't find resource type schema symbol '%s' in resourceStr: %s",
                    RESOURCE_TYPE_SCHEMA_SYMBOL, arg
            ));
        }

        String type = arg.substring(0, schemaSymbolIndex);
        String location = arg.substring(schemaSymbolIndex + RESOURCE_TYPE_SCHEMA_SYMBOL.length(), arg.length());

        return new String[]{
                type, location
        };
    }

}
