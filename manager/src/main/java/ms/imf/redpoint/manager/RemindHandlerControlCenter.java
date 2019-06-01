package ms.imf.redpoint.manager;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import ms.imf.redpoint.entity.NodePath;


public class RemindHandlerControlCenter {

    private static volatile RemindHandlerControlCenter INSTANCE;

    public static void init(RemindRepo repo) {
        if (INSTANCE == null) {
            synchronized (RemindHandlerControlCenter.class) {
                if (INSTANCE == null) {
                    INSTANCE = new RemindHandlerControlCenter(repo);
                }
            }
        }
    }

    public static RemindHandlerControlCenter instance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("please init " + RemindHandlerControlCenter.class.getSimpleName() + " first");
        }
        return INSTANCE;
    }

    private final RemindRepo mRemindRepo;
    private final Set<AbstractRemindHandler> remindHandlers = new LinkedHashSet<>();

    public RemindHandlerControlCenter(RemindRepo repo) {
        if (repo == null) { throw new IllegalArgumentException("repo can't be null"); }
        mRemindRepo = repo;
        repo.setRemindChangedListener(new RemindChangedListener() {
            @Override
            public void onRemindChanged() {
                notifyRemindChanged();
            }

            @Override
            public void onRemindChanged(Iterable<Remind> changedReminds) {
                // TODO: 19-6-1  opt
                notifyRemindChanged();
            }
        });
    }

    public void addRemindHandler(AbstractRemindHandler handler) {
        if (handler == null) { throw new IllegalArgumentException("handler can't be null"); }
        if (remindHandlers.contains(handler)) {
            return;
        }
        remindHandlers.add(handler);
        notifyRemindChanged(handler);
    }

    public void removeRemindHandler(AbstractRemindHandler handler) {
        if (handler == null) {
            return;
        }
        remindHandlers.remove(handler);
    }

    public void happened(AbstractRemindHandler handler) {

        if (!remindHandlerAttached(handler)) {
            return;
        }

        List<NodePath> remindChangedPaths = new LinkedList<>();
        for (NodePath path : handler.getPaths()) {
            if (mRemindRepo.removeMatchReminds(path.nodes()) > 0) {
                remindChangedPaths.add(path);
            }
        }

        if (!remindChangedPaths.isEmpty()) {
            notifyPathRemindChanged(remindChangedPaths);
        }

        notifyRemindChanged();
    }

    /**
     * 消费指定handler节点下所有消息(包括handler节点本身)
     */
    public void happenedAll(AbstractRemindHandler handler) {
        if (!remindHandlerAttached(handler)) {
            return;
        }
        for (NodePath path : handler.getPaths()) {
            mRemindRepo.removeMatchSubReminds(path.nodes());
        }
        notifyRemindChanged();
    }

    private void notifyPathRemindChanged(Iterable<NodePath> paths) {
        // TODO: 19-6-1  opt
        notifyRemindChanged();
    }

    public void notifyRemindChanged() {
        for (AbstractRemindHandler handler : remindHandlers) {
            notifyRemindChanged(handler);
        }
    }

    public void notifyRemindChanged(AbstractRemindHandler handler) {

        List<Remind> reminds = new LinkedList<>();
        for (NodePath path : handler.getPaths()) {
            reminds.addAll(mRemindRepo.getSubPathRemind(path.nodes()));
        }

        handler.showReminds(reminds);
    }

    public boolean remindHandlerAttached(AbstractRemindHandler handler) {
        if (handler == null) {
            return false;
        }
        return remindHandlers.contains(handler);
    }
}
