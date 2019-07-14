package ms.imf.redpoint.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.ANNOTATION_TYPE)
public @interface SubNode7 {
    /**
     * @see SubNode#value()
     */
    String value();

    /**
     * @see SubNode#args()
     */
    Arg[] args() default {};

    /**
     * @see SubNode#subNodes()
     */
    SubNode8[] subNodes() default {};

    /**
     * @see SubNode#subNodeContainerRef()
     */
    Class subNodeContainerRef() default Void.class;
}
