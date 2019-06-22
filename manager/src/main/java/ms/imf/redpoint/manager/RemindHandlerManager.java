package ms.imf.redpoint.manager;

public abstract class RemindHandlerManager {

    private static volatile RemindHandlerManager INSTANCE;

    public static synchronized void init(RemindHandlerManager instance) {
        if (INSTANCE != null) {
            throw new IllegalStateException("INSTANCE already exists");
        }
        INSTANCE = instance;
    }

    public static RemindHandlerManager instance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("please init " + RemindHandlerManager.class.getSimpleName() + " first");
        }
        return INSTANCE;
    }


    private final RemindRepo mRemindRepo;

    public RemindHandlerManager(RemindRepo repo) {
        if (repo == null) { throw new IllegalArgumentException("repo can't be null"); }
        mRemindRepo = repo;
        repo.setRemindChangedListener(new RemindChangedListener() {
            @Override
            public void onRemindChanged() {
                notifyRemindDataChanged();
            }

            @Override
            public void onRemindChanged(Iterable<Remind> changedReminds) {
                RemindHandlerManager.this.notifyRemindDataChanged(changedReminds);
            }
        });
    }

    public RemindRepo remindRepo() {
        return mRemindRepo;
    }

    public abstract void addRemindHandler(RemindHandler handler);

    public abstract void removeRemindHandler(RemindHandler handler);

    public abstract boolean remindHandlerAttached(RemindHandler handler);

    public abstract void happenedRemindHandler(RemindHandler handler);
    public abstract void happenedRemindHandlers(Iterable<RemindHandler> handler);

    public abstract void happenedRemind(Remind remind);
    public abstract void happenedReminds(Iterable<Remind> reminds);

    /**
     * 消费指定handler节点下所有消息(包括handler节点本身)
     */
    public abstract void happenedRemindHandlerWithSubNodeAll(RemindHandler handler);
    public abstract void happenedRemindHandlersWithSubNodeAll(Iterable<RemindHandler> handlers);

    public abstract void notifyRemindDataChanged();

    public abstract void notifyRemindDataChanged(Iterable<Remind> changedReminds);

    public abstract void notifyRemindHandlerChanged(RemindHandler handler);
}
