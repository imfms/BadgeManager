package ms.imf.redpoint.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.ANNOTATION_TYPE)
public @interface SubNode {

    /**
     * name
     */
    String value();

    /**
     * args
     */
    Arg[] args() default {};

    /**
     * 子节点列表, 与 {@link #subNodeContainerRef()} 只能指定一项
     */
    SubNode2[] subNodes() default {};

    /**
     * 子节点：被{@link NodeContainer}所标注的类的Class
     * <p>
     * 如果当前节点是最后一个节点的情况下(没有子节点)可使用{@link Void}.class
     * <p>
     * 与 {@link #subNodes()} 只能指定一项
     */
    Class subNodeContainerRef() default Void.class;
}
