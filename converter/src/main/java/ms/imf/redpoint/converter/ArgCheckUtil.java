package ms.imf.redpoint.converter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import ms.imf.redpoint.entity.NodeSchema;

class ArgCheckUtil {

    static void checkArg(List<ConvertRule> convertRules, List<NodeSchema> targetPathsSchema) throws IllegalArgumentException {
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
        for (int i = 0; i < convertRules.size(); i++) {
            ConvertRule convertRule = convertRules.get(i);

            if (convertRule == null) {
                throw new IllegalArgumentException(String.format("convertRules can't contain null value , but found on index '%d'", i));
            }

            if (convertRule.type == null
                    || convertRule.type.length() <= 0) {
                throw new IllegalArgumentException(String.format("convertRule.type can't be null or empty, but found on convertRules[%d]", i));
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
                throw new IllegalArgumentException(String.format("convertTo can't contain null value, but found on index '%d'", i));
            }

            if (convertTo.type == null
                    || convertTo.type.isEmpty()) {
                throw new IllegalArgumentException(String.format("convertTo.type can't be null or empty, but found on index '%d'", i));
            }

            if (convertTo.args != null) {
                for (int j = 0; j < convertTo.args.size(); j++) {
                    ConvertRule.Arg arg = convertTo.args.get(j);

                    if (arg.myLevel == null) {
                        throw new IllegalArgumentException(String.format("convertTo.myLevel can't be null but found on convertTos[%d].args[%d]", i, j));
                    }

                    if (arg.myLevel < 0) {
                        throw new IllegalArgumentException(String.format("convertTo.myLevel can't less 0 but found '%d' on convertTos[%d].args[%d]", arg.myLevel, i, j));
                    }

                    if (arg.myArg == null
                            || arg.myArg.length() <= 0) {
                        throw new IllegalArgumentException(String.format("convertTo.myArg can't be null or empty but found on convertTos[%d].args[%d]", i, j));

                    }
                    if (arg.hisArg == null
                            || arg.hisArg.length() <= 0) {
                        throw new IllegalArgumentException(String.format("convertTo.hisArg can't be null or empty but found on convertTos[%d].args[%d]", i, j));
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

        for (int i = 0; i < targetPathsSchema.size(); i++) {
            NodeSchema nodeSchema = targetPathsSchema.get(i);

            if (nodeSchema == null) {
                throw new IllegalArgumentException(String.format("nodeSchema can't be null, but found on targetPathsSchema[%d]", i));
            }

            if (nodeSchema.type == null
                    || nodeSchema.type.length() <= 0) {
                throw new IllegalArgumentException(String.format("nodeSchema.type can't be null or empty, but found on targetPathsSchema[%d]", i));
            }

            if (nodeSchema.args != null) {
                for (int j = 0; j < nodeSchema.args.size(); j++) {
                    String arg = nodeSchema.args.get(j);
                    if (arg == null
                            || arg.length() <= 0) {
                        throw new IllegalArgumentException(String.format("nodeSchema.args can't contain null or empty, but found on targetPathsSchema[%d].args[%d]", i, j));
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

            if (convertTo.args == null) {
                continue;
            }

            for (int argIndex = 0; argIndex < convertTo.args.size(); argIndex++) {
                ConvertRule.Arg arg = convertTo.args.get(argIndex);

                // arg.myLevel
                if (arg.myLevel > upToNowConvertRules.size() - 1) {
                    throw new IllegalArgumentException(String.format(
                            "found error on check convertRule[%d].convertTo[%d].args[%d].myLevel '%d': up to now convertRules(size = %d, %s) not contain level '%d' (level start from 0)",
                            ruleIndex, convertToIndex, argIndex,
                            arg.myLevel, upToNowConvertRules.size(), getMultiLevelConvertRuleTipStr(upToNowConvertRules), arg.myLevel
                    ));
                }

                // arg.myArg
                ConvertRule targetLevelConvertRule = upToNowConvertRules.get(arg.myLevel);
                if (targetLevelConvertRule.args == null
                        || !targetLevelConvertRule.args.contains(arg.myArg)) {
                    throw new IllegalArgumentException(String.format(
                            "found error on check convertRule[%d].convertTo[%d].args[%d].myArg '%s': target level(%d) convertRule's args('%s') not contains arg '%s'",
                            ruleIndex, convertToIndex, argIndex,
                            arg.myArg, arg.myLevel, targetLevelConvertRule.args, arg.myArg
                    ));
                }

                // arg.hisArg
                if (matchedNodeSchemas != null) {
                    NodeSchema nodeSchema = matchedNodeSchemas.get(convertToIndex);
                    if (nodeSchema.args == null
                            || !nodeSchema.args.contains(arg.hisArg)) {
                        throw new IllegalArgumentException(String.format(
                                "found error on check convertRule[%d].convertTo[%d].args[%d].hisArg '%s': target nodeSchema.args('%s') not contains arg '%s'",
                                ruleIndex, convertToIndex, argIndex,
                                arg.hisArg, nodeSchema.args, arg.hisArg
                        ));
                    }
                }
            }
        }
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
}
