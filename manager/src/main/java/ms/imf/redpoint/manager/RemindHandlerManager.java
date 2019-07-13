package ms.imf.redpoint.manager;

public abstract class RemindHandlerManager<RemindType extends Remind> {

    private static volatile RemindHandlerManager INSTANCE;

    public static synchronized <RemindType extends Remind> void init(RemindHandlerManager<RemindType> instance) {
        if (INSTANCE != null) {
            throw new IllegalStateException("INSTANCE already exists");
        }
        INSTANCE = instance;
    }

    public static <RemindType extends Remind> RemindHandlerManager<RemindType> instance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("please init " + RemindHandlerManager.class.getSimpleName() + " first");
        }
        return INSTANCE;
    }


    private final RemindRepo<RemindType> mRemindRepo;

    public RemindHandlerManager(RemindRepo<RemindType> repo) {
        if (repo == null) { throw new IllegalArgumentException("repo can't be null"); }
        mRemindRepo = repo;
        repo.setRemindChangedListener(new RemindChangedListener<RemindType>() {
            @Override
            public void onRemindChanged() {
                notifyRemindDataChanged();
            }

            @Override
            public void onRemindChanged(Iterable<RemindType> changedReminds) {
                RemindHandlerManager.this.notifyRemindDataChanged(changedReminds);
            }
        });
    }

    public RemindRepo<RemindType> remindRepo() {
        return mRemindRepo;
    }

    public abstract void addRemindHandler(RemindHandler<RemindType> handler);

    public abstract void removeRemindHandler(RemindHandler<RemindType> handler);

    public abstract boolean remindHandlerAttached(RemindHandler<RemindType> handler);

    public abstract void happenedRemindHandler(RemindHandler<RemindType> handler);
    public abstract void happenedRemindHandlers(Iterable<RemindHandler<RemindType>> handler);

    public abstract void happenedRemind(RemindType remind);
    public abstract void happenedReminds(Iterable<? extends RemindType> reminds);

    /**
     * 消费指定handler节点下所有消息(包括handler节点本身)
     */
    public abstract void happenedRemindHandlerWithSubNodeAll(RemindHandler<RemindType> handler);
    public abstract void happenedRemindHandlersWithSubNodeAll(Iterable<RemindHandler<RemindType>> handlers);

    public abstract void notifyRemindDataChanged();

    public abstract void notifyRemindDataChanged(Iterable<? extends RemindType> changedReminds);

    public abstract void notifyRemindHandlerChanged(RemindHandler<RemindType> handler);
}
