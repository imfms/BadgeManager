package ms.imf.redpoint.converter;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ConvertRule {

    @SerializedName("type")
    public String type;
    @SerializedName("args")
    public List<String> args;
    @SerializedName("sub")
    public List<ConvertRule> sub;
    @SerializedName("convertTo")
    public List<ConvertTo> convertTo;

    public static class ConvertTo {
        @SerializedName("type")
        public String type;
        @SerializedName("args")
        public List<Arg> args;
    }

    public static class Arg {
        @SerializedName("myLevel")
        public Integer myLevel;
        @SerializedName("myArg")
        public String myArg;
        @SerializedName("hisArg")
        public String hisArg;
    }

}