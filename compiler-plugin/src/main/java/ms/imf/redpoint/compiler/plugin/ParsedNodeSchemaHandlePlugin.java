package ms.imf.redpoint.compiler.plugin;

import java.util.List;

import ms.imf.redpoint.entity.NodeSchema;

public interface ParsedNodeSchemaHandlePlugin {

    /**
     * @param args        arguments
     * @param nodeSchemas nodeSchemas
     * @throws Exception framework will show your error to user friendly
     */
    void onParsed(String[] args, List<NodeSchema> nodeSchemas) throws Exception;
}
