package ms.imf.redpoint.compiler.plugin.node.helper.hardcode.generator;

import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;

import ms.imf.redpoint.compiler.plugin.AptProcessException;
import ms.imf.redpoint.compiler.plugin.ParsedNodeSchemaHandlePlugin;
import ms.imf.redpoint.compiler.plugin.PathEntity;
import ms.imf.redpoint.entity.NodeSchema;

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
 * <p>&nbsp;&nbsp;&nbsp;&nbsp;String&nbsp;arg${arg1};
 * <p>&nbsp;&nbsp;&nbsp;&nbsp;String&nbsp;arg${arg...};
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
public class NodeHelperHardcodeGeneratorCompilerPlugin implements ParsedNodeSchemaHandlePlugin {

    @Override
    public void onParsed(ProcessingEnvironment processingEnvironment, String[] args, List<PathEntity> treePathEntities, List<NodeSchema> treeNodeSchemas) throws AptProcessException {
        new Generator(processingEnvironment.getFiler())
                .generate(treePathEntities);
    }

}
