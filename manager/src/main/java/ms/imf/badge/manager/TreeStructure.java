package ms.imf.badge.manager;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * 树结构，每个树节点可携带数个数据
 * 提供树结构的构建和路径-数据的各项查询功能的封装
 *
 * @author f_ms
 * @date 19-7-28
 */
class TreeStructure<Node, Data> {

    private class DataNode {
        final Set<Data> dataSet = new HashSet<>();
        final Map<Node, DataNode> sub = new HashMap<>();
    }

    private final Map<Node, DataNode> tree = new HashMap<>();

    /**
     * 插入数据到指定路径下
     */
    void put(Data data, Iterable<Node> path) {
        putMore(data, Collections.singleton(path));
    }

    /**
     * @see #put(Data, Iterable)
     */
    void putMore(Data data, Iterable<? extends Iterable<Node>> paths) {
        for (Iterable<Node> path : paths) {

            Map<Node, DataNode> currentNode = tree;

            Iterator<Node> iterator = path.iterator();
            while (iterator.hasNext()) {
                Node node = iterator.next();

                DataNode dataNode = currentNode.get(node);
                if (dataNode == null) {
                    dataNode = new DataNode();
                    currentNode.put(node, dataNode);
                }

                currentNode = dataNode.sub;

                if (!iterator.hasNext()) {
                    dataNode.dataSet.add(data);
                }
            }

        }
    }

    /**
     * 从指定路径移除数据
     */
    void remove(Data data, Iterable<Node> path) {
       removeMore(data, Collections.singleton(path));
    }

    /**
     * @see #remove(Data, Iterable)
     */
    void removeMore(Data data, Iterable<? extends Iterable<Node>> paths) {
        for (Iterable<Node> path : paths) {

            Map<Node, DataNode> currentNode = tree;

            Iterator<Node> iterator = path.iterator();
            while (iterator.hasNext()) {
                Node node = iterator.next();
                DataNode dataNode = currentNode.get(node);
                if (dataNode == null) {
                    break;
                }

                if (iterator.hasNext()) {
                    currentNode = dataNode.sub;
                    continue;
                }

                dataNode.dataSet.remove(data);
                removeInvalidNode(path);
                break;
            }

        }
    }

    /**
     * 清空所有数据
     */
    void clear() {
        tree.clear();
    }

    /**
     * 获取完全匹配指定路径节点下的数据
     *
     * 例如有节点: 'a', 'a>b', 'a>b>c'
     * 指定路径为 'a>b>c' 的情况下只会返回节点 'a>b>c' 下的数据
     */
    Set<Data> getMatchPathData(Iterable<Node> path) {
        DataNode dataNode = getPathLastDataNode(path);
        if (dataNode == null) {
            return Collections.emptySet();
        }

        return new HashSet<>(
                dataNode.dataSet
        );
    }

    /**
     * 获取指定路径节点范围内所有数据
     * <p>
     * 例如有节点: 'a','a>b','a>b>c','a>b>c>d'
     * 则通过路径 'a>b>c'可查询到这些节点下的数据: 'a','a>b','a>b>c'
     */
    Set<Data> getPathRangeAllData(Iterable<Node> path) {
        return getPathsRangeAllData(Collections.singleton(path));
    }

    /**
     * @see #getPathRangeAllData(Iterable)
     */
    Set<Data> getPathsRangeAllData(Iterable<? extends Iterable<Node>> paths) {
        final Set<Data> result = new HashSet<>();

        for (Iterable<Node> path : paths) {

            Map<Node, DataNode> currentDataNode = tree;

            for (Node node : path) {
                DataNode dataNode = currentDataNode.get(node);
                if (dataNode == null) {
                    break;
                }

                currentDataNode = dataNode.sub;

                result.addAll(dataNode.dataSet);
            }

        }

        return result;
    }

    /**
     * 获取指定路径节点及其子节点内所有数据
     * <p>
     * 例如有节点: 'a','a>b','a>b>c','a>b>c>d'
     * 则通过路径 'a>b>c' 可查询到这些节点下的数据: 'a>b>c', 'a>b>c>d'
     */
    Set<Data> getMatchPathSubData(Iterable<Node> path) {
        return getMatchPathsSubData(Collections.singleton(path));
    }

