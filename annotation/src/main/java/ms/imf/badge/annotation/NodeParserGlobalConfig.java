package ms.imf.badge.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.annotation.processing.RoundEnvironment;

/**
 * 节点解析器全局配置，用于设置注解处理器提供的一些个性化选项
 *
 * @see NodeContainer
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface NodeParserGlobalConfig {

    /**
     * 注解处理器每轮对节点解析后执行的插件
     *
     * <p>
     * when {@link RoundEnvironment#processingOver()} == false
     */
    Plugin[] eachAptRoundNodeTreeParsedPlugins() default {};

    /**
     * 注解处理器最后一轮对节点解析后执行的插件，本轮不支持创建java-source-file
     *
     * <p>
     * when {@link RoundEnvironment#processingOver()} == true
     */
    Plugin[] lastAptRoundNodeTreeParsedPlugins() default {};
}
