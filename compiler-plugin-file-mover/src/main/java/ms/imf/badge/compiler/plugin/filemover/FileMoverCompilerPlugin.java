package ms.imf.badge.compiler.plugin.filemover;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.processing.ProcessingEnvironment;

import ms.imf.badge.compiler.plugin.AptProcessException;
import ms.imf.badge.compiler.plugin.NodeTreeHandlePlugin;
import ms.imf.badge.util.IOUtil;
import ms.imf.badge.util.ResourceHandler;

/**
 * 文件移动器，支持复制指定资源到指定位置。
 * <p>
 * 主要用于编译期将外部资源移入到java-resource，例如编译期将外部nodeConverterMappingFile复制到java-resource
 * <p>
 * <p>
 * 所需参数：
 * <pre>
 * 参数1: 源资源类型及位置，支持类型参见 {@link ResourceHandler}
 * 参数2: 文件移动后目标位置，支持类型参见 {@link ResourceHandler}
 * </pre>
 *
 * @author f_ms
 * @date 19-7-8
 */
public class FileMoverCompilerPlugin implements NodeTreeHandlePlugin {

    public static final int ARG_INDEX_SOURCE_LOCATION = 0;
    public static final int ARG_INDEX_TARGET_LOCATION = 1;

    @Override
    public void onNodeTreeParsed(NodeTreeHandlePlugin.PluginContext context) throws AptProcessException {

        InputStream sourceInputStream = getInputStreamArg(
                context.processingEnvironment(), context.args(), ARG_INDEX_SOURCE_LOCATION, "source resource location"
        );
        OutputStream targetOutputStream = getOutputStreamArg(
                context.processingEnvironment(), context.args(), ARG_INDEX_TARGET_LOCATION, "target resource location"
        );

        try {
            IOUtil.copy(sourceInputStream, targetOutputStream);
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

    private static InputStream getInputStreamArg(ProcessingEnvironment processingEnvironment, String[] args, int index, String argDesc) throws AptProcessException {

        String resourceStr = getArg(args, index, argDesc);

        try {
            return ResourceHandler.read(processingEnvironment, resourceStr);
        } catch (Exception e) {
            throw new AptProcessException(
                    String.format(
                            "found error on read resource %s: %s",
                            resourceStr, e.getMessage()
                    ),
                    e
            );
        }
    }

    private static OutputStream getOutputStreamArg(ProcessingEnvironment processingEnvironment, String[] args, int index, String argDesc) throws AptProcessException {
        String resourceStr = getArg(args, index, argDesc);

        try {
            return ResourceHandler.write(processingEnvironment, resourceStr);
        } catch (Exception e) {
            throw new AptProcessException(
                    String.format(
                            "found error on read resource %s: %s",
                            resourceStr, e.getMessage()
                    ),
                    e
            );
        }
    }

}
