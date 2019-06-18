package ms.imf.redpoint.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface PathAptGlobalConfig {

    /**
     * each apt round parsed node schemas handle plugins
     */
    Plugin[] eachAptRoundPlugins() default {};

    /**
     * last apt round parsed node schemas handle plugins, can't create java source file
     */
    Plugin[] lastAptRoundPlugins() default {};
}
