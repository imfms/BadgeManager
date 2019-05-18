package ms.imf.redpoint.converter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * path converter
 *
 * @author f_ms
 * @date 19-5-18
 */
public class PathConverter {

    private final List<ConvertRule> convertRules;

    /**
     * @param convertRulesJson 转换规则 格式为 toJson(List<{@link ConvertRule}>)
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
                        new TypeToken<List<ConvertRule>>() {
                        }.getType(),
                        "found error on parse convertRulesJson"
                ),
                PathConverter.<List<NodeSchema>>parseJson(
                        gson,
                        targetPathsSchema,
                        new TypeToken<List<NodeSchema>>() {
                        }.getType(),
                        "found error on parse targetPathsSchema"
                )
        );
    }

    /**
     * @param convertRules 转换规则 格式为 toJson(List<{@link ConvertRule}>)
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
     *
     * @param sourceNodes 源路径
     * @return 目标路径, null == 查无匹配
     */
    public List<Node> convert(List<Node> sourceNodes) {

        if (sourceNodes == null) {
            return null;
        }

        if (sourceNodes.isEmpty()) {
            return Collections.emptyList();
        }

        final List<ConvertRule> convertRules = matchConvertRule(sourceNodes);
        if (convertRules == null) {
            return null;
        }

        ConvertRule lastConvertRule = convertRules.get(convertRules.size() - 1);

        if (lastConvertRule.convertTo == null
                || lastConvertRule.convertTo.isEmpty()) {
            return null;
        }

        final List<Node> result = new LinkedList<>();
        for (ConvertRule.ConvertTo convertTo : lastConvertRule.convertTo) {

            final Map<String, String> args = new HashMap<>();

            if (convertTo.args != null) {
                for (ConvertRule.Arg arg : convertTo.args) {
                    Node targetLevelNode = sourceNodes.get(arg.myLevel);
                    if (targetLevelNode.args != null) {
                        args.put(
                                arg.hisArg,
                                targetLevelNode.args.get(arg.myArg)
                        );
                    }
                }
            }

            result.add(new Node(convertTo.type, args));
        }

        return result;
    }

    private List<ConvertRule> matchConvertRule(List<Node> nodes) {
        final List<ConvertRule> result = new ArrayList<>();

        List<ConvertRule> currentLevelRules = this.convertRules;

        for (Node node : nodes) {

            ConvertRule matchedConvertRule = null;

            if (currentLevelRules != null) {
                for (ConvertRule convertRule : currentLevelRules) {
                    if (convertRule.type.equals(node.type)) {
                        matchedConvertRule = convertRule;
                        break;
                    }
                }
            }

            if (matchedConvertRule == null) {
                return null;
            }

            result.add(matchedConvertRule);
            currentLevelRules = matchedConvertRule.sub;
        }

        return result;
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
