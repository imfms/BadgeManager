package ms.imf.redpoint.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import ms.imf.redpoint.compiler.plugin.NodeTreeParsedHandlerPlugin;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Plugin {

    /**
     * the plugin's class after the node tree is parsed
     */
    Class<? extends NodeTreeParsedHandlerPlugin> value();

    /**
     * plugin's arguments
     */
    String[] args() default {};
}
