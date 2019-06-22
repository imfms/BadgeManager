package ms.imf.redpoint.manager;

public abstract class RemindHandlerControlCenter {

    private static volatile RemindHandlerControlCenter INSTANCE;

    public static synchronized void init(RemindHandlerControlCenter instance) {
        if (INSTANCE != null) {
            throw new IllegalStateException("INSTANCE already exists");
        }
        INSTANCE = instance;
    }

    public static RemindHandlerControlCenter instance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("please init " + RemindHandlerControlCenter.class.getSimpleName() + " first");
        }
        return INSTANCE;
    }


    private final RemindRepo mRemindRepo;

    public RemindHandlerControlCenter(RemindRepo repo) {
        if (repo == null) { throw new IllegalArgumentException("repo can't be null"); }
        mRemindRepo = repo;
        repo.setRemindChangedListener(new RemindChangedListener() {
            @Override
            public void onRemindChanged() {
                notifyRemindDataChanged();
            }

            @Override
            public void onRemindChanged(Iterable<Remind> changedReminds) {
                RemindHandlerControlCenter.this.notifyRemindDataChanged(changedReminds);
            }
        });
    }

    public RemindRepo remindRepo() {
        return mRemindRepo;
    }

    public abstract void addRemindHandler(AbstractRemindHandler handler);

    public abstract void removeRemindHandler(AbstractRemindHandler handler);

    public abstract boolean remindHandlerAttached(AbstractRemindHandler handler);

    public abstract void happenedRemindHandler(AbstractRemindHandler handler);
    public abstract void happenedRemindHandlers(Iterable<AbstractRemindHandler> handler);

    public abstract void happenedRemind(Remind remind);
    public abstract void happenedReminds(Iterable<Remind> reminds);

    /**
     * 消费指定handler节点下所有消息(包括handler节点本身)
     */
    public abstract void happenedRemindHandlerWithSubNodeAll(AbstractRemindHandler handler);
    public abstract void happenedRemindHandlersWithSubNodeAll(Iterable<AbstractRemindHandler> handlers);

    public abstract void notifyRemindDataChanged();

    public abstract void notifyRemindDataChanged(Iterable<Remind> changedReminds);

    public abstract void notifyRemindHandlerChanged(AbstractRemindHandler handler);
}
