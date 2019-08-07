package ms.imf.badge.manager;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ms.imf.badge.entity.Node;
import ms.imf.badge.entity.NodePath;

/**
 * 以节点树方式管理{@link BadgeWidget}的{@link BadgeManager}
 * <p>
 * 通过所有{@link BadgeWidget}的支持路径生成一颗节点树，并将其引用到其支持的对应节点下，
 * 从而达到通过节点快速匹配到对应的{@link BadgeWidget}的目的，并通过此思想的优势来提升{@link BadgeManager}内各项匹配任务的效率
 * <p>
 *
 * @author f_ms
 * @date 19-07-26
 */
public class TreeModeBadgeManager<RemindType extends Remind> extends BadgeManager<RemindType> {

    private final Map<BadgeWidget<RemindType>, Set<NodePath>> attachedWidgets = new HashMap<>();
    private final TreeStructure<Node, BadgeWidget<RemindType>> widgetsTree = new ConcurrentSafeTreeStructure<>();

    public TreeModeBadgeManager(RemindRepo<RemindType> repo) {
        super(repo);
    }

    @Override
    public void attachWidget(BadgeWidget<RemindType> badgeWidget) {
        if (badgeWidget == null) { return; }

        if (widgetAttached(badgeWidget)) {
            notifyWidgetChanged(badgeWidget);
            return;
        }

        putBadgeWidgetPath(badgeWidget, badgeWidget.getPaths());

        notifyBadgeWidgetDataChanged(badgeWidget);
    }

    @Override
    public void detachWidget(BadgeWidget<RemindType> badgeWidget) {
        if (badgeWidget == null) { return; }
        if (!widgetAttached(badgeWidget)) { return; }

        removeWidgetPath(
                badgeWidget,
                getWidgetPath(badgeWidget)
        );
    }

    @Override
    public boolean widgetAttached(BadgeWidget<RemindType> badgeWidget) {
        return attachedWidgets.containsKey(badgeWidget);
    }

    @Override
    public void notifyWidgetChanged(BadgeWidget<RemindType> badgeWidget) {
        if (!widgetAttached(badgeWidget)) { return; }

        Set<NodePath> acceptPaths = badgeWidget.getPaths();
        Set<NodePath> lastAcceptPaths = getWidgetPath(badgeWidget);

        Set<NodePath> addPaths = new HashSet<>(acceptPaths);
        addPaths.removeAll(lastAcceptPaths);

        Set<NodePath> removePaths = new HashSet<>(lastAcceptPaths);
        removePaths.removeAll(acceptPaths);

        putBadgeWidgetPath(badgeWidget, addPaths, removePaths);

        if (!addPaths.isEmpty()
                || !removePaths.isEmpty()) {
            notifyBadgeWidgetDataChanged(badgeWidget);
        }
    }

    @Override
    public void happenedWidget(BadgeWidget<RemindType> badgeWidget) {
        if (!widgetAttached(badgeWidget)) { return; }

        Set<NodePath> paths = getWidgetPath(badgeWidget);

        if (remindRepo().removeMatchPathReminds(paths) <= 0) {
            return;
        }

        Set<BadgeWidget<RemindType>> needChangedWidgets = widgetsTree.getPathsRangeAllData(pathsToNodes(paths));
        notifyWidgetsDataChanged(needChangedWidgets);
    }

    @Override
    public void happenedWidgetWithSubPath(BadgeWidget<RemindType> badgeWidget) {
        if (!widgetAttached(badgeWidget)) { return; }

        Set<NodePath> paths = getWidgetPath(badgeWidget);

        if (remindRepo().removeMatchPathSubReminds(paths) <= 0) {
            return;
        }

        /*
        变更涉及widget支持路径节点范围内节点和路径根节点后整个树上的所有widget
         */
        Set<BadgeWidget<RemindType>> needChangedWidgets = new HashSet<>(
                widgetsTree.getPathsRangeAllData(pathsToNodes(paths))
        );
        needChangedWidgets.addAll(
                widgetsTree.getMatchPathsSubData(pathsToNodes(paths))
        );
        notifyWidgetsDataChanged(needChangedWidgets);
    }

    @Override
    public void notifyRemindDataChanged() {
        notifyWidgetsDataChanged(attachedWidgets.keySet());
    }

    @Override
    public void notifyRemindDataChanged(Iterable<? extends RemindType> changedReminds) {

        /*
        取所有变更消息支持路径中的最长路径，获取对应的widget刷新其数据
          */

        TreeStructure<Node, NodePath> remindTree = new TreeStructure<>();
        for (RemindType remind : changedReminds) {
            remindTree.put(remind.path(), remind.path().nodes());
        }
        Set<NodePath> longestPaths = remindTree.getLongestPathData();

        notifyWidgetsDataChanged(
                widgetsTree.getPathsRangeAllData(pathsToNodes(longestPaths))
        );
    }

