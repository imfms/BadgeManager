package ms.imf.redpoint.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Path {

    SubNode[] value() default {};

    /**
     * Node {
     * <p>
     * | String type; {@link SubNode#type()}
     * <p>
     * | Arg[] args; {@link SubNode#args()}}
     * <p>
     * | Node[] subNodes; {@link SubNode#subNodes()}}
     * <p>
     * | String subNodeRef; {@link SubNode#subRef()}
     * <p>
     * }
     * <p>
     * Arg {
     * <p>
     * | String name; {@link NodeArg#value()}}
     * <p>
     * | String[] limits; {@link NodeArg#valueLimits()}}
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
    Mapper[] nodesJsonRefClassMapper() default {};

    /**
     * node's type
     */
    NodeType type() default NodeType.UNLIMIT;
}