package ms.imf.redpoint.manager;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ms.imf.redpoint.entity.Node;
import ms.imf.redpoint.entity.NodePath;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TreeStructureTest {

    private TreeStructure<Node, String> tree;

    @Before
    public void setUp() throws Exception {
        tree = new TreeStructure<>();
    }

    @Test
    public void put() {
        String data1 = "1";
        String data2 = "2";
        String data3 = "3";
        NodePath pathAbc = NodePath.instance("a", "b", "c");
        NodePath pathAbd = NodePath.instance("a", "b", "d");

        assertThat(
                tree.getMatchPathData(pathAbc.nodes()),
                is(Collections.<String>emptySet())
        );

        tree.put(data1, pathAbc.nodes());
        assertThat(
                tree.getMatchPathData(pathAbc.nodes()),
                is(Collections.singleton(data1))
        );

        tree.put(data2, pathAbc.nodes());
        assertThat(
                tree.getMatchPathData(pathAbc.nodes()),
                CoreMatchers.<Set<String>>is(new HashSet<>(Arrays.asList(data1, data2)))
        );

        tree.putMore(data3, Arrays.asList(pathAbc.nodes(), pathAbd.nodes()));
        assertThat(
                tree.getMatchPathData(pathAbc.nodes()),
                CoreMatchers.<Set<String>>is(new HashSet<>(Arrays.asList(data1, data2, data3)))
        );
        assertThat(
                tree.getMatchPathData(pathAbd.nodes()),
                CoreMatchers.<Set<String>>is(new HashSet<>(Collections.singletonList(data3)))
        );
    }

    @Test
    public void remove() {
        String data1 = "1";
        String data2 = "2";
        String data3 = "3";

        NodePath pathAb = NodePath.instance("a", "b");
        NodePath pathAbc = NodePath.instance("a", "b", "c");
        NodePath[] paths = {pathAb, pathAbc};

        for (NodePath nodePath : paths) {
            for (String data : new String[]{data1, data2, data3}) {
                tree.put(data, nodePath.nodes());
            }
        }

        for (NodePath path : paths) {
            tree.remove(data1, path.nodes());
            assertThat(
                    tree.getMatchPathData(path.nodes()),
                    CoreMatchers.<Set<String>>is(new HashSet<>(Arrays.asList(data2, data3)))
            );
        }


        tree.removeMore(data2, pathsToNodes(Arrays.asList(paths)));
        for (NodePath path : paths) {
            assertThat(
                    tree.getMatchPathData(path.nodes()),
                    CoreMatchers.<Set<String>>is(new HashSet<>(Arrays.asList(data3)))
            );
        }
    }

    @Test
    public void getMatchPathData() {
        String data1 = "1";
        String data2 = "2";
        String data3 = "3";
        NodePath pathAbc = NodePath.instance("a", "b", "c");
        NodePath pathAbd = NodePath.instance("a", "b", "d");
        NodePath[] paths = {pathAbc, pathAbd};

        for (NodePath path : paths) {
            assertThat(
                    tree.getMatchPathData(path.nodes()),
                    is(Collections.<String>emptySet())
            );

            tree.put(data1, path.nodes());
            assertThat(
                    tree.getMatchPathData(path.nodes()),
                    is(Collections.singleton(data1))
            );

            tree.put(data2, path.nodes());
            assertThat(
                    tree.getMatchPathData(path.nodes()),
                    CoreMatchers.<Set<String>>is(new HashSet<>(Arrays.asList(data1, data2)))
            );
        }

        tree.putMore(data3, pathsToNodes(Arrays.asList(paths)));

        for (NodePath path : paths) {
            assertThat(
                    tree.getMatchPathData(path.nodes()),
                    CoreMatchers.<Set<String>>is(new HashSet<>(Arrays.asList(data1, data2, data3)))
            );
        }

        for (NodePath path : new NodePath[]{
                NodePath.instance("a"),
                NodePath.instance("a", "b")
        }) {
            assertThat(
                    tree.getMatchPathData(path.nodes()),
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
            tree.put(names.toString(), path.nodes());
        }

        // 应查询无果
        assertThat(
                tree.getPathRangeAllData(Collections.<Node>emptyList()),
                is(Collections.<String>emptySet())
        );

        // 非边界
        assertThat(
                tree.getPathRangeAllData(NodePath.instance("a", "b", "c").nodes()),
                CoreMatchers.<Set<String>>is(new HashSet<>(Arrays.asList("a", "ab", "abc")))
        );
        assertThat(
                tree.getPathsRangeAllData(Arrays.asList(
                        NodePath.instance("a", "b", "c").nodes(),
                        NodePath.instance("a", "b", "e").nodes()
                )),
                CoreMatchers.<Set<String>>is(new HashSet<>(Arrays.asList("a", "ab", "abc", "abe")))
        );

        // 边界
        assertThat(
                tree.getPathRangeAllData(NodePath.instance("a", "b", "e").nodes()),
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
            tree.put(names.toString(), path.nodes());
        }

        // 应查询无果
        assertThat(
                tree.getMatchPathSubData(Collections.<Node>emptyList()),
                is(Collections.<String>emptySet())
        );

        // 非边界
        assertThat(
                tree.getMatchPathSubData(NodePath.instance("a", "b").nodes()),
                CoreMatchers.<Set<String>>is(new HashSet<>(Arrays.asList(
                        "ab", "abc", "abd", "abde"
                )))
        );
        assertThat(
                tree.getMatchPathsSubData(Arrays.asList(
                        NodePath.instance("a", "b", "c").nodes(),
                        NodePath.instance("a", "b", "d").nodes()
                )),
                CoreMatchers.<Set<String>>is(new HashSet<>(Arrays.asList(
                        "abc", "abd", "abde"
                )))
        );

        // 边界
        assertThat(
                tree.getMatchPathSubData(NodePath.instance("a", "b", "c").nodes()),
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
            tree.put(names.toString(), path.nodes());
        }
        assertThat(
                tree.getLongestPathData(),
                CoreMatchers.<Set<String>>is(new HashSet<>(Arrays.asList(
                        "abcdef",
                        "abf"
                )))
        );

        tree.remove("abcdef", NodePath.instance("a", "b", "c", "d", "e", "f").nodes());
        assertThat(
                tree.getLongestPathData(),
                CoreMatchers.<Set<String>>is(new HashSet<>(Arrays.asList(
                        "abcde",
                        "abf"
                )))
        );

        tree.remove("abcd", NodePath.instance("a", "b", "c", "d").nodes());
        tree.remove("abcde", NodePath.instance("a", "b", "c", "d", "e").nodes());
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
            tree.put(names.toString(), path.nodes());
        }
        assertThat(
                tree.getShortestPathData(),
                CoreMatchers.<Set<String>>is(new HashSet<>(Arrays.asList(
                        "ab", "ad"
                )))
        );
        
        tree.remove("ad", NodePath.instance("a", "d").nodes());
        assertThat(
                tree.getShortestPathData(),
                CoreMatchers.<Set<String>>is(new HashSet<>(Arrays.asList(
                        "ab", "ade"
                )))
        );
    }

    private Iterable<? extends Iterable<Node>> pathsToNodes(Iterable<NodePath> paths) {
        List<List<Node>> nodes = new ArrayList<>();

        for (NodePath path : paths) {
            nodes.add(path.nodes());
        }

        return nodes;
    }
}