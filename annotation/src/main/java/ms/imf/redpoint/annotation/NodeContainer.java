package ms.imf.redpoint.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 节点容器注解，用于标注一个类所对应的各节点
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface NodeContainer {

    /**
     * 声明包含的节点
     */
    SubNode[] value() default {};

    /**
     * 以 JSON 方式声明包含的节点
     * <p>由于注解类型不支持嵌套，虽然已经努力通过创建不同的SubNode[x]注解类型来规避此限制，但其支持的层级仍然是有限的，所以对于节点子层级过深的需求提供了以 JSON 字符串来表达的方式
     * <p>
     * <p>
     * JSON node format:
     * <p>
     * <pre>
     * Node {
     *   String name; // {@link SubNode#value()}
     *   Arg[] args; // {@link SubNode#args()}}
     *   Node[] subNodes; // {@link SubNode#subNodes()}}
     *   String subNodeRef; // {@link SubNode#subNodeContainerRef()}
     * }
     * Arg {
     *   String name; // {@link Arg#value()}}
     *   String[] limits; // {@link Arg#valueLimits()}}
     * }
     * </pre>
     */
    String[] nodeJson() default {};

    /**
     *
     * {@link #nodeJson()}.subNodeRef对{@link Mapper#key()}别名引用的用于查询{@link SubNode#subNodeContainerRef()}的字典
     * <p>
     * 例如 {"subNodeRef": "abc"}
     * 则注解处理器将通过字典中查找key==abc时对应的{@link SubNode#subNodeContainerRef}用于建立子节点关系
     */
    Mapper[] nodeJsonRefContainerMapper() default {};

    /**
     * 节点容器的类型
     * <p>
     * 当最终整体节点关系链接完毕后，注解处理器会根据此类型值对节点最终的类型进行校验，不符合的情况下将抛出错误信息
     */
    ContainerType type() default ContainerType.UNLIMIT;
}