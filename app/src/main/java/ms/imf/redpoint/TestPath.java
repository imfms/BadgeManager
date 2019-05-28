package ms.imf.redpoint;

import ms.imf.redpoint.annotation.Node;
import ms.imf.redpoint.annotation.Path;
import ms.imf.redpoint.annotation.SubNode;

/**
 * TestPath
 *
 * @author f_ms
 * @date 19-5-16
 */
@Path({
        @Node(type = "home"),
        @Node(type = "mine", args = {"uid"}, subNodes = {
                @SubNode(type = "type")
        })
})
public class TestPath {
    public static void main(String[] args) {
        System.out.println(TestPath_Path.class);
    }
}
