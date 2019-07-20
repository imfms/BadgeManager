package ms.imf.redpoint.compiler.plugin.nodecontainer.helpercode;

import ms.imf.redpoint.compiler.plugin.AptProcessException;
import ms.imf.redpoint.compiler.plugin.NodeTreeHandlePlugin;

/*
package ${CLASS_PACKAGE_NAME}

interface ${CLASS}_Path {

  interface ${subNodeType} {

    String name$;
    String arg${arg1};
    String arg${arg...};

    interface ${subNodeType} {
        ...
    }

  }

  ...

}
 */

/**
 * node helper hardcode generator plugin
 * <p>
 * format:
 * <p>
 *
 * package&nbsp;${CLASS_PACKAGE_NAME}
 * <p>
 * <p>interface&nbsp;${CLASS}_Path&nbsp;{
 * <p>
 * <p>&nbsp;&nbsp;interface&nbsp;${subNodeType}&nbsp;{
 * <p>
 * <p>&nbsp;&nbsp;&nbsp;&nbsp;String&nbsp;name$;
 * <p>&nbsp;&nbsp;&nbsp;&nbsp;String&nbsp;arg${arg...};
 * <p>&nbsp;&nbsp;&nbsp;&nbsp;String&nbsp;arg${arg...}${argValueLimit...};
 * <p>
 * <p>&nbsp;&nbsp;&nbsp;&nbsp;interface&nbsp;${subNodeType}&nbsp;{
 * <p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;...
 * <p>&nbsp;&nbsp;&nbsp;&nbsp;}
 * <p>
 * <p>&nbsp;&nbsp;}
 * <p>
 * <p>&nbsp;&nbsp;...
 * <p>
 * <p>}
 *
 * @author f_ms
 * @date 2019/6/18
 */
public class NodeContainerHelperCodeGeneratorPlugin implements NodeTreeHandlePlugin {

    @Override
    public void onNodeTreeParsed(PluginContext context) throws AptProcessException {
        new Generator(context.processingEnvironment().getFiler())
                .generate(context.flatNodeContainerEntities());
    }
}
