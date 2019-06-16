package ms.imf.redpoint.compiler.plugin;

import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;

import ms.imf.redpoint.entity.NodeSchema;

public interface ParsedNodeSchemaHandlePlugin {

    /**
     * @param processingEnvironment annotation processingEnvironment
     * @param args        arguments
     * @param nodeSchemas nodeSchemas
     * @throws Exception framework will show your error to user friendly
     */
    void onParsed(ProcessingEnvironment processingEnvironment, String[] args, List<NodeSchema> nodeSchemas) throws Exception;
}
