package ms.imf.redpoint.manager;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ms.imf.redpoint.entity.NodePath;

/**
 * 以节点树方式管理{@link RemindHandler}的{@link RemindHandlerManager}
 * <p>
 * 通过所有RemindHandler的支持路径生成一颗节点树，并将RemindHandler引用到其支持的对应节点下，
 * 从而达到通过节点快速匹配到对应的RemindHandler的目的，并通过此思想的优势来提升RemindHandlerManager内各项匹配任务的效率
 * <p>
 *
 * TODO concurrent safe
 * TODO test
 *
 * @author f_ms
 * @date 19-07-26
 */
public class TreeModeRemindHandlerManager<RemindType extends Remind> extends RemindHandlerManager<RemindType> {

    private final Map<RemindHandler<RemindType>, Set<NodePath>> attachedRemindHandlers = new HashMap<>();
    private final TreeStructure<RemindHandler<RemindType>> remindHandlerTree = new TreeStructure<>();

    public TreeModeRemindHandlerManager(RemindRepo<RemindType> repo) {
        super(repo);
    }

    @Override
    public void attachRemindHandler(RemindHandler<RemindType> remindHandler) {
        if (remindHandler == null) { return; }

        if (remindHandlerAttached(remindHandler)) {
            notifyRemindHandlerChanged(remindHandler);
            return;
        }

        putRemindHandlerPath(remindHandler, remindHandler.getPaths());

        notifyRemindHandlerDataChanged(remindHandler);
    }

    @Override
    public void detachRemindHandler(RemindHandler<RemindType> remindHandler) {
        if (remindHandler == null) { return; }
        if (!remindHandlerAttached(remindHandler)) { return; }

        removeRemindHandlerPath(
                remindHandler,
                getRemindHandlerPath(remindHandler)
        );
    }

    @Override
    public boolean remindHandlerAttached(RemindHandler<RemindType> remindHandler) {
        return attachedRemindHandlers.containsKey(remindHandler);
    }

    @Override
    public void notifyRemindHandlerChanged(RemindHandler<RemindType> remindHandler) {
        if (!remindHandlerAttached(remindHandler)) { return; }

        Set<NodePath> acceptPaths = remindHandler.getPaths();
        Set<NodePath> lastAcceptPaths = getRemindHandlerPath(remindHandler);

        Set<NodePath> addPaths = new HashSet<>(acceptPaths);
        addPaths.removeAll(lastAcceptPaths);

        Set<NodePath> removePaths = new HashSet<>(lastAcceptPaths);
        removePaths.removeAll(acceptPaths);

        putRemindHandlerPath(remindHandler, addPaths, removePaths);

        if (!addPaths.isEmpty()
                || !removePaths.isEmpty()) {
            notifyRemindHandlerDataChanged(remindHandler);
        }
    }

    @Override
    public void happenedRemindHandler(RemindHandler<RemindType> remindHandler) {
        if (!remindHandlerAttached(remindHandler)) { return; }

        Set<NodePath> paths = getRemindHandlerPath(remindHandler);

        if (remindRepo().removeMatchReminds(paths) <= 0) {
            return;
        }

        Set<RemindHandler<RemindType>> needChangedHandlers = remindHandlerTree.getPathRangeAllData(paths);
        notifyRemindHandlersDataChanged(needChangedHandlers);
    }

    @Override
    public void happenedRemindHandlerWithSubPathAll(RemindHandler<RemindType> remindHandler) {
        if (!remindHandlerAttached(remindHandler)) { return; }

        Set<NodePath> paths = getRemindHandlerPath(remindHandler);

        if (remindRepo().removeMatchSubReminds(paths) <= 0) {
            return;
        }

        /*
        变更涉及handler支持路径节点范围内节点和路径根节点后整个树上的所有handler
         */
        Set<RemindHandler<RemindType>> needChangedHandlers = new HashSet<>(
                remindHandlerTree.getPathRangeAllData(paths)
        );
        needChangedHandlers.addAll(
                remindHandlerTree.getMatchPathSubData(paths)
        );
        notifyRemindHandlersDataChanged(needChangedHandlers);
    }

    @Override
    public void notifyRemindDataChanged() {
        notifyRemindHandlersDataChanged(attachedRemindHandlers.keySet());
    }

    @Override
    public void notifyRemindDataChanged(Iterable<? extends RemindType> changedReminds) {

        /*
        取所有变更消息支持路径中的最长路径，获取对应的handler刷新其数据
          */

        TreeStructure<NodePath> remindTree = new TreeStructure<>();
        for (RemindType remind : changedReminds) {
            remindTree.put(remind.path(), remind.path());
        }
        Set<NodePath> longestPaths = remindTree.getLongestPathData();

        notifyRemindHandlersDataChanged(
                remindHandlerTree.getPathRangeAllData(longestPaths)
        );
    }

