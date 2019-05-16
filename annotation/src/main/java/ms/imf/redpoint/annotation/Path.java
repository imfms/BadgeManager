package ms.imf.redpoint.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Path {
    Node[] value() default {};

    /**
     * Node {
     * <p>
     * | String type; {@link Node#type()}
     * <p>
     * | String[] args; {@link Node#args()}}
     * <p>
     * | Node[] subNodes;
     * <p>
     * | String subNodeRef; {@link Node#subRef()}
     * <p>
     * }
     */
    String[] nodesJson() default {};

    /**
     * {@link #nodesJson()}中subNodeRef中的引用值的映射
     * 例如 "subNodeRef": "abc"
     * 则在nodesJsonRefClassMapper中需要存在abc对应的subNodeRefClass: @Mapper(key = "abc", value = Xxx.class)
     */
    Mapper[] nodesJsonRefClassMapper() default {};
}