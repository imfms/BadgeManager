package ms.imf.redpoint.compiler.plugin.nodetree.export.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.io.PrintStream;
import java.lang.reflect.Type;

import ms.imf.redpoint.compiler.plugin.AptProcessException;
import ms.imf.redpoint.compiler.plugin.NodeTreeHandlePlugin;
import ms.imf.redpoint.entity.NodeTree;
import ms.imf.redpoint.util.ResourceHandler;

/**
 * 以JSON格式导出节点树
 *
 * <p>
 * <p>
 * 所需参数：
 * <pre>
 *  参数1: 导出资源类型及目标，支持类型参见 {@link ResourceHandler}
 * </pre>
 *
 * JSON格式:
 * <pre>
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
            throw new IllegalArgumentException("args can't be empty, I need resource type and location, please add it into args[0]");
        }

        final String resourceStr = context.args()[0];

        try {

            PrintStream target = new PrintStream(
                    ResourceHandler.write(context.processingEnvironment(), resourceStr)
            );

            gson().toJson(context.nodeTree(), target);
            target.flush();

        } catch (Exception e) {
            throw new AptProcessException(String.format("found error on write nodeTree to %s: %s", resourceStr, e.getMessage()), e);
        }
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
