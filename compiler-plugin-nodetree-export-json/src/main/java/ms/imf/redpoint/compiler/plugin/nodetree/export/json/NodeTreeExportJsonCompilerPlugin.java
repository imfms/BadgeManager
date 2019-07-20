package ms.imf.redpoint.compiler.plugin.nodetree.export.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Type;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.StandardLocation;

import ms.imf.redpoint.compiler.plugin.AptProcessException;
import ms.imf.redpoint.compiler.plugin.NodeTreeHandlePlugin;
import ms.imf.redpoint.entity.NodeTree;

/**
 * 以JSON格式导出节点树
 *
 * <pre>
 * 所需参数，被方括号[]包裹的内容为可选项
 *  参数1: Java风格资源导出目标：[packageName/]resourceName
 *
 * 导出JSON格式:
 *
 * [NodeTree {
 *   String name;
 *   List&lt;String&gt; args;
 *   List&lt;NodeTree&gt; sub;
 * }]
 * </pre>
 *
 * @author f_ms
 * @date 19-6-16
 */
public class NodeTreeExportJsonCompilerPlugin implements NodeTreeHandlePlugin {

    public static final String JSON_KEY_NAME = "name";
    public static final String JSON_KEY_ARGS = "args";
    public static final String JSON_KEY_SUB = "sub";

    @Override
    public void onNodeTreeParsed(PluginContext context) throws AptProcessException {
        if (context.args() == null
                || context.args().length <= 0
                || context.args()[0] == null
                || context.args()[0].isEmpty()) {
            throw new IllegalArgumentException("args can't be empty, I need java style resource location, please add it into args[0]");
        }

        final String resource = context.args()[0];

        final String resourcePackage;
        final String resourceName;
        int splitIndex = resource.indexOf('/');
        if (splitIndex < 0) {
            resourcePackage = "";
            resourceName = resource;
        } else {
            resourcePackage = resource.substring(0, splitIndex);
            resourceName = resource.substring(splitIndex + 1);
        }

        try (PrintStream os = exportOutputStream(context.processingEnvironment(), resourcePackage, resourceName)) {

            gson().toJson(context.nodeTree(), os);
            os.flush();

        } catch (Exception e) {
            throw new AptProcessException(String.format("found error on write nodeSchema to JavaStyle resource '%s': %s", resource, e.getMessage()), e);
        }
    }

    private PrintStream exportOutputStream(ProcessingEnvironment processingEnvironment, String resourcePackage, String resourceName) throws IOException {
        return new PrintStream(
                processingEnvironment.getFiler()
                        .createResource(StandardLocation.CLASS_OUTPUT, resourcePackage, resourceName)
                        .openOutputStream());
    }

    private Gson gson() {
        return new GsonBuilder()
                .registerTypeAdapter(NodeTree.class, new JsonSerializer<NodeTree>() {
                    @Override
                    public JsonElement serialize(NodeTree src, Type typeOfSrc, JsonSerializationContext context) {
                        JsonObject jsonObject = new JsonObject();

                        jsonObject.addProperty(JSON_KEY_NAME, src.name);
                        jsonObject.add(JSON_KEY_ARGS, context.serialize(src.args));
                        jsonObject.add(JSON_KEY_SUB, context.serialize(src.sub));

                        return jsonObject;
                    }
                })
                .create();
    }
}
