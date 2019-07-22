package ms.imf.redpoint.manager;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import ms.imf.redpoint.entity.NodePath;

/**
 * {@link RemindHandlerManager}的简易实现，本实现为demo用途，没有良好的性能表现，推荐生产环境自行重写
 *
 * @param <RemindType> 支持的消息数据类型
 */
public class SimpleRemindHandlerManager<RemindType extends Remind> extends RemindHandlerManager<RemindType> {

    private final Set<RemindHandler<RemindType>> remindHandlers = new LinkedHashSet<>();

    public SimpleRemindHandlerManager(RemindRepo<RemindType> repo) {
        super(repo);
    }

    @Override
    public void attachRemindHandler(RemindHandler<RemindType> remindHandler) {
        if (remindHandler == null) {
            throw new IllegalArgumentException("handler can't be null");
        }
        if (remindHandlers.contains(remindHandler)) {
            return;
        }
        remindHandlers.add(remindHandler);
        notifyRemindHandlerChanged(remindHandler);
    }

    @Override
    public void detachRemindHandler(RemindHandler<RemindType> remindHandler) {
        if (remindHandler == null) {
            return;
        }
        remindHandlers.remove(remindHandler);
    }

    @Override
    public void happenedRemindHandler(RemindHandler<RemindType> remindHandler) {

        if (!remindHandlerAttached(remindHandler)) {
            return;
        }

        List<NodePath> remindChangedPaths = new LinkedList<>();
        for (NodePath path : remindHandler.getPaths()) {
            if (remindRepo().removeMatchReminds(NodePath.instance(path.nodes())) > 0) {
                remindChangedPaths.add(path);
            }
        }

        notifyRemindDataChanged();
    }

    @Override
    public void happenedRemindHandlerWithSubPathAll(RemindHandler<RemindType> remindHandler) {
        if (!remindHandlerAttached(remindHandler)) {
            return;
        }
        for (NodePath path : remindHandler.getPaths()) {
            remindRepo().removeMatchSubReminds(NodePath.instance(path.nodes()));
        }
        notifyRemindDataChanged();
    }

    @Override
    public void notifyRemindDataChanged() {
        for (RemindHandler<RemindType> handler : remindHandlers) {
            notifyRemindHandlerChanged(handler);
        }
    }

    @Override
    public void notifyRemindDataChanged(Iterable<? extends RemindType> changedReminds) {
        notifyRemindDataChanged();
    }

    @Override
    public void notifyRemindHandlerChanged(RemindHandler<RemindType> remindHandler) {

        List<RemindType> reminds = new LinkedList<>();
        for (NodePath path : remindHandler.getPaths()) {
            reminds.addAll(remindRepo().getMatchSubReminds(NodePath.instance(path.nodes())));
        }

        remindHandler.showReminds(reminds);
    }

    @Override
    public boolean remindHandlerAttached(RemindHandler<RemindType> remindHandler) {
        if (remindHandler == null) {
            return false;
        }
        return remindHandlers.contains(remindHandler);
    }
}
