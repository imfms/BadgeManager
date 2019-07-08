package ms.imf.redpoint.compiler.plugin.filemover;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.StandardLocation;

import ms.imf.redpoint.compiler.plugin.AptProcessException;
import ms.imf.redpoint.compiler.plugin.ParsedNodeSchemaHandlePlugin;
import ms.imf.redpoint.compiler.plugin.PluginContext;

/**
 * 文件移动器，支持复制指定资源到指定位置。
 * <p>
 * 主要用于编译期将外部资源移入到java-resource，例如编译期将外部nodeConverterMappingFile复制到java-resource
 * <p>
 * <p>
 * 所需参数
 * <p>
 * 参数1: 源文件位置
 * <p>
 * 参数2: 文件移动后目标位置
 * <p>
 * <p>
 * 文件位置支持以下类型, 方括号[]包裹的内容为可选项:
 * <p>
 * <p>
 * file://[/]filePath: 常规文件系统绝对、相对位置
 * <p>
 * &nbsp;&nbsp;例如: file:///home/user/resource.txt, /home/user/resource.txt, resource.txt
 * <p>
 * javaResource://[packageName/]resourceName: java-style资源位置
 * <p>
 * &nbsp;&nbsp;例如: javaResource://com.foo/resource.txt, javaResource://resource.txt
 *
 * @author f_ms
 * @date 19-7-8
 */
public class FileMoverCompilerPlugin implements ParsedNodeSchemaHandlePlugin {

    public static final int ARG_INDEX_SOURCE_LOCATION = 0;
    public static final int ARG_INDEX_TARGET_LOCATION = 1;
    public static final String RESOURCE_TYPE_SCHEMA_SYMBOL = "://";
    public static final int COPY_BUFFER_SIZE = 4096;

    @Override
    public void onParsed(PluginContext context) throws AptProcessException {

        InputStream sourceLocation = getInputStreamArg(
                context.processingEnvironment(), context.args(), ARG_INDEX_SOURCE_LOCATION, "source resource location"
        );
        OutputStream targetLocation = getOutputStreamArg(
                context.processingEnvironment(), context.args(), ARG_INDEX_TARGET_LOCATION, "target resource location"
        );

        try {
            byte[] buffer = new byte[COPY_BUFFER_SIZE];
            int readSize;
            while ((readSize = sourceLocation.read(buffer)) != -1) {
                targetLocation.write(buffer, 0, readSize);
            }
        } catch (IOException e) {
            throw new AptProcessException(
                    String.format(
                            "found error on copy '%s' to '%s': %s",
                            context.args()[ARG_INDEX_SOURCE_LOCATION],
                            context.args()[ARG_INDEX_TARGET_LOCATION],
                            e.getMessage()
                    ),
                    e
            );
        }
    }

    enum ResourceTypeHandler {

        FILE("file") {
            @Override
            InputStream read(ProcessingEnvironment processingEnvironment, String type, String location) throws Exception {
                return new FileInputStream(location);
            }

            @Override
            OutputStream write(ProcessingEnvironment processingEnvironment, String type, String location) throws Exception {
                return new FileOutputStream(location);
            }
        },
        JAVA_RESOURCE("javaResource") {
            @Override
            InputStream read(ProcessingEnvironment processingEnvironment, String type, String location) throws Exception {
                /*
                a.b.c/d.txt -> a/b/c/d.txt
                 */
                String rawLocation = location.replace('.', '/');
                if (!rawLocation.startsWith("/")) {
                    rawLocation = '/' + rawLocation;
                }
                return FileMoverCompilerPlugin.class.getClassLoader().getResourceAsStream(rawLocation);
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

        ResourceTypeHandler(String type) {
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

    private static OutputStream getOutputStreamArg(ProcessingEnvironment processingEnvironment, String[] args, int index, String argDesc) throws AptProcessException {
        String[] locationArg = getLocationArg(args, index, argDesc);

        String type = locationArg[0];
        String location = locationArg[1];

        ResourceTypeHandler resourceTypeHandler = getResourceTypeHandler(type, location);
        try {
            return resourceTypeHandler.write(processingEnvironment, type, location);
        } catch (Exception e) {
            throw new AptProcessException(
                    String.format(
                            "found error on write resource %s://%s: %s",
                            type, location, e.getMessage()
                    ),
                    e
            );
        }
    }

    private static InputStream getInputStreamArg(ProcessingEnvironment processingEnvironment, String[] args, int index, String argDesc) throws AptProcessException {
        String[] locationArg = getLocationArg(args, index, argDesc);

        String type = locationArg[0];
        String location = locationArg[1];

        ResourceTypeHandler resourceTypeHandler = getResourceTypeHandler(type, location);
        try {
            InputStream read = resourceTypeHandler.read(processingEnvironment, type, location);
            if (read == null) {
                throw new RuntimeException(String.format("can't find resource: %s://%s", type, location));
            }
            return read;
        } catch (Exception e) {
            throw new AptProcessException(
                    String.format(
                            "found error on read resource %s://%s: %s",
                            type, location, e.getMessage()
                    ),
                    e
            );
        }
    }

    private static ResourceTypeHandler getResourceTypeHandler(String type, String location) throws AptProcessException {
        for (ResourceTypeHandler resourceTypeHandler : ResourceTypeHandler.values()) {
            if (resourceTypeHandler.accept(type, location)) {
                return resourceTypeHandler;
            }
        }

        throw new AptProcessException(String.format(
                "unaccept resource type '%s', I only accept resourceType: %s",
                type,
                Arrays.toString(ResourceTypeHandler.acceptTypes())
        ));
    }

    private static String[] getLocationArg(String[] args, int index, String argDesc) throws AptProcessException {
        String arg = getArg(args, index, argDesc);

        int schemaSymbolIndex = arg.indexOf(RESOURCE_TYPE_SCHEMA_SYMBOL);
        if (schemaSymbolIndex < 0) {
            throw new AptProcessException(String.format(
                    "can't find resource type schema symbol '%s' in arg[%d](%s)",
                    RESOURCE_TYPE_SCHEMA_SYMBOL, index, argDesc
            ));
        }

        String type = arg.substring(0, schemaSymbolIndex);
        String location = arg.substring(schemaSymbolIndex + RESOURCE_TYPE_SCHEMA_SYMBOL.length(), arg.length());

        return new String[]{
                type, location
        };
    }


    private static String getArg(String[] args, int index, String argDesc) throws AptProcessException {
        if (args == null
                || args.length <= index
                || args[index] == null
                || args[index].isEmpty()) {
            throw new AptProcessException(String.format(
                    "args[%d](%s) can't be empty, please add it into args[%d]",
                    index, argDesc, index
            ));
        }
        return args[index];
    }
}