    /**
     * @see #getMatchPathSubData(Iterable)
     */
    Set<Data> getMatchPathsSubData(Iterable<? extends Iterable<Node>> paths) {
        Set<Data> result = new HashSet<>();

        for (Iterable<Node> path : paths) {

            DataNode matchDataNode = getPathLastDataNode(path);
            if (matchDataNode == null) {
                continue;
            }

            addDataNodeToContainer(matchDataNode, result);
        }

        return result;
    }

    /**
     * 获取节点树中拥有最长节点的数据
     * 例如有些数据在右侧路径: a>b, a>b>c, a>d, a>d>e
     * 则返回这些路径下的数据: 'a>b>c', 'a>d>e'
     */
    Set<Data> getLongestPathData() {
        Set<Data> result = new HashSet<>();
        for (Map.Entry<Node, DataNode> entry : tree.entrySet()) {
            addLongestPathDataToContainer(entry.getValue(), result);
        }
        return result;
    }

    /**
     * 获取节点树中拥有最短节点的数据
     * 例如有些数据在右侧路径: a>b, a>b>c, a>d, a>d>e
     * 则返回这些路径下的数据: 'a>b', 'a>d'
     */
    Set<Data> getShortestPathData() {
        HashSet<Data> result = new HashSet<>();
        for (Map.Entry<Node, DataNode> entry : tree.entrySet()) {
            addShortestPathDataToContainer(entry.getValue(), result);
        }
        return result;
    }

    private DataNode getPathLastDataNode(Iterable<Node> path) {
        DataNode matchDataNode = null;
        Map<Node, DataNode> currentNode = tree;

        Iterator<Node> nodeIterator = path.iterator();
        while (nodeIterator.hasNext()) {
            Node node = nodeIterator.next();

            DataNode dataNode = currentNode.get(node);
            if (dataNode == null) {
                break;
            }

            currentNode = dataNode.sub;

            if (!nodeIterator.hasNext()) {
                matchDataNode = dataNode;
            }
        }

        return matchDataNode;
    }

    private void addDataNodeToContainer(DataNode dataNode, Collection<Data> resultContainer) {
        resultContainer.addAll(dataNode.dataSet);
        for (Map.Entry<Node, DataNode> entry : dataNode.sub.entrySet()) {
            addDataNodeToContainer(entry.getValue(), resultContainer);
        }
    }

    private void addLongestPathDataToContainer(DataNode dataNode, Set<Data> resultContainer) {
        if (dataNode.sub.isEmpty()) {
            resultContainer.addAll(dataNode.dataSet);
        } else {
            for (Map.Entry<Node, DataNode> entry : dataNode.sub.entrySet()) {
                addLongestPathDataToContainer(entry.getValue(), resultContainer);
            }
        }
    }
    private void addShortestPathDataToContainer(DataNode dataNode, Collection<Data> resultContainer) {
        if (!dataNode.dataSet.isEmpty()) {
            resultContainer.addAll(dataNode.dataSet);
            return;
        }
        for (Map.Entry<Node, DataNode> entry : dataNode.sub.entrySet()) {
            addShortestPathDataToContainer(entry.getValue(), resultContainer);
        }
    }
    private void removeInvalidNode(Iterable<Node> path) {
        Map<Node, DataNode> currentNode = tree;

        for (Node node : path) {
            DataNode dataNode = currentNode.get(node);
            if (dataNode == null) {
                break;
            }

            if (!hasData(dataNode)) {
                currentNode.remove(node);
                break;
            }

            currentNode = dataNode.sub;
        }
    }
    private boolean hasData(DataNode dataNode) {
        if (!dataNode.sub.isEmpty()) {
            for (Map.Entry<Node, DataNode> entry : dataNode.sub.entrySet()) {
                if (hasData(entry.getValue())) {
                    return true;
                }
            }
        }
        if (!dataNode.dataSet.isEmpty()) {
            return true;
        }
        return false;
    }
}
