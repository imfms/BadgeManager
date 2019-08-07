package ms.imf.badge.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import ms.imf.badge.compiler.plugin.NodeTreeHandlePlugin;

/**
 * 节点解析器插件
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Plugin {

    /**
     * 插件类class
     */
    Class<? extends NodeTreeHandlePlugin> value();

    /**
     * 插件执行参数，该字段将会在插件运行时提供给插件类的实例，用于对插件的定制化功能提供支持
     */
    String[] args() default {};
}
