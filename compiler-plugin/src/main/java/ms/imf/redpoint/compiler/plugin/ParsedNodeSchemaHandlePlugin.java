package ms.imf.redpoint.compiler.plugin;

import java.util.List;

import ms.imf.redpoint.entity.NodeSchema;

public interface ParsedNodeSchemaHandlePlugin {
    void onParsed(List<NodeSchema> nodeSchemas) throws Exception;
}
