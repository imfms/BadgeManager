package ms.imf.redpoint;

import ms.imf.redpoint.annotation.Arg;
import ms.imf.redpoint.annotation.NodeContainer;
import ms.imf.redpoint.annotation.SubNode;
import ms.imf.redpoint.annotation.SubNode2;

/**
 * TestPath
 *
 * @author f_ms
 * @date 19-5-16
 */
@NodeContainer(
        value = {
                @SubNode(value = "home"),
                @SubNode(value = "mine", args = @Arg("uid"), subNodes = {
                        @SubNode2(value = "name", args = @Arg(value = "arg1", valueLimits = {"a", "b", "c"}))
                })
        },
        nodeJson = {

                "{\"name\":\"nodeJsonType1\",\"args\":[{\"name\":\"nodeJsonType1Arg1\"},{\"name\":\"nodeJsonType1Arg2\",\"limits\":[\"limit1\", \"limit2\",\"limit3\"]}]}",
                "{\"name\":\"nodeJsonType2\",\"args\":[{\"name\":\"nodeJsonType2Arg1\"},{\"name\":\"nodeJsonType2Arg2\",\"limits\":[\"limit1\",\"limit2\",\"limit3\"]}]}"
        }
)
public class TestPath2 {
    public static void main(String[] args) {
        System.out.println(TestPath_Node.class);
    }
}
