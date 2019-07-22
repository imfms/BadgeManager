package ms.imf.redpoint.compiler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import ms.imf.redpoint.annotation.Mapper;
import ms.imf.redpoint.annotation.NodeContainer;
import ms.imf.redpoint.annotation.NodeParserGlobalConfig;
import ms.imf.redpoint.annotation.Plugin;
import ms.imf.redpoint.annotation.SubNode;
import ms.imf.redpoint.annotation.SubNode2;
import ms.imf.redpoint.annotation.SubNode3;
import ms.imf.redpoint.annotation.SubNode4;
import ms.imf.redpoint.annotation.SubNode5;
import ms.imf.redpoint.annotation.SubNode6;
import ms.imf.redpoint.annotation.SubNode7;
import ms.imf.redpoint.annotation.SubNode8;
import ms.imf.redpoint.annotation.SubNode9;

abstract class Constants {

    private Constants() {
        throw new IllegalStateException("no instance");
    }

    /**
     * @see NodeContainer#nodeJsonRefContainerMapper()
     */
    static final String NodeContainer_nodeJsonRefContainerMapper = "nodeJsonRefContainerMapper";
    /**
     * @see NodeContainer#value()
     */
    static final String NodeContainer_value = "value";
    /**
     * @see Mapper#key()
     */
    static final String Mapper_key = "key";
    /**
     * @see Mapper#value()
     */
    static final String Mapper_value = "value";
    /**
     * @see NodeParserGlobalConfig#eachAptRoundNodeTreeParsedPlugins()
     */
    static final String NodeParserGlobalConfig_eachAptRoundNodeTreeParsedPlugins = "eachAptRoundNodeTreeParsedPlugins";
    /**
     * @see NodeParserGlobalConfig#lastAptRoundNodeTreeParsedPlugins()
     */
    static final String NodeParserGlobalConfig_lastAptRoundNodeTreeParsedPlugins = "lastAptRoundNodeTreeParsedPlugins";
    /**
     * @see Plugin#value()
     */
    static final String Plugin_value = "value";
    /**
     * @see SubNode#subNodes()
     */
    static final String SubNode_subNodes = "subNodes";
    /**
     * @see SubNode#subNodeContainerRef()
     */
    static final String SubNode_subNodeContainerRef = "subNodeContainerRef";

    static void selfCheck() {

        for (Object[][] objects : new Object[][][]{
                {{NodeContainer.class}, {NodeContainer_nodeJsonRefContainerMapper, NodeContainer_value}},
                {{Mapper.class}, {Mapper_key, Mapper_value}},
                {
                        {NodeParserGlobalConfig.class},
                        {
                                NodeParserGlobalConfig_eachAptRoundNodeTreeParsedPlugins,
                                NodeParserGlobalConfig_lastAptRoundNodeTreeParsedPlugins
                        }
                },
                {{Plugin.class}, {Plugin_value}},
                {
                        {
                                SubNode.class, SubNode2.class, SubNode3.class,
                                SubNode4.class, SubNode5.class, SubNode6.class,
                                SubNode7.class, SubNode8.class,
                        },
                        {SubNode_subNodes, SubNode_subNodeContainerRef}
                },
                {{SubNode9.class}, {SubNode_subNodeContainerRef}}
        }) {

            for (Object type : objects[0]) {

                List<String> methodNames = getMethodNames((Class) type);

                for (Object name : objects[1]) {
                    if (!methodNames.contains(name)) {
                        throw new IllegalStateException(String.format(
                                "self-check fail: type '%s' not exist method '%s'",
                                ((Class) type).getCanonicalName(),
                                name
                        ));
                    }
                }
            }
        }
    }

    private static List<String> getMethodNames(Class type) {

        Method[] methods = type.getMethods();

        List<String> methodNames = new ArrayList<>(methods.length);

        for (int i = 0; i < methods.length; i++) {
            methodNames.add(
                    methods[i].getName()
            );
        }

        return methodNames;
    }

}
