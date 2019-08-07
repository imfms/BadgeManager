package ms.imf.badge.compiler.plugin.nodecontainer.helpercode;

import ms.imf.badge.compiler.plugin.AptProcessException;
import ms.imf.badge.compiler.plugin.NodeTreeHandlePlugin;

/**
 * 节点辅助引用代码生成器
 * <p>
 * 辅助代码格式:
 * <pre>
 * package ${CLASS_PACKAGE_NAME}
 *
 * interface ${CLASS}_Path {
 *
 *   interface ${subNodeType} {
 *
 *     String name$;
 *     String arg${arg...};
 *     String arg${arg...}${argValueLimit...};
 *     ...
 *
 *     interface ${subNodeType} {
 *       ...
 *     }
 *
 *     ...
 *
 *   }
 *
 *   ...
 *
 * }
 * </pre>
 * @author f_ms
 * @date 2019/6/18
 */
public class NodeContainerHelperCodeGeneratorCompilePlugin implements NodeTreeHandlePlugin {
    @Override
    public void onNodeTreeParsed(PluginContext context) throws AptProcessException {
        new Generator(context.processingEnvironment().getFiler())
                .generate(context.flatNodeContainerEntities());
    }
}
