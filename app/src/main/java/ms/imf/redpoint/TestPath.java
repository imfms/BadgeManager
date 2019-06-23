package ms.imf.redpoint;

import ms.imf.redpoint.annotation.NodeArg;
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
@Path(value = {
        @SubNode(type = "home"),
        @SubNode(type = "mine", args = @NodeArg("uid"), subNodes = {
                @SubNode2(type = "type", args = @NodeArg(value = "arg1", valueLimits = {"a", "b", "c"}))
        })
},
        nodesJson = {
                "{\"type\":\"nodeJsonType1\",\"args\":[{\"name\":\"nodeJsonType1Arg1\"},{\"name\":\"nodeJsonType1Arg2\",\"limits\":[\"limit1\", \"limit2\",\"limit3\"]}]}",
                "{\"type\":\"nodeJsonType2\",\"args\":[{\"name\":\"nodeJsonType2Arg1\"},{\"name\":\"nodeJsonType2Arg2\",\"limits\":[\"limit1\",\"limit2\",\"limit3\"]}]}"
        }
)
@PathAptGlobalConfig(
        eachAptRoundPlugins = @Plugin(NodeHelperHardcodeGeneratorCompilerPlugin.class),
        lastAptRoundPlugins = @Plugin(value = NodeSchemaExportJsonCompilerPlugin.class, args = "a.b.c/hi.json")
)
public class TestPath {
    public static void main(String[] args) {
        System.out.println(TestPath_Path.class);
    }
}
