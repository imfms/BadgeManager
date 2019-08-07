package ms.imf.badge.converter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ms.imf.badge.entity.Node;
import ms.imf.badge.entity.NodeTree;

/**
 * 节点转换器，根据转换规则将一条节点路径转换为另一条节点路径
 * <p>
 * 在多前端的APP中，不同前端由于其平台环境特殊性，所以对业务的视图实现不尽相同
 * 所以前端拿到的节点信息很可能跟实际代码有所出入，需要将拿到的节点转换为代码上的实际节点
 * 但纯手工硬编码转换过程难以跟实际节点对应、易出错、错误难以发现、难以维护……
 * <p>
 * 遂制定出了一套相对覆盖面广泛的节点转换规则 List<{@link ConvertRule}>> ，此转换器负责对转换规则的合法性校验及实际转换任务的执行
 *
 * @author f_ms
 * @date 19-5-18
 * @see ConvertRule
 */
public class NodePathConverter {

    private final List<ConvertRule> convertRules;

    /**
     * @param convertRulesJson 转换规则 格式为 toJson(List<{@link ConvertRule}>)
     * @throws IllegalArgumentException 格式校验未通过
     */
    public NodePathConverter(String convertRulesJson) throws IllegalArgumentException {
        this(convertRulesJson, true);
    }

    /**
     * @param convertRulesJson 转换规则 格式为 toJson(List<{@link ConvertRule}>)
     * @param isCheckRules     是否校验转换规则合法性, 不校验情况提供给用于编译期已经校验，运行时无需二次校验的情况，实际校验任务由{@link ConvertRuleChecker}接管
     * @throws IllegalArgumentException 格式校验未通过
     */
    public NodePathConverter(String convertRulesJson, boolean isCheckRules) throws IllegalArgumentException {
        this(convertRulesJson, null, isCheckRules);
    }

    /**
     * @param convertRulesJson         转换规则 格式为 toJson(List<{@link ConvertRule}>)
     * @param targetNodeTreeSchemaJson 可选,转换目标节点树的全貌，用于参与转换规则的校验以发现更多规则本身的错误，格式为 toJson(List<{@link NodeTree}>)
     * @throws IllegalArgumentException 格式校验未通过
     */
    public NodePathConverter(String convertRulesJson, String targetNodeTreeSchemaJson) throws IllegalArgumentException {
        this(convertRulesJson, targetNodeTreeSchemaJson, true);
    }

    /**
     * @param convertRulesJson         转换规则 格式为 toJson(List<{@link ConvertRule}>)
     * @param targetNodeTreeSchemaJson 可选,转换目标节点树的全貌，用于参与转换规则的校验以发现更多规则本身的错误，格式为 toJson(List<{@link NodeTree}>)
     * @param isCheckRules             是否校验转换规则合法性, 不校验情况提供给用于编译期已经校验，运行时无需二次校验的情况，实际校验任务由{@link ConvertRuleChecker}接管
     * @throws IllegalArgumentException 格式校验未通过
     */
    public NodePathConverter(String convertRulesJson, String targetNodeTreeSchemaJson, boolean isCheckRules) throws IllegalArgumentException {
        this(new Gson(), convertRulesJson, targetNodeTreeSchemaJson, isCheckRules);
    }

    /**
     * @param convertRulesJsonInputStream         转换规则 格式为 toJson(List<{@link ConvertRule}>)
     * @param targetNodeTreeSchemaJsonInputStream 可选,转换目标节点树的全貌，用于参与转换规则的校验以发现更多规则本身的错误，格式为 toJson(List<{@link NodeTree}>)
     * @throws IllegalArgumentException 格式校验未通过
     */
    public NodePathConverter(InputStream convertRulesJsonInputStream, InputStream targetNodeTreeSchemaJsonInputStream) throws IllegalArgumentException {
        this(convertRulesJsonInputStream, targetNodeTreeSchemaJsonInputStream, true);
    }

    /**
     * @param convertRulesJsonInputStream         转换规则 格式为 toJson(List<{@link ConvertRule}>)
     * @param targetNodeTreeSchemaJsonInputStream 可选,转换目标节点树的全貌，用于参与转换规则的校验以发现更多规则本身的错误，格式为 toJson(List<{@link NodeTree}>)
     * @param isCheckRules                        是否校验转换规则合法性, 不校验情况提供给用于编译期已经校验，运行时无需二次校验的情况，实际校验任务由{@link ConvertRuleChecker}接管
     * @throws IllegalArgumentException 格式校验未通过
     */
    public NodePathConverter(InputStream convertRulesJsonInputStream, InputStream targetNodeTreeSchemaJsonInputStream, boolean isCheckRules) throws IllegalArgumentException {
        this(new Gson(), convertRulesJsonInputStream, targetNodeTreeSchemaJsonInputStream, isCheckRules);
    }

