package ms.imf.redpoint.compiler.plugin.nodeconverter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;

import ms.imf.redpoint.compiler.plugin.AptProcessException;
import ms.imf.redpoint.compiler.plugin.ParsedNodeSchemaHandlePlugin;
import ms.imf.redpoint.compiler.plugin.PathEntity;
import ms.imf.redpoint.converter.ArgCheckUtil;
import ms.imf.redpoint.entity.NodeSchema;

/**
 * node convert checker compiler plugin
 * @author f_ms
 */
public class NodeConverterCheckCompilerPlugin implements ParsedNodeSchemaHandlePlugin {

    @Override
    public void onParsed(ProcessingEnvironment processingEnvironment, String[] args, List<PathEntity> treePathEntities, List<NodeSchema> treeNodeSchemas) throws AptProcessException {

        if (args == null
                || args.length <= 0
                || args[0] == null
                || args[0].isEmpty()) {
            throw new AptProcessException("args can't be empty, I need convertCheckConfigFilePath, please add it into args[0]");
        }

        String convertCheckConfigFilePath = args[0];

        InputStream convertCheckFileInputStream;
        try {
            convertCheckFileInputStream = new FileInputStream(convertCheckConfigFilePath);
        } catch (FileNotFoundException e) {
            throw new AptProcessException(String.format("args[0]-convertCheckConfigFilePath(%s) not exist, please check file path", convertCheckConfigFilePath), e);
        }

        try {
            ArgCheckUtil.checkArg(convertCheckFileInputStream, treeNodeSchemas);
        } catch (IllegalArgumentException e) {
            throw new AptProcessException(String.format("found error on convert config check: %s", e.getMessage()), e);
        }
    }

}