    private void notifyRemindHandlerDataChanged(RemindHandler<RemindType> remindHandler) {
        notifyRemindHandlersDataChanged(Collections.singleton(remindHandler));
    }
    private void notifyRemindHandlersDataChanged(Set<RemindHandler<RemindType>> remindHandlers) {
        if (remindHandlers.isEmpty()) { return; }

        /*
        handlers支持的路径节点大多存在重合部分，全部交给repo查询会增大开销
        例如路径a>b查询出的消息是包含a>b>c查询出的消息的，这时候只需要查询出a>b就可以了
        所以取所有支持路径中的最短重合路径获取消息，然后生成消息树为handler分发
         */

        // 对handlers支持路径去重
        Set<NodePath> allPaths = getRemindHandlersAllPaths(remindHandlers);
        if (allPaths.isEmpty()) {
            for (RemindHandler<RemindType> remindHandler : remindHandlers) {
                remindHandler.showReminds(Collections.<RemindType>emptyList());
            }
            return;
        }

        // 获取所有路径中的最短路径, 用于减少重合路径部分的无用查询, 例如有: a>b, a>b>c 则获取到: a>b
        Set<NodePath> shortestPaths = getShortestPaths(allPaths);

        // 查询消息
        Map<NodePath, ? extends Collection<? extends RemindType>> rootNodePathSubRemindsMap = remindRepo().getMatchSubReminds(shortestPaths);

        // 生成消息树
        TreeStructure<RemindType> remindTree = new TreeStructure<>();
        for (Collection<? extends RemindType> reminds : rootNodePathSubRemindsMap.values()) {
            for (RemindType remind : reminds) {
                remindTree.put(remind, remind.path());
            }
        }

        // 分发消息
        for (RemindHandler<RemindType> remindHandler : remindHandlers) {
            Set<NodePath> paths = getRemindHandlerPath(remindHandler);
            // 获取所有路径中的最短路径, 用于减少重合路径部分的无用查询, 例如有: a>b, a>b>c 则获取到: a>b
            paths = getShortestPaths(paths);
            if (paths.isEmpty()) {
                remindHandler.showReminds(Collections.<RemindType>emptyList());
                continue;
            }

            List<RemindType> handlerReminds = new LinkedList<>();
            for (NodePath path : paths) {
                Collection<? extends RemindType> reminds = remindTree.getMatchPathSubData(path);
                if (reminds != null) {
                    handlerReminds.addAll(reminds);
                }
            }
            remindHandler.showReminds(handlerReminds);
        }
    }

    private void putRemindHandlerPath(RemindHandler<RemindType> handler, Set<NodePath> put) {
        putRemindHandlerPath(handler, put, Collections.<NodePath>emptySet());
    }
    private void putRemindHandlerPath(RemindHandler<RemindType> handler, Set<NodePath> put, Set<NodePath> remove) {
        Set<NodePath> attachedNodePaths = attachedRemindHandlers.get(handler);
        if (attachedNodePaths == null) {
            attachedNodePaths = new HashSet<>();
            attachedRemindHandlers.put(handler, attachedNodePaths);
        }

        if (!put.isEmpty()) {
            remindHandlerTree.put(handler, put);
            attachedNodePaths.addAll(put);
        }

        if (!remove.isEmpty()) {
            remindHandlerTree.remove(handler, remove);
            attachedNodePaths.removeAll(remove);
        }
    }
    private void removeRemindHandlerPath(RemindHandler<RemindType> handler, Set<NodePath> remove) {
        remindHandlerTree.remove(handler, remove);
        attachedRemindHandlers.remove(handler);
    }
    private Set<NodePath> getRemindHandlerPath(RemindHandler<RemindType> handler) {
        return attachedRemindHandlers.get(handler);
    }

    private Set<NodePath> getShortestPaths(Set<NodePath> paths) {

        TreeStructure<NodePath> pathTree = new TreeStructure<>();
        for (NodePath path : paths) {
            pathTree.put(path, path);
        }

        return pathTree.getShortestPathData();
    }

    private Set<NodePath> getRemindHandlersAllPaths(Collection<RemindHandler<RemindType>> remindHandlers) {
        Set<NodePath> result = new HashSet<>();
        for (RemindHandler<RemindType> remindHandler : remindHandlers) {
            result.addAll(attachedRemindHandlers.get(remindHandler));
        }
        return result;
    }
}
