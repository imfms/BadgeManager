package ms.imf.redpoint.manager;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import ms.imf.redpoint.entity.NodePath;


public class SimpleRemindHandlerManager extends AbstractRemindHandlerManager {

    private final Set<RemindHandler> remindHandlers = new LinkedHashSet<>();

    public SimpleRemindHandlerManager(RemindRepo repo) {
        super(repo);
    }

    @Override
    public void addRemindHandler(RemindHandler handler) {
        if (handler == null) { throw new IllegalArgumentException("handler can't be null"); }
        if (remindHandlers.contains(handler)) {
            return;
        }
        remindHandlers.add(handler);
        notifyRemindHandlerChanged(handler);
    }

    @Override
    public void removeRemindHandler(RemindHandler handler) {
        if (handler == null) {
            return;
        }
        remindHandlers.remove(handler);
    }

    @Override
    public void happenedRemindHandler(RemindHandler handler) {

        if (!remindHandlerAttached(handler)) {
            return;
        }

        List<NodePath> remindChangedPaths = new LinkedList<>();
        for (NodePath path : handler.getPaths()) {
            if (remindRepo().removeMatchReminds(path.nodes()) > 0) {
                remindChangedPaths.add(path);
            }
        }

        notifyRemindDataChanged();
    }

    /**
     * 消费指定handler节点下所有消息(包括handler节点本身)
     */
    @Override
    public void happenedRemindHandlerWithSubNodeAll(RemindHandler handler) {
        if (!remindHandlerAttached(handler)) {
            return;
        }
        for (NodePath path : handler.getPaths()) {
            remindRepo().removeMatchSubReminds(path.nodes());
        }
        notifyRemindDataChanged();
    }

    @Override
    public void notifyRemindDataChanged() {
        for (RemindHandler handler : remindHandlers) {
            notifyRemindHandlerChanged(handler);
        }
    }

    @Override
    public void notifyRemindHandlerChanged(RemindHandler handler) {

        List<Remind> reminds = new LinkedList<>();
        for (NodePath path : handler.getPaths()) {
            reminds.addAll(remindRepo().getSubPathRemind(path.nodes()));
        }

        handler.showReminds(reminds);
    }

    @Override
    public boolean remindHandlerAttached(RemindHandler handler) {
        if (handler == null) {
            return false;
        }
        return remindHandlers.contains(handler);
    }

    @Override
    public void happenedRemind(Remind remind) {
        remindRepo().removeRemind(remind.id);
        notifyRemindDataChanged();
    }
}