    private NodePathConverter(Gson gson, InputStream convertRulesJsonInputStream, InputStream targetNodeTreeSchemaJsonInputStream, boolean isCheckRules) throws IllegalArgumentException {
        this(
                ConvertRuleChecker.<List<ConvertRule>>parseJson(
                        gson,
                        convertRulesJsonInputStream,
                        new TypeToken<List<ConvertRule>>() {
                        }.getType(),
                        "found error on parse convertRulesJsonInputStream"
                ),
                ConvertRuleChecker.<List<NodeTree>>parseJson(
                        gson,
                        targetNodeTreeSchemaJsonInputStream,
                        new TypeToken<List<NodeTree>>() {
                        }.getType(),
                        "found error on parse targetNodeTreeSchemaJsonInputStream"
                ),
                isCheckRules
        );
    }

    private NodePathConverter(Gson gson, String convertRulesJson, String targetNodeTreeSchemaJson, boolean isCheckRules) throws IllegalArgumentException {
        this(
                ConvertRuleChecker.<List<ConvertRule>>parseJson(
                        gson,
                        convertRulesJson,
                        new TypeToken<List<ConvertRule>>() {
                        }.getType(),
                        "found error on parse convertRulesJson"
                ),
                ConvertRuleChecker.<List<NodeTree>>parseJson(
                        gson,
                        targetNodeTreeSchemaJson,
                        new TypeToken<List<NodeTree>>() {
                        }.getType(),
                        "found error on parse targetNodeTreeSchemaJson"
                ),
                isCheckRules
        );
    }

    /**
     * @param convertRules 转换规则 格式为 toJson(List<{@link ConvertRule}>)
     * @throws IllegalArgumentException 格式校验未通过
     */
    public NodePathConverter(List<ConvertRule> convertRules) throws IllegalArgumentException {
        this(convertRules, null);
    }

    /**
     * @param convertRules 转换规则 格式为 toJson(List<{@link ConvertRule}>)
     * @param isCheckRules 是否校验转换规则合法性, 不校验情况提供给用于编译期已经校验，运行时无需二次校验的情况，实际校验任务由{@link ConvertRuleChecker}接管
     * @throws IllegalArgumentException 格式校验未通过
     */
    public NodePathConverter(List<ConvertRule> convertRules, boolean isCheckRules) throws IllegalArgumentException {
        this(convertRules, null, isCheckRules);
    }

    /**
     * @param convertRules         转换规则 格式为 toJson(List<{@link ConvertRule}>)
     * @param targetNodeTreeSchema 可选,转换目标节点树的全貌，用于参与转换规则的校验以发现更多规则本身的错误
     * @throws IllegalArgumentException 格式校验未通过
     */
    public NodePathConverter(List<ConvertRule> convertRules, List<NodeTree> targetNodeTreeSchema) throws IllegalArgumentException {
        this(convertRules, targetNodeTreeSchema, true);
    }

    /**
     * @param convertRules         转换规则 格式为 toJson(List<{@link ConvertRule}>)
     * @param targetNodeTreeSchema 可选,转换目标节点树的全貌，用于参与转换规则的校验以发现更多规则本身的错误
     * @param isCheckRules         是否校验转换规则合法性, 不校验情况提供给用于编译期已经校验，运行时无需二次校验的情况，实际校验任务由{@link ConvertRuleChecker}接管
     * @throws IllegalArgumentException 格式校验未通过
     */
    public NodePathConverter(List<ConvertRule> convertRules, List<NodeTree> targetNodeTreeSchema, boolean isCheckRules) throws IllegalArgumentException {
        if (convertRules == null) {
            throw new IllegalArgumentException("convertRules can't be null");
        }
        if (isCheckRules) {
            ConvertRuleChecker.check(convertRules, targetNodeTreeSchema);
        }
        this.convertRules = convertRules;
    }

    /**
     * 根据规则转换节点链列表到目标节点链
     *
     * @param sourceNodes 源节点链
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

            result.add(new Node(convertTo.name, args));
        }

        return result;
    }

    /**
     * 匹配节点列表对应的转换规则列表
     *
     * @param nodes 节点列表
     * @return 匹配的节点转换规则列表
     */
    private List<ConvertRule> matchConvertRule(List<Node> nodes) {
        final List<ConvertRule> result = new ArrayList<>();

        List<ConvertRule> currentLevelRules = this.convertRules;

        for (Node node : nodes) {

            ConvertRule matchedConvertRule = null;

            if (currentLevelRules != null) {
                for (ConvertRule convertRule : currentLevelRules) {
                    if (convertRule.name.equals(node.name)) {
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
