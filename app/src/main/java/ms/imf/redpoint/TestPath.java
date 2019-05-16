package ms.imf.redpoint;

import ms.imf.redpoint.annotation.Node;
import ms.imf.redpoint.annotation.Path;

/**
 * TestPath
 *
 * @author f_ms
 * @date 19-5-16
 */
@Path({
        @Node(type = "home", subRef = Void.class),
        @Node(type = "mine", args = {"uid"}, subRef = Void.class)
})
public class TestPath {
    public static void main(String[] args) {
        System.out.println(TestPath_Path.class);
    }
}
