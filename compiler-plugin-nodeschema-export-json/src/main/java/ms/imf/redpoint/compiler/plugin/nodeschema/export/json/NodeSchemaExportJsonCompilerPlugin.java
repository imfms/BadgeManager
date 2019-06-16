package ms.imf.redpoint.compiler.plugin.nodeschema.export.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.StandardLocation;

import ms.imf.redpoint.compiler.plugin.ParsedNodeSchemaHandlePlugin;
import ms.imf.redpoint.entity.NodeSchema;

/**
 * java style resource mode export node schema to json
 * - arg[0]: target java style resource location: packageName/resourceName
 * <p>
 * output json format:
 * <p>
 * [NodeSchema {
 * <p>
 * &nbsp;&nbsp;String type;
 * <p>
 * &nbsp;&nbsp;List&lt;String&gt; args;
 * <p>
 * &nbsp;&nbsp;List&lt;NodeSchema&gt; sub;
 * <p>
 * }]
 *
 * @author f_ms
 * @date 19-6-16
 */
public class NodeSchemaExportJsonCompilerPlugin implements ParsedNodeSchemaHandlePlugin {

    public static final String JSON_KEY_TYPE = "type";
    public static final String JSON_KEY_ARGS = "args";
    public static final String JSON_KEY_SUB = "sub";

    @Override
    public void onParsed(ProcessingEnvironment processingEnvironment, String[] args, List<NodeSchema> nodeSchemas) throws Exception {
        if (args == null
                || args.length <= 0
                || args[0] == null
                || args[0].isEmpty()) {
            throw new IllegalArgumentException("args can't be empty, I need java style resource location, please add it into args[0]");
        }

        final String resource = args[0];

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

        try (OutputStream os = processingEnvironment.getFiler()
                .createResource(StandardLocation.CLASS_OUTPUT, resourcePackage, resourceName)
                .openOutputStream()) {

            new GsonBuilder()
                    .registerTypeAdapter(NodeSchema.class, new JsonSerializer<NodeSchema>() {
                        @Override
                        public JsonElement serialize(NodeSchema src, Type typeOfSrc, JsonSerializationContext context) {
                            JsonObject jsonObject = new JsonObject();

                            jsonObject.addProperty(JSON_KEY_TYPE, src.type);
                            jsonObject.add(JSON_KEY_ARGS, context.serialize(src.args));
                            jsonObject.add(JSON_KEY_SUB, context.serialize(src.sub));

                            return jsonObject;
                        }
                    })
                    .create();

            os.write(new Gson().toJson(nodeSchemas).getBytes());
            os.flush();

        } catch (Exception e) {
            throw new IllegalStateException(String.format("found error on write nodeSchema to JavaStyle resource '%s': %s", resource, e.getMessage()), e);
        }
    }
}
