package ms.imf.badge.compiler.plugin.nodeconverter;

import java.io.InputStream;

import ms.imf.badge.compiler.plugin.AptProcessException;
import ms.imf.badge.compiler.plugin.NodeTreeHandlePlugin;
import ms.imf.badge.converter.ConvertRule;
import ms.imf.badge.converter.ConvertRuleChecker;
import ms.imf.badge.util.ResourceHandler;

/**
 * 节点转换规则检查插件，用于对编译期节点转换规则的检查提供支持。fail-fast使错误尽早显现
 *
 * <pre>
 * 所需参数：
 *   参数1：节点转换规则JSON资源类型及位置，JSON格式为toJson(List<{@link ConvertRule}>)，支持资源类型参见 {@link ResourceHandler}
 * </pre>
 *
 * @author f_ms
 */
public class NodeConvertRuleCheckCompilerPlugin implements NodeTreeHandlePlugin {

    @Override
    public void onNodeTreeParsed(PluginContext context) throws AptProcessException {
        if (context.args() == null
                || context.args().length <= 0
                || context.args()[0] == null
                || context.args()[0].isEmpty()) {
            throw new AptProcessException("args can't be empty, I need convertCheckConfigResourceStr, please add it into args[0]");
        }

        String resourceStr = context.args()[0];

        InputStream convertCheckFileInputStream;
        try {
            convertCheckFileInputStream = ResourceHandler.read(context.processingEnvironment(), resourceStr);
        } catch (Exception e) {
            throw new AptProcessException(
                    String.format(
                            "found error on read args[0]-convertCheckConfigResource %s: %s",
                            resourceStr,
                            e.getMessage()
                    ),
                    e
            );
        }

        try {
            ConvertRuleChecker.check(convertCheckFileInputStream, context.nodeTree());
        } catch (IllegalArgumentException e) {
            throw new AptProcessException(String.format("found error on convert config check: %s", e.getMessage()), e);
        }
    }
}
