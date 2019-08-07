package ms.imf.badge.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明一个节点
 *
 * 由于注解类型不支持嵌套，所以子节点{@link #subNodes()}使用独立注解类型层层上下级引用的方式进行规避，已经努力声明了足够多的子节点类型，可以满足大多数使用场景。
 * 如果仍然不够使用可使用JSON字符串声明节点的方式{@link NodeContainer#nodeJson()}
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
