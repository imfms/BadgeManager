package ms.imf.redpoint.converter;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ConvertRule {

    @SerializedName("name")
    public String name;
    @SerializedName("args")
    public List<String> args;
    @SerializedName("sub")
    public List<ConvertRule> sub;
    @SerializedName("convertTo")
    public List<ConvertTo> convertTo;

    public static class ConvertTo {
        @SerializedName("name")
        public String name;
        @SerializedName("args")
        public List<Arg> args;
    }

    public static class Arg {
        @SerializedName("hisArg")
        public String hisArg;
        /**
         * 与 {@link #refValue} 只能出现一个
         */
        @SerializedName("value")
        public String value;
        /**
         * 与 {@link #value} 只能出现一个
         */
        @SerializedName("refValue")
        public RefValue refValue;

        public static class RefValue {
            @SerializedName("myLevel")
            public Integer myLevel;
            @SerializedName("myArg")
            public String myArg;
        }

    }

}