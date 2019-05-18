package ms.imf.redpoint.converter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

/**
 * path converter
 *
 * @author f_ms
 * @date 19-5-18
 */
public class PathConverter {

    private final List<ConvertRule> convertRules;

    /**
     * @param convertRulesJson  转换规则 格式为 toJson(List<{@link ConvertRule}>)
     * @throws IllegalArgumentException 格式校验未通过
     */
    public PathConverter(String convertRulesJson) throws IllegalArgumentException {
        this(convertRulesJson, null);
    }

    /**
     * @param convertRulesJson  转换规则 格式为 toJson(List<{@link ConvertRule}>)
     * @param targetPathsSchema 可选,目标Path的全貌,用于参与转换规则的校验以发现更多规则本身的错误,格式为 toJson(List<{@link NodeSchema}>)
     * @throws IllegalArgumentException 格式校验未通过
     */
    public PathConverter(String convertRulesJson, String targetPathsSchema) throws IllegalArgumentException {
        this(new Gson(), convertRulesJson, targetPathsSchema);
    }

    /**
     * @param convertRulesJson  转换规则 格式为 toJson(List<{@link ConvertRule}>)
     * @param targetPathsSchema 可选,目标Path的全貌,用于参与转换规则的校验以发现更多规则本身的错误,格式为 toJson(List<{@link NodeSchema}>)
     * @throws IllegalArgumentException 格式校验未通过
     */
    private PathConverter(Gson gson, String convertRulesJson, String targetPathsSchema) throws IllegalArgumentException {
        this(
                PathConverter.<List<ConvertRule>>parseJson(
                        gson,
                        convertRulesJson,
                        new TypeToken<List<ConvertRule>>(){}.getType(),
                        "found error on parse convertRulesJson"
                ),
                PathConverter.<List<NodeSchema>>parseJson(
                        gson,
                        targetPathsSchema,
                        new TypeToken<List<NodeSchema>>(){}.getType(),
                        "found error on parse targetPathsSchema"
                )
        );
    }

    /**
     * @param convertRules      转换规则 格式为 toJson(List<{@link ConvertRule}>)
     * @throws IllegalArgumentException 格式校验未通过
     */
    public PathConverter(List<ConvertRule> convertRules) throws IllegalArgumentException {
        this(convertRules, null);
    }

    /**
     * @param convertRules      转换规则 格式为 toJson(List<{@link ConvertRule}>)
     * @param targetPathsSchema 可选,目标Path的全貌,用于参与转换规则的校验以发现更多规则本身的错误,格式为 toJson(List<{@link NodeSchema}>)
     * @throws IllegalArgumentException 格式校验未通过
     */
    public PathConverter(List<ConvertRule> convertRules, List<NodeSchema> targetPathsSchema) throws IllegalArgumentException {
        ArgCheckUtil.checkArg(convertRules, targetPathsSchema);
        this.convertRules = convertRules;
    }

    /**
     * 转换路径
     * @param sourceNodes 源路径
     * @return 目标路径, null == 查无匹配
     */
    public List<Node> convert(List<Node> sourceNodes) {
        // TODO: 19-5-18
        return null;
    }

    private static <T> T parseJson(Gson gson, String json, Type type, String errorDescribe) throws IllegalArgumentException {
        if (json == null) {
            return null;
        }
        try {
            return gson.fromJson(json, type);
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("%s: %s", errorDescribe, e.getMessage()), e);
        }
    }
}
