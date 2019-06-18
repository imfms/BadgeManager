package ms.imf.redpoint.compiler.plugin.nodeconverter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import ms.imf.redpoint.compiler.plugin.AptProcessException;
import ms.imf.redpoint.compiler.plugin.ParsedNodeSchemaHandlePlugin;
import ms.imf.redpoint.compiler.plugin.PluginContext;
import ms.imf.redpoint.converter.ArgCheckUtil;

/**
 * node convert checker compiler plugin
 * @author f_ms
 */
public class NodeConverterCheckCompilerPlugin implements ParsedNodeSchemaHandlePlugin {

    @Override
    public void onParsed(PluginContext context) throws AptProcessException {
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
            ArgCheckUtil.checkArg(convertCheckFileInputStream, context.treeNodeSchemas());
        } catch (IllegalArgumentException e) {
            throw new AptProcessException(String.format("found error on convert config check: %s", e.getMessage()), e);
        }
    }
}
