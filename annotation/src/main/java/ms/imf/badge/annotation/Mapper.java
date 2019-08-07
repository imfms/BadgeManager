package ms.imf.badge.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link NodeContainer#nodeJson()}中以{@link SubNode#subNodeContainerRef()}方式对子节点树进行链接的别名声明
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Mapper {

    /**
     * 别名
     *
     * @see #value()
     * @see NodeContainer#nodeJson()
     */
    String key();

    /**
     * {@link #key}指向的被{@link NodeContainer}标注的类，该类将代表着它自身{@link NodeContainer}所表达的子节点树
     * @see SubNode#subNodeContainerRef()
     */
    Class value();
}

