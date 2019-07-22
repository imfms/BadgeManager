package ms.imf.redpoint.compiler.plugin.nodeconverter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import ms.imf.redpoint.compiler.plugin.AptProcessException;
import ms.imf.redpoint.compiler.plugin.NodeTreeHandlePlugin;
import ms.imf.redpoint.converter.ConvertRule;
import ms.imf.redpoint.converter.ConvertRuleChecker;

/**
 * 节点转换规则检查插件，用于对编译期节点转换规则的检查提供支持。fail-fast使错误尽早显现
 *
 * <pre>
 * 所需参数：
 *   参数1：节点转换规则JSON文件路径，JSON格式为toJson(List<{@link ConvertRule}>)
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
            throw new AptProcessException("args can't be empty, I need convertCheckConfigFilePath, please add it into args[0]");
        }

        String convertCheckConfigFilePath = context.args()[0];

        InputStream convertCheckFileInputStream;
        try {
            convertCheckFileInputStream = new FileInputStream(convertCheckConfigFilePath);
        } catch (FileNotFoundException e) {
            throw new AptProcessException(String.format("args[0]-convertCheckConfigFilePath(%s) not exist, please check file path", convertCheckConfigFilePath), e);
        }

        try {
            ConvertRuleChecker.check(convertCheckFileInputStream, context.nodeTree());
        } catch (IllegalArgumentException e) {
            throw new AptProcessException(String.format("found error on convert config check: %s", e.getMessage()), e);
        }
    }
}
