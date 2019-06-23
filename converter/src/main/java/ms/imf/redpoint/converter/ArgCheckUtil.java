package ms.imf.redpoint.converter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import ms.imf.redpoint.entity.NodeSchema;

public class ArgCheckUtil {

    public static void checkArg(String convertRulesJson, String targetPathsSchemaJson) throws IllegalArgumentException {
        Gson gson = new Gson();
        checkArg(
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
                )
        );
    }

    public static void checkArg(InputStream convertRulesJsonReadStream, List<NodeSchema> targetPathsSchema) throws IllegalArgumentException {
        checkArg(
                ArgCheckUtil.<List<ConvertRule>>parseJson(
                        new Gson(),
                        convertRulesJsonReadStream,
                        new TypeToken<List<ConvertRule>>() {
                        }.getType(),
                        "found error on parse convertRulesJsonReadStream"
                ),
                targetPathsSchema
        );
    }

    public static void checkArg(List<ConvertRule> convertRules, List<NodeSchema> targetPathsSchema) throws IllegalArgumentException {
        if (convertRules == null) {
            throw new IllegalArgumentException("convertRules can't be null");
        }

        try {
            checkConvertRules(convertRules);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("found error on check convertRules: %s", e.getMessage()));
        }

        if (targetPathsSchema != null) {
            try {
                checkTargetPathsSchema(targetPathsSchema);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(String.format("found error on check targetPathsSchema: %s", e.getMessage()), e);
            }
        }

        try {
            checkConvertRulesConvertToArgValidity(convertRules, targetPathsSchema);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("found error on check convertRules: %s", e.getMessage()), e);
        }
    }

    private static void checkConvertRules(List<ConvertRule> convertRules) throws IllegalArgumentException {

        final HashSet<String> repeatCheckTypeNames = new HashSet<>();

        for (int i = 0; i < convertRules.size(); i++) {
            ConvertRule convertRule = convertRules.get(i);

            if (convertRule == null) {
                throw new IllegalArgumentException(String.format("convertRules can't contain null value , but found on index '%d'", i));
            }

            if (convertRule.type == null
                    || convertRule.type.length() <= 0) {
                throw new IllegalArgumentException(String.format("convertRule.type can't be null or empty, but found on convertRules[%d]", i));
            }

            if (!repeatCheckTypeNames.add(convertRule.type)) {
                throw new IllegalArgumentException(String.format("found repeat type in same level: %s", convertRule.type));
            }

            if (convertRule.args != null) {
                for (int j = 0; j < convertRule.args.size(); j++) {
                    String arg = convertRule.args.get(j);
                    if (arg == null
                            || arg.length() <= 0) {
                        throw new IllegalArgumentException(String.format("convertRule.args can't contain null or empty, but found on convertRules[%d].args[%d]", i, j));
                    }
                }
            }

            try {
                checkConvertTo(convertRule.convertTo);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(String.format("found error on check convertRules[%s].convertTo: %s", i, e.getMessage()), e);
            }

            if (convertRule.sub != null) {
                try {
                    checkConvertRules(convertRule.sub);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(String.format("found error on check convertRules[%d].sub: %s", i, e.getMessage()), e);
                }
            }
        }
    }

    private static void checkConvertTo(List<ConvertRule.ConvertTo> convertTos) throws IllegalArgumentException {
        if (convertTos == null) {
            return;
        }
        for (int i = 0; i < convertTos.size(); i++) {
            ConvertRule.ConvertTo convertTo = convertTos.get(i);

            if (convertTo == null) {
                throw new IllegalArgumentException(String.format("can't contain null value, but found on index '%d'", i));
            }

            if (convertTo.type == null
                    || convertTo.type.isEmpty()) {
                throw new IllegalArgumentException(String.format("type can't be null or empty, but found on index '%d'", i));
            }

            if (convertTo.args != null) {
                for (int j = 0; j < convertTo.args.size(); j++) {
                    ConvertRule.Arg arg = convertTo.args.get(j);

                    if (arg == null) {
                        throw new IllegalArgumentException(String.format("args can't be null but found on convertTos[%d].args[%d]", i, j));
                    }

                    if (arg.hisArg == null
                            || arg.hisArg.length() <= 0) {
                        throw new IllegalArgumentException(String.format("args.hisArg can't be null or empty but found on convertTos[%d].args[%d]", i, j));
                    }

                    if (arg.value == null
                            && arg.refValue == null) {
                        throw new IllegalArgumentException(String.format("args' value and refValue must and only exist one but found none on convertTos[%d].args[%d]", i, j));
                    }

                    if (arg.value != null
                            && arg.refValue != null) {
                        throw new IllegalArgumentException(String.format("args' value and refValue only can exist one but found more on convertTos[%d].args[%d]", i, j));
                    }

                    if (arg.refValue != null) {
                        if (arg.refValue.myLevel == null) {
                            throw new IllegalArgumentException(String.format("args.refValue.myLevel can't be null but found on convertTos[%d].args[%d]", i, j));
                        }
                        if (arg.refValue.myLevel < 0) {
                            throw new IllegalArgumentException(String.format("args.refValue.myLevel can't less 0 but found '%d' on convertTos[%d].args[%d]", arg.refValue.myLevel, i, j));
                        }
                        if (arg.refValue.myArg == null
                                || arg.refValue.myArg.length() <= 0) {
                            throw new IllegalArgumentException(String.format("args.refValue.myArg can't be null or empty but found on convertTos[%d].args[%d]", i, j));
                        }
                    }
                }
            }
        }
    }

    private static void checkConvertRulesConvertToArgValidity(List<ConvertRule> convertRules, List<NodeSchema> targetPathsSchema) throws IllegalArgumentException {
        checkConvertRulesConvertToArgValidity(convertRules, Collections.<ConvertRule>emptyList(), targetPathsSchema);
    }

    private static void checkConvertRulesConvertToArgValidity(List<ConvertRule> checkConvertRules, List<ConvertRule> upToNowConvertRules, List<NodeSchema> targetPathsSchema) throws IllegalArgumentException {

        for (int ruleIndex = 0; ruleIndex < checkConvertRules.size(); ruleIndex++) {
            ConvertRule convertRule = checkConvertRules.get(ruleIndex);

            // convertRule.convertTo
            if (convertRule.convertTo != null) {
                checkConvertRuleConvertTo(concatToNew(upToNowConvertRules, convertRule), targetPathsSchema, ruleIndex, convertRule);
            }

            // convertRule.sub
            if (convertRule.sub != null) {
                try {
                    checkConvertRulesConvertToArgValidity(convertRule.sub, concatToNew(upToNowConvertRules, convertRule), targetPathsSchema);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(String.format("found error on check convertRule[%d].sub: %s", ruleIndex, e.getMessage()), e);
                }
            }
        }
    }

    private static List<NodeSchema> matchNodeSchema(List<NodeSchema> targetPathsSchema, List<ConvertRule.ConvertTo> convertTos) throws IllegalArgumentException {
        final List<NodeSchema> results = new ArrayList<>(convertTos.size());

        List<NodeSchema> compareNodeSchemas = targetPathsSchema;
        for (ConvertRule.ConvertTo convertTo : convertTos) {

            NodeSchema rightNodeSchema = null;

            if (compareNodeSchemas != null) {
                for (NodeSchema compareNodeSchema : compareNodeSchemas) {
                    if (compareNodeSchema.type.equals(convertTo.type)) {
                        rightNodeSchema = compareNodeSchema;
                    }
                }
            }

            if (rightNodeSchema == null) {

                // errorNodesTipSb == true/true/[false/false/false]
                Iterator<ConvertRule.ConvertTo> iterator = convertTos.iterator();
                final StringBuilder errorNodesTipSb = new StringBuilder();
                while (iterator.hasNext()) {
                    ConvertRule.ConvertTo next = iterator.next();

                    if (next == convertTo) {
                        errorNodesTipSb.append('[');
                    }
                    errorNodesTipSb
                            .append(next.type)
                            .append(
                                    iterator.hasNext() ? '/' : ']'
                            );
                }

                throw new IllegalArgumentException(String.format("convertTos not matched targetPathsSchema: %s", errorNodesTipSb));
            } else {
                results.add(rightNodeSchema);
                compareNodeSchemas = rightNodeSchema.sub;
            }
        }

        return results;
    }

    private static void checkTargetPathsSchema(List<NodeSchema> targetPathsSchema) throws IllegalArgumentException {

        final HashSet<String> repeatCheckTypeNames = new HashSet<>();

        for (int i = 0; i < targetPathsSchema.size(); i++) {
            NodeSchema nodeSchema = targetPathsSchema.get(i);

            if (nodeSchema == null) {
                throw new IllegalArgumentException(String.format("nodeSchema can't be null, but found on targetPathsSchema[%d]", i));
            }

            if (nodeSchema.type == null
                    || nodeSchema.type.length() <= 0) {
                throw new IllegalArgumentException(String.format("nodeSchema.type can't be null or empty, but found on targetPathsSchema[%d]", i));
            }

            if (!repeatCheckTypeNames.add(nodeSchema.type)) {
                throw new IllegalArgumentException(String.format("found repeat type in same level: %s", nodeSchema.type));
            }

            if (nodeSchema.args != null) {
                for (int j = 0; j < nodeSchema.args.size(); j++) {
                    NodeSchema.NodeArg arg = nodeSchema.args.get(j);
                    if (arg == null) {
                        throw new IllegalArgumentException(String.format("nodeSchema.args can't contain null value, but found on targetPathsSchema[%d].args[%d]", i, j));
                    }

                    if (arg.name == null) {
                        throw new IllegalArgumentException(String.format("nodeSchema.args.name can't be null, but found on targetPathsSchema[%d].args[%d].name", i, j));
                    }

                    if (arg.valueLimits != null) {
                        int nullIndex = arg.valueLimits.indexOf(null);
                        if (nullIndex >= 0) {
                            throw new IllegalArgumentException(String.format("nodeSchema.args.name.valueLimits can't contain null value, but found on targetPathsSchema[%d].args[%d].valueLimits[%d]", i, j, nullIndex));
                        }
                    }
                }
            }

            if (nodeSchema.sub != null) {
                try {
                    checkTargetPathsSchema(nodeSchema.sub);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(String.format("found error on check nodeSchema[%d].sub: %s", i, e.getMessage()));
                }
            }

        }

    }

    private static void checkConvertRuleConvertTo(List<ConvertRule> upToNowConvertRules, List<NodeSchema> targetPathsSchema, int ruleIndex, ConvertRule convertRule) {

        // check types
        final List<NodeSchema> matchedNodeSchemas;
        if (targetPathsSchema == null) {
            matchedNodeSchemas = null;
        } else {
            try {
                matchedNodeSchemas = matchNodeSchema(targetPathsSchema, convertRule.convertTo);
            } catch (Exception e) {
                throw new IllegalArgumentException(String.format("found error on match convertRule[%d]: %s", ruleIndex, e.getMessage()), e);
            }
        }

        // convertTo.args
        for (int convertToIndex = 0; convertToIndex < convertRule.convertTo.size(); convertToIndex++) {
            ConvertRule.ConvertTo convertTo = convertRule.convertTo.get(convertToIndex);

            NodeSchema matchedNodeSchema = matchedNodeSchemas != null
                    ? matchedNodeSchemas.get(convertToIndex)
                    : null;

            if (convertTo.args == null) {
                continue;
            }

            for (int argIndex = 0; argIndex < convertTo.args.size(); argIndex++) {
                ConvertRule.Arg arg = convertTo.args.get(argIndex);
                NodeSchema.NodeArg matchedNodeArg = matchedNodeSchema != null
                        ? getNodeSchemaNodeArgFromArgName(arg.hisArg, matchedNodeSchema)
                        : null;

                // arg.hisArg
                if (matchedNodeSchema != null) {
                    if (!nodeSchemaContainsArgName(arg.hisArg, matchedNodeSchema)) {
                        throw new IllegalArgumentException(String.format(
                                "found error on check convertRule[%d].convertTo[%d].args[%d].hisArg '%s': target nodeSchema.args('%s') not contains arg '%s'",
                                ruleIndex, convertToIndex, argIndex,
                                arg.hisArg, matchedNodeSchema.args, arg.hisArg
                        ));
                    }
                }

                // arg.value
                if (arg.value != null) {
                    if (matchedNodeArg != null) {
                        if (matchedNodeArg.valueLimits != null
                                && !matchedNodeArg.valueLimits.isEmpty()) {
                            if (!matchedNodeArg.valueLimits.contains(arg.value)) {
                                throw new IllegalArgumentException(String.format(
                                        "found error on check convertRule[%d].convertTo[%d].args[%d].value '%s': target nodeSchema.args('%s').valueLimits not contains value '%s'",
                                        ruleIndex, convertToIndex, argIndex,
                                        arg.hisArg, matchedNodeSchema.args, arg.value
                                ));
                            }
                        }
                    }
                }

                // arg.refValue
                if (arg.refValue != null) {

                    // refValue.myLevel
                    if (arg.refValue.myLevel > upToNowConvertRules.size() - 1) {
                        throw new IllegalArgumentException(String.format(
                                "found error on check convertRule[%d].convertTo[%d].args[%d].refValue.myLevel '%d': up to now convertRules(size = %d, %s) not contain level '%d' (level start from 0)",
                                ruleIndex, convertToIndex, argIndex,
                                arg.refValue.myLevel, upToNowConvertRules.size(), getMultiLevelConvertRuleTipStr(upToNowConvertRules), arg.refValue.myLevel
                        ));
                    }

                    // refValue.myArg
                    ConvertRule targetLevelConvertRule = upToNowConvertRules.get(arg.refValue.myLevel);
                    if (targetLevelConvertRule.args == null
                            || !targetLevelConvertRule.args.contains(arg.refValue.myArg)) {
                        throw new IllegalArgumentException(String.format(
                                "found error on check convertRule[%d].convertTo[%d].args[%d].refValue.myArg '%s': target level(%d) convertRule's args('%s') not contains arg '%s'",
                                ruleIndex, convertToIndex, argIndex,
                                arg.refValue.myArg, arg.refValue.myLevel, targetLevelConvertRule.args, arg.refValue.myArg
                        ));
                    }
                }

            }
        }
    }

    private static boolean nodeSchemaContainsArgName(String argName, NodeSchema nodeSchema) {
        return getNodeSchemaNodeArgFromArgName(argName, nodeSchema) != null;
    }

    private static NodeSchema.NodeArg getNodeSchemaNodeArgFromArgName(String argName, NodeSchema nodeSchema) {
        if (nodeSchema == null
                || nodeSchema.args == null
                || nodeSchema.args.isEmpty()) {
            return null;
        }

        NodeSchema.NodeArg result = null;
        for (NodeSchema.NodeArg nodeArg : nodeSchema.args) {
            if (argName.equals(nodeArg.name)) {
                result = nodeArg;
                break;
            }
        }

        return result;
    }

    private static StringBuffer getMultiLevelConvertRuleTipStr(List<ConvertRule> convertRules) {
        final StringBuffer upToNowRulesTipStr = new StringBuffer("convertRules");
        for (ConvertRule rule : convertRules) {
            upToNowRulesTipStr
                    .append('[')
                    .append(rule.type)
                    .append(']');
        }
        return upToNowRulesTipStr;
    }

    private static List<ConvertRule> concatToNew(List<ConvertRule> list, ConvertRule convertRule) {
        final List<ConvertRule> nextUpToNowConvertRules = new ArrayList<>(list.size() + 1);
        nextUpToNowConvertRules.addAll(list);
        nextUpToNowConvertRules.add(convertRule);
        return nextUpToNowConvertRules;
    }

    static <T> T parseJson(Gson gson, String json, Type type, String errorDescribe) throws IllegalArgumentException {
        if (json == null) {
            return null;
        }
        try {
            return gson.fromJson(json, type);
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("%s: %s", errorDescribe, e.getMessage()), e);
        }
    }

    static <T> T parseJson(Gson gson, InputStream jsonInputStream, Type type, String errorDescribe) throws IllegalArgumentException {
        if (jsonInputStream == null) {
            return null;
        }
        try {
            return gson.fromJson(new InputStreamReader(jsonInputStream), type);
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("%s: %s", errorDescribe, e.getMessage()), e);
        }
    }

}
