package ms.imf.badge.demo;

import ms.imf.badge.annotation.Arg;
import ms.imf.badge.annotation.NodeContainer;
import ms.imf.badge.annotation.NodeParserGlobalConfig;
import ms.imf.badge.annotation.Plugin;
import ms.imf.badge.annotation.SubNode;
import ms.imf.badge.annotation.SubNode2;
import ms.imf.badge.compiler.plugin.nodecontainer.helpercode.NodeContainerHelperCodeGeneratorCompilePlugin;
import ms.imf.badge.compiler.plugin.nodetree.export.json.NodeTreeExportJsonCompilerPlugin;

/**
 * TestPath
 *
 * @author f_ms
 * @date 19-5-16
 */
@NodeContainer(
        value = {
                @SubNode(value = "home", subNodeContainerRef = TestPath2.class),
                @SubNode(value = "mine", args = @Arg("uid"), subNodes = {
                        @SubNode2(value = "name", args = @Arg(value = "arg1", valueLimits = {"a", "b", "c"}))
                })
        },
        nodeJson = {
                "{\"name\":\"nodeJsonType1\",\"args\":[{\"name\":\"nodeJsonType1Arg1\"},{\"name\":\"nodeJsonType1Arg2\",\"limits\":[\"limit1\", \"limit2\",\"limit3\"]}]}",
                "{\"name\":\"nodeJsonType2\",\"args\":[{\"name\":\"nodeJsonType2Arg1\"},{\"name\":\"nodeJsonType2Arg2\",\"limits\":[\"limit1\",\"limit2\",\"limit3\"]}]}"
        }
)
@NodeParserGlobalConfig(
        eachAptRoundNodeTreeParsedPlugins = @Plugin(NodeContainerHelperCodeGeneratorCompilePlugin.class),
        lastAptRoundNodeTreeParsedPlugins = @Plugin(value = NodeTreeExportJsonCompilerPlugin.class, args = "javaResource://a.b.c/hi.json")
)
public class TestPath {
    public static void main(String[] args) {
        System.out.println(TestPath_Node.class);
    }
}
