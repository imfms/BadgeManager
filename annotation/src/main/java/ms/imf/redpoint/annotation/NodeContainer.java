package ms.imf.redpoint.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface NodeContainer {

    SubNode[] value() default {};

    /**
     * Node {
     * <p>
     * | String name; {@link SubNode#value()}
     * <p>
     * | Arg[] args; {@link SubNode#args()}}
     * <p>
     * | Node[] subNodes; {@link SubNode#subNodes()}}
     * <p>
     * | String subNodeRef; {@link SubNode#subNodeContainerRef()}
     * <p>
     * }
     * <p>
     * Arg {
     * <p>
     * | String name; {@link Arg#value()}}
     * <p>
     * | String[] limits; {@link Arg#valueLimits()}}
     * <p>
     * }
     * <p>
     */
    String[] nodesJson() default {};

    /**
     * {@link #nodesJson()}中subNodeRef中的引用值的映射
     * 例如 "subNodeRef": "abc"
     * 则在nodesJsonRefClassMapper中需要存在abc对应的subNodeRefClass: @Mapper(key = "abc", value = Xxx.class)
     */
    Mapper[] nodesJsonTreeRefMapper() default {};

    /**
     * node container's type
     */
    ContainerType type() default ContainerType.UNLIMIT;
}