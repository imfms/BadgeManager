package ms.imf.redpoint.converter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ms.imf.redpoint.entity.Node;
import ms.imf.redpoint.entity.NodeSchema;

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
        this(convertRulesJson, true);
    }

    /**
     * @param convertRulesJson 转换规则 格式为 toJson(List<{@link ConvertRule}>)
     * @param isCheckRules     是否校验convertRules, 用于编译期已经校验，运行时无需二次校验的情况
     * @throws IllegalArgumentException 格式校验未通过
     */
    public PathConverter(String convertRulesJson, boolean isCheckRules) throws IllegalArgumentException {
        this(convertRulesJson, null, isCheckRules);
    }


    /**
     * @param convertRulesJson      转换规则 格式为 toJson(List<{@link ConvertRule}>)
     * @param targetPathsSchemaJson 可选,目标Path的全貌,用于参与转换规则的校验以发现更多规则本身的错误,格式为 toJson(List<{@link NodeSchema}>)
     * @throws IllegalArgumentException 格式校验未通过
     */
    public PathConverter(String convertRulesJson, String targetPathsSchemaJson) throws IllegalArgumentException {
        this(convertRulesJson, targetPathsSchemaJson, true);
    }

    /**
     * @param convertRulesJson      转换规则 格式为 toJson(List<{@link ConvertRule}>)
     * @param targetPathsSchemaJson 可选,目标Path的全貌,用于参与转换规则的校验以发现更多规则本身的错误,格式为 toJson(List<{@link NodeSchema}>)
     * @param isCheckRules          是否校验convertRules, 用于编译期已经校验，运行时无需二次校验的情况
     * @throws IllegalArgumentException 格式校验未通过
     */
    public PathConverter(String convertRulesJson, String targetPathsSchemaJson, boolean isCheckRules) throws IllegalArgumentException {
        this(new Gson(), convertRulesJson, targetPathsSchemaJson, isCheckRules);
    }

    /**
     * @param convertRulesJsonInputStream      转换规则 格式为 toJson(List<{@link ConvertRule}>)
     * @param targetPathsSchemaJsonInputStream 可选,目标Path的全貌,用于参与转换规则的校验以发现更多规则本身的错误,格式为 toJson(List<{@link NodeSchema}>)
     * @throws IllegalArgumentException 格式校验未通过
     */
    public PathConverter(InputStream convertRulesJsonInputStream, InputStream targetPathsSchemaJsonInputStream) throws IllegalArgumentException {
        this(convertRulesJsonInputStream, targetPathsSchemaJsonInputStream, true);
    }

    /**
     * @param convertRulesJsonInputStream      转换规则 格式为 toJson(List<{@link ConvertRule}>)
     * @param targetPathsSchemaJsonInputStream 可选,目标Path的全貌,用于参与转换规则的校验以发现更多规则本身的错误,格式为 toJson(List<{@link NodeSchema}>)
     * @param isCheckRules                     是否校验convertRules, 用于编译期已经校验，运行时无需二次校验的情况
     * @throws IllegalArgumentException 格式校验未通过
     */
    public PathConverter(InputStream convertRulesJsonInputStream, InputStream targetPathsSchemaJsonInputStream, boolean isCheckRules) throws IllegalArgumentException {
        this(new Gson(), convertRulesJsonInputStream, targetPathsSchemaJsonInputStream, isCheckRules);
    }

    private PathConverter(Gson gson, InputStream convertRulesJsonInputStream, InputStream targetPathsSchemaJsonInputStream, boolean isCheckRules) throws IllegalArgumentException {
        this(
                ArgCheckUtil.<List<ConvertRule>>parseJson(
                        gson,
                        convertRulesJsonInputStream,
                        new TypeToken<List<ConvertRule>>() {
                        }.getType(),
                        "found error on parse convertRulesJsonInputStream"
                ),
                ArgCheckUtil.<List<NodeSchema>>parseJson(
                        gson,
                        targetPathsSchemaJsonInputStream,
                        new TypeToken<List<NodeSchema>>() {
                        }.getType(),
                        "found error on parse targetPathsSchemaJsonInputStream"
                ),
                isCheckRules
        );
    }

    private PathConverter(Gson gson, String convertRulesJson, String targetPathsSchemaJson, boolean isCheckRules) throws IllegalArgumentException {
        this(
                ArgCheckUtil.<List<ConvertRule>>parseJson(
                        gson,
                        convertRulesJson,
                        new TypeToken<List<ConvertRule>>() {
                        }.getType(),
                        "found error on parse convertRulesJson"
                ),
                ArgCheckUtil.<List<NodeSchema>>parseJson(
                        gson,
                        targetPathsSchemaJson,
                        new TypeToken<List<NodeSchema>>() {
                        }.getType(),
                        "found error on parse targetPathsSchemaJson"
                ),
                isCheckRules
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
     * @param convertRules 转换规则 格式为 toJson(List<{@link ConvertRule}>)
     * @param isCheckRules 是否校验convertRules, 用于编译期已经校验，运行时无需二次校验的情况
     * @throws IllegalArgumentException 格式校验未通过
     */
    public PathConverter(List<ConvertRule> convertRules, boolean isCheckRules) throws IllegalArgumentException {
        this(convertRules, null, isCheckRules);
    }

    /**
     * @param convertRules      转换规则 格式为 toJson(List<{@link ConvertRule}>)
     * @param targetPathsSchema 可选,目标Path的全貌,用于参与转换规则的校验以发现更多规则本身的错误,格式为 toJson(List<{@link NodeSchema}>)
     * @throws IllegalArgumentException 格式校验未通过
     */
    public PathConverter(List<ConvertRule> convertRules, List<NodeSchema> targetPathsSchema) throws IllegalArgumentException {
        this(convertRules, targetPathsSchema, true);
    }

    /**
     * @param convertRules      转换规则 格式为 toJson(List<{@link ConvertRule}>)
     * @param targetPathsSchema 可选,目标Path的全貌,用于参与转换规则的校验以发现更多规则本身的错误,格式为 toJson(List<{@link NodeSchema}>)
     * @param isCheckRules      是否校验convertRules, 用于编译期已经校验，运行时无需二次校验的情况
     * @throws IllegalArgumentException 格式校验未通过
     */
    public PathConverter(List<ConvertRule> convertRules, List<NodeSchema> targetPathsSchema, boolean isCheckRules) throws IllegalArgumentException {
        if (convertRules == null) {
            throw new IllegalArgumentException("convertRules can't be null");
        }
        if (isCheckRules) {
            ArgCheckUtil.checkArg(convertRules, targetPathsSchema);
        }
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

                    if (arg.value != null) {
                        args.put(arg.hisArg, arg.value);
                        continue;
                    }

                    if (arg.refValue != null) {
                        Node targetLevelNode = sourceNodes.get(arg.refValue.myLevel);
                        if (targetLevelNode.args != null) {
                            args.put(
                                    arg.hisArg,
                                    targetLevelNode.args.get(arg.refValue.myArg)
                            );
                        }
                        continue;
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
}
