package ms.imf.redpoint.converter;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class NodeSchema {
    @SerializedName("type")
    public String type;
    @SerializedName("args")
    public List<String> args;
    @SerializedName("sub")
    public List<NodeSchema> sub;
}