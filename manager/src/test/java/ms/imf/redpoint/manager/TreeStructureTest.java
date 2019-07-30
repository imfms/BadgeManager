package ms.imf.redpoint.manager;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import ms.imf.redpoint.entity.Node;
import ms.imf.redpoint.entity.NodePath;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TreeStructureTest {

    private TreeStructure<String> tree;

    @Before
    public void setUp() throws Exception {
        tree = new TreeStructure<>();
    }

    @Test
    public void put() {
        String data1 = "1";
        String data2 = "2";
        String data3 = "3";
        NodePath path1 = NodePath.instance("a", "b", "c");
        NodePath path2 = NodePath.instance("a", "b", "d");

        assertThat(
                tree.getMatchPathData(path1),
                is(Collections.<String>emptySet())
        );

        tree.put(data1, path1);
        assertThat(
                tree.getMatchPathData(path1),
                is(Collections.singleton(data1))
        );

        tree.put(data2, path1);
        assertThat(
                tree.getMatchPathData(path1),
                CoreMatchers.<Set<String>>is(new HashSet<>(Arrays.asList(data1, data2)))
        );

        tree.put(data3, new HashSet<>(Arrays.asList(path1, path2)));
        assertThat(
                tree.getMatchPathData(path1),
                CoreMatchers.<Set<String>>is(new HashSet<>(Arrays.asList(data1, data2, data3)))
        );
        assertThat(
                tree.getMatchPathData(path2),
                CoreMatchers.<Set<String>>is(new HashSet<>(Collections.singletonList(data3)))
        );
    }

    @Test
    public void remove() {
        String data1 = "1";
        String data2 = "2";
        String data3 = "3";

        NodePath path1 = NodePath.instance("a", "b");
        NodePath path2 = NodePath.instance("a", "b", "c");
        NodePath[] paths = {path1, path2};

        for (NodePath nodePath : paths) {
            for (String data : new String[]{data1, data2, data3}) {
                tree.put(data, nodePath);
            }
        }

        for (NodePath path : paths) {
            tree.remove(data1, path);
            assertThat(
                    tree.getMatchPathData(path),
                    CoreMatchers.<Set<String>>is(new HashSet<>(Arrays.asList(data2, data3)))
            );
        }

        tree.remove(data2, new HashSet<>(Arrays.asList(paths)));
        for (NodePath path : paths) {
            assertThat(
                    tree.getMatchPathData(path),
                    CoreMatchers.<Set<String>>is(new HashSet<>(Arrays.asList(data3)))
            );
        }
    }

    @Test
    public void getMatchPathData() {
        String data1 = "1";
        String data2 = "2";
        String data3 = "3";
        NodePath path1 = NodePath.instance("a", "b", "c");
        NodePath path2 = NodePath.instance("a", "b", "d");
        NodePath[] paths = {path1, path2};

        for (NodePath path : paths) {
            assertThat(
                    tree.getMatchPathData(path),
                    is(Collections.<String>emptySet())
            );

            tree.put(data1, path);
            assertThat(
                    tree.getMatchPathData(path),
                    is(Collections.singleton(data1))
            );

            tree.put(data2, path);
            assertThat(
                    tree.getMatchPathData(path),
                    CoreMatchers.<Set<String>>is(new HashSet<>(Arrays.asList(data1, data2)))
            );
        }

        tree.put(data3, new HashSet<>(Arrays.asList(paths)));

        for (NodePath path : paths) {
            assertThat(
                    tree.getMatchPathData(path),
                    CoreMatchers.<Set<String>>is(new HashSet<>(Arrays.asList(data1, data2, data3)))
            );
        }

        for (NodePath path : new NodePath[]{
                NodePath.instance("a"),
                NodePath.instance("a", "b")
        }) {
            assertThat(
                    tree.getMatchPathData(path),
                    is(Collections.<String>emptySet())
            );
        }
    }

    @Test
    public void getPathRangeAllData() {
        NodePath[] nodePaths = {
                NodePath.instance("a"),
                NodePath.instance("a", "b"),
                NodePath.instance("a", "b", "c"),
                NodePath.instance("a", "b", "c", "d"),
                NodePath.instance("a", "b", "e"),
        };
        for (NodePath path : nodePaths) {
            StringBuilder names = new StringBuilder();
            for (Node node : path.nodes()) {
                names.append(node.name);
            }
            tree.put(names.toString(), path);
        }

        // 应查询无果
        assertThat(
                tree.getPathRangeAllData(NodePath.instance(Collections.<Node>emptyList())),
                is(Collections.<String>emptySet())
        );

        // 非边界
        assertThat(
                tree.getPathRangeAllData(NodePath.instance("a", "b", "c")),
                CoreMatchers.<Set<String>>is(new HashSet<>(Arrays.asList("a", "ab", "abc")))
        );
        assertThat(
                tree.getPathRangeAllData(new HashSet<>(Arrays.asList(
                        NodePath.instance("a", "b", "c"),
                        NodePath.instance("a", "b", "e")
                ))),
                CoreMatchers.<Set<String>>is(new HashSet<>(Arrays.asList("a", "ab", "abc", "abe")))
        );

        // 边界
        assertThat(
                tree.getPathRangeAllData(NodePath.instance("a", "b", "e")),
                CoreMatchers.<Set<String>>is(new HashSet<>(Arrays.asList("a", "ab", "abe")))
        );
    }

    @Test
    public void getMatchPathSubData() {
        NodePath[] nodePaths = {
                NodePath.instance("a"),
                NodePath.instance("a", "b"),
                NodePath.instance("a", "b", "c"),
                NodePath.instance("a", "b", "d"),
                NodePath.instance("a", "b", "d", "e"),
                NodePath.instance("a", "f", "g"),
        };
        for (NodePath path : nodePaths) {
            StringBuilder names = new StringBuilder();
            for (Node node : path.nodes()) {
                names.append(node.name);
            }
            tree.put(names.toString(), path);
        }

        // 应查询无果
        assertThat(
                tree.getMatchPathSubData(NodePath.instance(Collections.<Node>emptyList())),
                is(Collections.<String>emptySet())
        );

        // 非边界
        assertThat(
                tree.getMatchPathSubData(NodePath.instance("a", "b")),
                CoreMatchers.<Set<String>>is(new HashSet<>(Arrays.asList(
                        "ab", "abc", "abd", "abde"
                )))
        );
        assertThat(
                tree.getMatchPathSubData(new HashSet<>(Arrays.asList(
                        NodePath.instance("a", "b", "c"),
                        NodePath.instance("a", "b", "d")
                ))),
                CoreMatchers.<Set<String>>is(new HashSet<>(Arrays.asList(
                        "abc", "abd", "abde"
                )))
        );

        // 边界
        assertThat(
                tree.getMatchPathSubData(NodePath.instance("a", "b", "c")),
                CoreMatchers.<Set<String>>is(new HashSet<>(Arrays.asList(
                        "abc"
                )))
        );
    }

    @Test
    public void getLongestPathData() {
        NodePath[] nodePaths = {
                NodePath.instance("a"),
                NodePath.instance("a", "b"),
                NodePath.instance("a", "b", "c"),
                NodePath.instance("a", "b", "c", "d"),
                NodePath.instance("a", "b", "c", "d", "e"),
                NodePath.instance("a", "b", "c", "d", "e", "f"),
                NodePath.instance("a", "b", "f"),
        };

        assertThat(
                tree.getLongestPathData(),
                is(Collections.<String>emptySet())
        );

        for (NodePath path : nodePaths) {
            StringBuilder names = new StringBuilder();
            for (Node node : path.nodes()) {
                names.append(node.name);
            }
            tree.put(names.toString(), path);
        }
        assertThat(
                tree.getLongestPathData(),
                CoreMatchers.<Set<String>>is(new HashSet<>(Arrays.asList(
                        "abcdef",
                        "abf"
                )))
        );

        tree.remove("abcdef", NodePath.instance("a", "b", "c", "d", "e", "f"));
        assertThat(
                tree.getLongestPathData(),
                CoreMatchers.<Set<String>>is(new HashSet<>(Arrays.asList(
                        "abcde",
                        "abf"
                )))
        );

        tree.remove("abcd", NodePath.instance("a", "b", "c", "d"));
        tree.remove("abcde", NodePath.instance("a", "b", "c", "d", "e"));
        assertThat(
                tree.getLongestPathData(),
                CoreMatchers.<Set<String>>is(new HashSet<>(Arrays.asList(
                        "abc",
                        "abf"
                )))
        );
    }

    @Test
    public void getShortestPathData() {
        NodePath[] nodePaths = {
                NodePath.instance("a", "b"),
                NodePath.instance("a", "b", "c"),
                NodePath.instance("a", "d"),
                NodePath.instance("a", "d", "e"),
        };

        assertThat(
                tree.getShortestPathData(),
                is(Collections.<String>emptySet())
        );

        for (NodePath path : nodePaths) {
            StringBuilder names = new StringBuilder();
            for (Node node : path.nodes()) {
                names.append(node.name);
            }
            tree.put(names.toString(), path);
        }
        assertThat(
                tree.getShortestPathData(),
                CoreMatchers.<Set<String>>is(new HashSet<>(Arrays.asList(
                        "ab", "ad"
                )))
        );
        
        tree.remove("ad", NodePath.instance("a", "d"));
        assertThat(
                tree.getShortestPathData(),
                CoreMatchers.<Set<String>>is(new HashSet<>(Arrays.asList(
                        "ab", "ade"
                )))
        );
    }
}