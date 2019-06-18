package ms.imf.redpoint.compiler.plugin;

import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;

import ms.imf.redpoint.entity.NodeSchema;

public interface ParsedNodeSchemaHandlePlugin {

    /**
     * @param processingEnvironment annotation processingEnvironment
     * @param args                  arguments
     * @param treePathEntities          path entities tree
     * @param treeNodeSchemas           node schema tree
     * @throws AptProcessException framework will show your error to user friendly
     */
    void onParsed(ProcessingEnvironment processingEnvironment, String[] args, List<PathEntity> treePathEntities, List<NodeSchema> treeNodeSchemas) throws AptProcessException;
}
