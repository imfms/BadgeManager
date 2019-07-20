package ms.imf.redpoint.compiler.plugin;

import java.util.List;

import javax.lang.model.element.TypeElement;

/**
 * 节点容器实体，类声明所支持节点时的基本单元
 * 用于对结点容器注解的信息进行解析加工封装为更接近使用的类型
 * 例如节点对子节点的引用由Class转换为其实际解析后的实体
 */
public class NodeContainerEntity {

    /**
     * 包含节点
     */
    public List<Node> nodes;

    /**
     * 节点容器宿主，声明节点容器宿主的类
     */
    public TypeElement host;

    /**
     * 节点信息
     */
    public static class Node {
        /**
         * 节点名
         */
        public String name;
        /**
         * 支持参数
         */
        public List<Arg> args;
        /**
         * 包含的子节点
         * 与{@link #subRef}只会存在一项
         */
        public List<Node> sub;
        /**
         * 指向的子节点实体
         * 与{@link #sub}只会存在一项
         */
        public NodeContainerEntity subRef;

        /**
         * 节点参数信息
         */
        public static class Arg {
            /**
             * 参数名
             */
            public String name;
            /**
             * 参数支持的值，如果为null或empty则代表对支持值没有限制
             */
            public List<String> valueLimits;
        }
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
