package ms.imf.redpoint.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 节点声明注解
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.ANNOTATION_TYPE)
public @interface SubNode {

    /**
     * 节点名
     */
    String value();

    /**
     * 节点支持的参数
     */
    Arg[] args() default {};

    /**
     * 子节点, 与 {@link #subNodeContainerRef()} 只能指定一项
     */
    SubNode2[] subNodes() default {};

    /**
     * 子节点：被{@link NodeContainer}所标注的类Class，它将代表着自身被标注的{@link NodeContainer}所表达的子节点树
     * <p>
     * 与 {@link #subNodes()} 只能指定一项
     */
    Class subNodeContainerRef() default Void.class;
}
