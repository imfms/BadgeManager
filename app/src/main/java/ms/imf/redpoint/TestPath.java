package ms.imf.redpoint;

import ms.imf.redpoint.annotation.Path;
import ms.imf.redpoint.annotation.PathAptGlobalConfig;
import ms.imf.redpoint.annotation.Plugin;
import ms.imf.redpoint.annotation.SubNode;
import ms.imf.redpoint.annotation.SubNode2;
import ms.imf.redpoint.compiler.plugin.node.helper.hardcode.generator.NodeHelperHardcodeGeneratorCompilerPlugin;
import ms.imf.redpoint.compiler.plugin.nodeschema.export.json.NodeSchemaExportJsonCompilerPlugin;

/**
 * TestPath
 *
 * @author f_ms
 * @date 19-5-16
 */
@Path({
        @SubNode(type = "home"),
        @SubNode(type = "mine", args = {"uid"}, subNodes = {
                @SubNode2(type = "type")
        })
})
@PathAptGlobalConfig(
        eachAptRoundPlugins = @Plugin(NodeHelperHardcodeGeneratorCompilerPlugin.class),
        lastAptRoundPlugins = @Plugin(value = NodeSchemaExportJsonCompilerPlugin.class, args = "a.b.c/hi.json")
)
public class TestPath {
    public static void main(String[] args) {
        System.out.println(TestPath_Path.class);
    }
}
