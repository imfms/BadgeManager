package ms.imf.redpoint.compiler.plugin;

import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;

import ms.imf.redpoint.entity.NodeTree;

public interface NodeTreeParsedHandlerPlugin {

    interface PluginContext {
        ProcessingEnvironment processingEnvironment();
        String[] args();
        List<NodeContainerAnnotationEntity> flatNodeContainerEntities();
        List<NodeContainerAnnotationEntity> treeNodeContainerEntities();
        List<NodeTree> nodeTree();
    }

    /**
     * @param pluginContext plugin context
     * @throws AptProcessException framework will show your error to user friendly
     */
    void onNodeTreeParsed(PluginContext pluginContext) throws AptProcessException;
    
}