    private void notifyBadgeWidgetDataChanged(BadgeWidget<RemindType> badgeWidget) {
        notifyWidgetsDataChanged(Collections.singleton(badgeWidget));
    }
    private void notifyWidgetsDataChanged(Set<BadgeWidget<RemindType>> badgeWidgets) {
        if (badgeWidgets.isEmpty()) { return; }

        /*
        widgets支持的路径节点大多存在重合部分，全部交给repo查询会增大开销
        例如路径a>b查询出的消息是包含a>b>c查询出的消息的，这时候只需要查询出a>b就可以了
        所以取所有支持路径中的最短重合路径获取消息，然后生成消息树为widget分发消息
         */

        // 对widgets支持路径去重
        Set<NodePath> allPaths = getWidgetsAllPaths(badgeWidgets);
        if (allPaths.isEmpty()) {
            for (BadgeWidget<RemindType> badgeWidget : badgeWidgets) {
                badgeWidget.showReminds(Collections.<RemindType>emptyList());
            }
            return;
        }

        // 获取所有路径中的最短路径, 用于减少重合路径部分的无用查询, 例如有: a>b, a>b>c 则获取到: a>b
        Set<NodePath> shortestPaths = getShortestPaths(allPaths);

        // 查询消息
        Map<NodePath, ? extends Collection<? extends RemindType>> rootNodePathSubRemindsMap = remindRepo().getMatchPathSubReminds(shortestPaths);

        // 生成消息树
        TreeStructure<Node, RemindType> remindTree = new TreeStructure<>();
        for (Collection<? extends RemindType> reminds : rootNodePathSubRemindsMap.values()) {
            for (RemindType remind : reminds) {
                remindTree.put(remind, remind.path().nodes());
            }
        }

        // 分发消息
        for (BadgeWidget<RemindType> badgeWidget : badgeWidgets) {
            Set<NodePath> paths = getWidgetPath(badgeWidget);
            // 获取所有路径中的最短路径, 用于减少重合路径部分的无用查询, 例如有: a>b, a>b>c 则获取到: a>b
            paths = getShortestPaths(paths);
            if (paths.isEmpty()) {
                badgeWidget.showReminds(Collections.<RemindType>emptyList());
                continue;
            }

            List<RemindType> widgetReminds = new LinkedList<>();
            for (NodePath path : paths) {
                Collection<? extends RemindType> reminds = remindTree.getMatchPathSubData(path.nodes());
                if (reminds != null) {
                    widgetReminds.addAll(reminds);
                }
            }
            badgeWidget.showReminds(widgetReminds);
        }
    }

    private void putBadgeWidgetPath(BadgeWidget<RemindType> widget, Set<NodePath> put) {
        putBadgeWidgetPath(widget, put, Collections.<NodePath>emptySet());
    }
    private void putBadgeWidgetPath(BadgeWidget<RemindType> widget, Set<NodePath> put, Set<NodePath> remove) {
        Set<NodePath> attachedNodePaths = attachedWidgets.get(widget);
        if (attachedNodePaths == null) {
            attachedNodePaths = new HashSet<>();
            attachedWidgets.put(widget, attachedNodePaths);
        }

        if (!put.isEmpty()) {
            widgetsTree.putMore(widget, pathsToNodes(put));
            attachedNodePaths.addAll(put);
        }

        if (!remove.isEmpty()) {
            widgetsTree.removeMore(widget, pathsToNodes(remove));
            attachedNodePaths.removeAll(remove);
        }
    }
    private void removeWidgetPath(BadgeWidget<RemindType> widget, Set<NodePath> remove) {
        widgetsTree.removeMore(widget, pathsToNodes(remove));
        attachedWidgets.remove(widget);
    }
    private Set<NodePath> getWidgetPath(BadgeWidget<RemindType> widget) {
        return attachedWidgets.get(widget);
    }

    private Set<NodePath> getShortestPaths(Set<NodePath> paths) {

        TreeStructure<Node, NodePath> pathTree = new TreeStructure<>();
        for (NodePath path : paths) {
            pathTree.put(path, path.nodes());
        }

        return pathTree.getShortestPathData();
    }

    private Set<NodePath> getWidgetsAllPaths(Collection<BadgeWidget<RemindType>> badgeWidgets) {
        Set<NodePath> result = new HashSet<>();
        for (BadgeWidget<RemindType> badgeWidget : badgeWidgets) {
            result.addAll(attachedWidgets.get(badgeWidget));
        }
        return result;
    }

    private Iterable<? extends Iterable<Node>> pathsToNodes(Iterable<NodePath> paths) {
        List<List<Node>> nodes = new LinkedList<>();

        for (NodePath path : paths) {
            nodes.add(path.nodes());
        }

        return nodes;
    }
}
