package ms.imf.redpoint.compiler.plugin;

import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;

import ms.imf.redpoint.entity.NodeSchema;

public interface PluginContext {

    ProcessingEnvironment processingEnvironment();
    String[] args();
    List<PathEntity> allPathEntities();
    List<PathEntity> treePathEntities();
    List<NodeSchema> treeNodeSchemas();

}
