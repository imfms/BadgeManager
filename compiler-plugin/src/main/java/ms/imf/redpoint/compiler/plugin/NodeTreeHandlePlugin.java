package ms.imf.redpoint.compiler.plugin;

import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;

import ms.imf.redpoint.entity.NodeTree;

/**
 * 节点树处理插件
 *
 * @author f_ms
 */
public interface NodeTreeHandlePlugin {

    /**
     * 插件实例调用上下文
     */
    interface PluginContext {

        /**
         * 获取apt运行环境信息
         * @see javax.annotation.processing.Processor#init(ProcessingEnvironment)
         */
        ProcessingEnvironment processingEnvironment();

        /**
         * 获取调用参数
         */
        String[] args();

        /**
         * 获取扁平结构节点容器实体
         * 例如有节点容器树： a(a1,a2(a3)),b
         * 则返回结果中包含的所有实体: a(a1,a2(a3)),b; a1; a2(a3); a3; b;
         */
        List<NodeContainerEntity> flatNodeContainerEntities();

        /**
         * 获取树形结构节点容器实体
         * 例如有节点容器树： a(a1,a2(a3)),b
         * 则返回结果中只包含根节点容器实体： a(a1,a2(a3)); b
         * a1和a2(a3)不会出现在结果中
         */
        List<NodeContainerEntity> treeNodeContainerEntities();

        /**
         * 获取节点树
         * 节点树不包含节点容器相关信息，仅有节点信息
         */
        List<NodeTree> nodeTree();
    }

    /**
     * 当节点树被解析完毕
     *
     * @param pluginContext plugin context
     * @throws AptProcessException framework will show your error friendly
     */
    void onNodeTreeParsed(PluginContext pluginContext) throws AptProcessException;
    
}
