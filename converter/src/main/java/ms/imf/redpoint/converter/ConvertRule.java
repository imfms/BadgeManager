package ms.imf.redpoint.converter;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 节点转换规则
 */
public class ConvertRule {

    /**
     * 节点名
     */
    @SerializedName("name")
    public String name;

    /**
     * 支持参数列表
     */
    @SerializedName("args")
    public List<String> args;

    /**
     * 子节点列表
     */
    @SerializedName("sub")
    public List<ConvertRule> sub;

    /**
     * 节点转换目标节点列表
     */
    @SerializedName("convertTo")
    public List<ConvertTo> convertTo;

    /**
     * 节点转换目标节点
     */
    public static class ConvertTo {
        /**
         * 转换目标节点名
         */
        @SerializedName("name")
        public String name;

        /**
         * 转换目标参数列表
         */
        @SerializedName("args")
        public List<Arg> args;
    }

    /**
     * 节点转换目标参数
     */
    public static class Arg {

        /**
         * 目标参数名
         */
        @SerializedName("hisArg")
        public String hisArg;

        /**
         * 目标参数值，用于指定固定值类型参数
         * <p>
         * 与 {@link #refValue} 必须且只能出现一个
         */
        @SerializedName("value")
        public String value;

        /**
         * 目标参数引用值，用于引用源节点链上某节点的参数
         * 与 {@link #value} 必须且只能出现一个
         */
        @SerializedName("refValue")
        public RefValue refValue;

        /**
         * 目标参数引用值信息，用于引用源节点列表上某节点的参数
         * 可以达到让让目标节点参数的值使用源节点列表中的某节点中某参数的值
         */
        public static class RefValue {

            /**
             * 引用源节点列表中的第几个节点？节点从0开始
             * 例如源节点列表： home, list, item(itemId=1, type=vip), detail
             * 如果要引用节点 'item(itemId=1)'，则 myLevel 应指定为 '2'
             */
            @SerializedName("myLevel")
            public Integer myLevel;

            /**
             * 引用节点的哪个参数？
             * 例如源节点列表：home, list, item(itemId=1, type=vip), detail
             * 如果要引用 'item' 的 'itemId' 参数，则 myArg 应指定为 'itemId'
             */
            @SerializedName("myArg")
            public String myArg;
        }

    }

}