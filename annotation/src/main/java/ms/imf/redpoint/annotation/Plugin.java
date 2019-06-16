package ms.imf.redpoint.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import ms.imf.redpoint.compiler.plugin.ParsedNodeSchemaHandlePlugin;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Plugin {

    /**
     * parsed node schemas handle plugin's class
     */
    Class<? extends ParsedNodeSchemaHandlePlugin> value();

    /**
     * plugin's arguments
     */
    String[] args() default {};
}
