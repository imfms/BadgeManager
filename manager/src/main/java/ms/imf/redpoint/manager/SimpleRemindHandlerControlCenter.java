package ms.imf.redpoint.manager;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import ms.imf.redpoint.entity.NodePath;


public class SimpleRemindHandlerControlCenter extends AbstractRemindHandlerControlCenter {

    private final Set<AbstractRemindHandler> remindHandlers = new LinkedHashSet<>();

    public SimpleRemindHandlerControlCenter(RemindRepo repo) {
        super(repo);
    }

    @Override
    public void addRemindHandler(AbstractRemindHandler handler) {
        if (handler == null) { throw new IllegalArgumentException("handler can't be null"); }
        if (remindHandlers.contains(handler)) {
            return;
        }
        remindHandlers.add(handler);
        notifyRemindHandlerChanged(handler);
    }

    @Override
    public void removeRemindHandler(AbstractRemindHandler handler) {
        if (handler == null) {
            return;
        }
        remindHandlers.remove(handler);
    }

    @Override
    public void happenedRemindHandler(AbstractRemindHandler handler) {

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
    public void happenedRemindHandlerWithSubNodeAll(AbstractRemindHandler handler) {
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
        for (AbstractRemindHandler handler : remindHandlers) {
            notifyRemindHandlerChanged(handler);
        }
    }

    @Override
    public void notifyRemindHandlerChanged(AbstractRemindHandler handler) {

        List<Remind> reminds = new LinkedList<>();
        for (NodePath path : handler.getPaths()) {
            reminds.addAll(remindRepo().getSubPathRemind(path.nodes()));
        }

        handler.showReminds(reminds);
    }

    @Override
    public boolean remindHandlerAttached(AbstractRemindHandler handler) {
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
