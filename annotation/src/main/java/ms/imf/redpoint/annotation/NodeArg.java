package ms.imf.redpoint.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.ANNOTATION_TYPE)
public @interface NodeArg {
    /**
     * arg name
     */
    String value();

    /**
     * limits of arg values
     */
    String[] valueLimits() default {};
}
