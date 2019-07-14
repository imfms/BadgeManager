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

import ms.imf.redpoint.entity.NodeTree;

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
                ArgCheckUtil.<List<NodeTree>>parseJson(
                        gson,
                        targetPathsSchemaJson,
                        new TypeToken<List<NodeTree>>() {
                        }.getType(),
                        "found error on parse targetPathsSchemaJson"
                )
        );
    }

    public static void checkArg(InputStream convertRulesJsonReadStream, List<NodeTree> targetPathsSchema) throws IllegalArgumentException {
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

    public static void checkArg(List<ConvertRule> convertRules, List<NodeTree> targetPathsSchema) throws IllegalArgumentException {
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
                throw new IllegalArgumentException(String.format("[%d]: found null value", i));
            }

            if (convertRule.type == null) {
                throw new IllegalArgumentException(String.format("[%d].type: found null value", i));
            }

            if (!repeatCheckTypeNames.add(convertRule.type)) {
                throw new IllegalArgumentException(String.format("[%d].type(%s) repeat type in same node level", i, convertRule.type));
            }

            if (convertRule.args != null) {
                for (int j = 0; j < convertRule.args.size(); j++) {
                    String arg = convertRule.args.get(j);
                    if (arg == null
                            || arg.length() <= 0) {
                        throw new IllegalArgumentException(String.format("[%d](%s).args[%d]: found null value", i, convertRule.type, j));
                    }
                }
            }

            try {
                checkConvertTo(convertRule.convertTo);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(String.format("[%d](%s).convertTo: %s", i, convertRule.type, e.getMessage()), e);
            }

            if (convertRule.sub != null) {
                try {
                    checkConvertRules(convertRule.sub);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(String.format("[%d](%s).sub: %s", i, convertRule.type, e.getMessage()), e);
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
                throw new IllegalArgumentException(String.format("[%d]: found null value", i));
            }

            if (convertTo.type == null) {
                throw new IllegalArgumentException(String.format("[%d].type: found null value", i));
            }

            if (convertTo.args != null) {
                try {
                    checkConvertToArgs(convertTo);
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                            String.format("[%d](%s): %s", i, convertTo.type, e.getMessage()), e
                    );
                }
            }
        }
    }

    private static void checkConvertToArgs(ConvertRule.ConvertTo convertTo) {
        for (int i = 0; i < convertTo.args.size(); i++) {
            ConvertRule.Arg arg = convertTo.args.get(i);

            try {

                if (arg == null) { throw new IllegalArgumentException("found null value"); }

                if (arg.hisArg == null) { throw new IllegalArgumentException("found null value"); }

                if (arg.value == null
                        && arg.refValue == null) {
                    throw new IllegalArgumentException("value and refValue must exist one, but found none");
                }

                if (arg.value != null
                        && arg.refValue != null) {
                    throw new IllegalArgumentException("value and refValue only can exist one, but found more");
                }

                if (arg.refValue != null) {
                    if (arg.refValue.myLevel == null) {
                        throw new IllegalArgumentException("refValue.myLevel: myLevel can't be null, but found null value");
                    }
                    if (arg.refValue.myLevel < 0) {
                        throw new IllegalArgumentException(String.format("refValue.myLevel(%d): myLevel can't less 0", arg.refValue.myLevel));
                    }
                    if (arg.refValue.myArg == null) {
                        throw new IllegalArgumentException("refValue.myArg: found null value");
                    }
                }

            } catch (Exception e) {
                throw new IllegalArgumentException(
                        String.format("args[%d]: %s", i, e.getMessage()), e
                );
            }
        }
    }

    private static void checkConvertRulesConvertToArgValidity(List<ConvertRule> convertRules, List<NodeTree> targetPathsSchema) throws IllegalArgumentException {
        checkConvertRulesConvertToArgValidity(convertRules, Collections.<ConvertRule>emptyList(), targetPathsSchema);
    }

    private static void checkConvertRulesConvertToArgValidity(List<ConvertRule> checkConvertRules, List<ConvertRule> upToNowConvertRules, List<NodeTree> targetPathsSchema) throws IllegalArgumentException {

        for (int ruleIndex = 0; ruleIndex < checkConvertRules.size(); ruleIndex++) {
            ConvertRule convertRule = checkConvertRules.get(ruleIndex);

            // convertRule.convertTo
            if (convertRule.convertTo != null) {
                try {
                    checkConvertRuleConvertTo(concatToNew(upToNowConvertRules, convertRule), targetPathsSchema, convertRule);
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                            String.format("[%d](%s): %s", ruleIndex, convertRule.type, e.getMessage()), e
                    );
                }
            }

            // convertRule.sub
            if (convertRule.sub != null) {
                try {
                    checkConvertRulesConvertToArgValidity(convertRule.sub, concatToNew(upToNowConvertRules, convertRule), targetPathsSchema);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(String.format("[%d](%s).sub: %s", ruleIndex, convertRule.type, e.getMessage()), e);
                }
            }
        }
    }

    private static List<NodeTree> matchNodeSchema(List<NodeTree> targetPathsSchema, List<ConvertRule.ConvertTo> convertTos) throws IllegalArgumentException {
        final List<NodeTree> results = new ArrayList<>(convertTos.size());

        List<NodeTree> compareNodeTrees = targetPathsSchema;
        for (ConvertRule.ConvertTo convertTo : convertTos) {

            NodeTree rightNodeTree = null;

            if (compareNodeTrees != null) {
                for (NodeTree compareNodeTree : compareNodeTrees) {
                    if (compareNodeTree.type.equals(convertTo.type)) {
                        rightNodeTree = compareNodeTree;
                    }
                }
            }

            if (rightNodeTree == null) {

                // errorNodesTipSb == true/true/*false/false/false*
                Iterator<ConvertRule.ConvertTo> iterator = convertTos.iterator();
                final StringBuilder errorNodesTipSb = new StringBuilder();
                while (iterator.hasNext()) {
                    ConvertRule.ConvertTo next = iterator.next();

                    if (next == convertTo) {
                        errorNodesTipSb.append('*');
                    }

                    errorNodesTipSb.append(next.type);

                    errorNodesTipSb.append(
                            iterator.hasNext() ? '/' : '*'
                    );
                }

                throw new IllegalArgumentException(String.format("not match target node schema: %s", errorNodesTipSb));
            } else {
                results.add(rightNodeTree);
                compareNodeTrees = rightNodeTree.sub;
            }
        }

        return results;
    }

    private static void checkTargetPathsSchema(List<NodeTree> targetPathsSchema) throws IllegalArgumentException {

        final HashSet<String> repeatCheckTypeNames = new HashSet<>();

        for (int schemaIndex = 0; schemaIndex < targetPathsSchema.size(); schemaIndex++) {
            NodeTree nodeTree = targetPathsSchema.get(schemaIndex);

            try {

                if (nodeTree == null) {
                    throw new IllegalArgumentException("found null value");
                }

                if (nodeTree.type == null) {
                    throw new IllegalArgumentException("type: found null value");
                }

                if (!repeatCheckTypeNames.add(nodeTree.type)) {
                    throw new IllegalArgumentException("repeat type in same level");
                }

                if (nodeTree.args != null) {
                    for (int schemaArgIndex = 0; schemaArgIndex < nodeTree.args.size(); schemaArgIndex++) {
                        NodeTree.Arg arg = nodeTree.args.get(schemaArgIndex);
                        if (arg == null) {
                            throw new IllegalArgumentException(String.format("args[%d]: found null value", schemaArgIndex));
                        }

                        if (arg.name == null) {
                            throw new IllegalArgumentException(String.format("args[%d].name: found null value", schemaArgIndex));
                        }

                        if (arg.valueLimits != null) {
                            int nullIndex = arg.valueLimits.indexOf(null);
                            if (nullIndex >= 0) {
                                throw new IllegalArgumentException(String.format("args[%d].valueLimits[%d]: found null value", schemaArgIndex, nullIndex));
                            }
                        }
                    }
                }

                if (nodeTree.sub != null) {
                    try {
                        checkTargetPathsSchema(nodeTree.sub);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException(String.format("sub: %s", e.getMessage()));
                    }
                }

            } catch (Exception e) {
                throw new IllegalArgumentException(
                        String.format(
                                "[%d]%s: %s",
                                schemaIndex,
                                nodeTree != null && nodeTree.type != null
                                        ? String.format("(%s)", nodeTree.type)
                                        : "",
                                e.getMessage()
                        ),
                        e
                );
            }

        }

    }

    private static void checkConvertRuleConvertTo(List<ConvertRule> upToNowConvertRules, List<NodeTree> targetPathsSchema, ConvertRule convertRule) {

        // check types
        final List<NodeTree> matchedNodeTrees;
        if (targetPathsSchema == null) {
            matchedNodeTrees = null;
        } else {
            try {
                matchedNodeTrees = matchNodeSchema(targetPathsSchema, convertRule.convertTo);
            } catch (Exception e) {
                throw new IllegalArgumentException(String.format("convertTo: %s", e.getMessage()), e);
            }
        }

        // convertTo.args
        for (int convertToIndex = 0; convertToIndex < convertRule.convertTo.size(); convertToIndex++) {
            ConvertRule.ConvertTo convertTo = convertRule.convertTo.get(convertToIndex);

            NodeTree matchedNodeTree = matchedNodeTrees != null
                    ? matchedNodeTrees.get(convertToIndex)
                    : null;

            if (convertTo.args == null) {
                continue;
            }

            for (int convertToArgIndex = 0; convertToArgIndex < convertTo.args.size(); convertToArgIndex++) {
                ConvertRule.Arg arg = convertTo.args.get(convertToArgIndex);
                NodeTree.Arg matchedNodeArg = matchedNodeTree != null
                        ? getNodeSchemaNodeArgFromArgName(arg.hisArg, matchedNodeTree)
                        : null;

                // arg.hisArg
                if (matchedNodeTree != null) {
                    if (!nodeSchemaContainsArgName(arg.hisArg, matchedNodeTree)) {
                        throw new IllegalArgumentException(String.format(
                                "convertTo[%d](%s).args[%d].hisArg(%s): target nodeSchema.args('%s') not contains arg '%s'",
                                convertToIndex, convertTo.type, convertToArgIndex,
                                arg.hisArg, matchedNodeTree.args, arg.hisArg
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
                                        "convertTo[%d](%s).args[%d].value(%s): target nodeSchema.arg(%s).valueLimits(%s) not contains value '%s'",
                                        convertToIndex, convertTo.type, convertToArgIndex,
                                        arg.hisArg, matchedNodeArg.name, matchedNodeArg.valueLimits, arg.value
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
                                "convertTo[%d](%s).args[%d].refValue.myLevel(%d): up to now convertRules(size = %d, %s) not contain level '%d' (level start from 0)",
                                convertToIndex, convertTo.type, convertToArgIndex, arg.refValue.myLevel,
                                upToNowConvertRules.size(), getMultiLevelConvertRuleTipStr(upToNowConvertRules), arg.refValue.myLevel
                        ));
                    }

                    // refValue.myArg
                    ConvertRule targetLevelConvertRule = upToNowConvertRules.get(arg.refValue.myLevel);
                    if (targetLevelConvertRule.args == null
                            || !targetLevelConvertRule.args.contains(arg.refValue.myArg)) {
                        throw new IllegalArgumentException(String.format(
                                "convertTo[%d](%s).args[%d].refValue.myArg(%s): target level(%d) convertRule's args(%s) not contains arg '%s'",
                                convertToIndex, convertTo.type, convertToArgIndex, arg.refValue.myArg,
                                arg.refValue.myLevel, targetLevelConvertRule.args, arg.refValue.myArg
                        ));
                    }
                }

            }
        }
    }

    private static boolean nodeSchemaContainsArgName(String argName, NodeTree nodeTree) {
        return getNodeSchemaNodeArgFromArgName(argName, nodeTree) != null;
    }

    private static NodeTree.Arg getNodeSchemaNodeArgFromArgName(String argName, NodeTree nodeTree) {
        if (nodeTree == null
                || nodeTree.args == null
                || nodeTree.args.isEmpty()) {
            return null;
        }

        NodeTree.Arg result = null;
        for (NodeTree.Arg nodeArg : nodeTree.args) {
            if (argName.equals(nodeArg.name)) {
                result = nodeArg;
                break;
            }
        }

        return result;
    }

    private static StringBuffer getMultiLevelConvertRuleTipStr(List<ConvertRule> convertRules) {
        final StringBuffer upToNowRulesTipStr = new StringBuffer();

        Iterator<ConvertRule> iterator = convertRules.iterator();
        while (iterator.hasNext()) {
            ConvertRule rule = iterator.next();

            upToNowRulesTipStr.append(rule.type);

            if (iterator.hasNext()) {
                upToNowRulesTipStr.append('/');
            }
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
