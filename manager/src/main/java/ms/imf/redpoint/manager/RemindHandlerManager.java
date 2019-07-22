package ms.imf.redpoint.manager;

/**
 * RemindHandler管理器
 * <p>
 * 用于管理RemindHandler支持节点路径对应消息数据的获取/更新/触发
 *
 * @param <RemindType> 支持的消息类型
 * @author f_ms
 */
public abstract class RemindHandlerManager<RemindType extends Remind> {

    private final RemindRepo<RemindType> mRemindRepo;

    public RemindHandlerManager(RemindRepo<RemindType> repo) {
        if (repo == null) { throw new IllegalArgumentException("repo can't be null"); }
        mRemindRepo = repo;
        repo.setRemindDataChangedListener(new RemindDataChangedListener<RemindType>() {
            @Override
            public void onRemindDataChanged() {
                notifyRemindDataChanged();
            }

            @Override
            public void onRemindDataChanged(Iterable<RemindType> changedReminds) {
                RemindHandlerManager.this.notifyRemindDataChanged(changedReminds);
            }
        });
    }

    public RemindRepo<RemindType> remindRepo() {
        return mRemindRepo;
    }

    /**
     * 添加管理的{@link RemindHandler}
     *
     * @param remindHandler 被附加的{@link RemindHandler}
     */
    public abstract void attachRemindHandler(RemindHandler<RemindType> remindHandler);

    /**
     * 解除对{@link RemindHandler}的管理
     *
     * @param remindHandler 被解除管理的{@link RemindHandler}
     */
    public abstract void detachRemindHandler(RemindHandler<RemindType> remindHandler);

    /**
     * 指定{@link RemindHandler}是否被附加？
     *
     * @param remindHandler 被查询的{@link RemindHandler}
     * @return true == 是的，我在管理它, false==不，它没有被我管理
     */
    public abstract boolean remindHandlerAttached(RemindHandler<RemindType> remindHandler);

    /**
     * {@link RemindHandler}被触发，触发范围为支持的节点路径
     * @param remindHandler 被触发的{@link RemindHandler}
     */
    public abstract void happenedRemindHandler(RemindHandler<RemindType> remindHandler);

    /**
     * {@link RemindHandler}被触发，触发范围为消息处理器支持的节点路径及其的子路径
     *
     * @param remindHandler 被触发的{@link RemindHandler}
     */
    public abstract void happenedRemindHandlerWithSubPathAll(RemindHandler<RemindType> remindHandler);

    /**
     * 当{@link RemindHandler}支持nodePath发成变更
     *
     * @param remindHandler 发生变更的{@link RemindHandler}
     */
    public abstract void notifyRemindHandlerChanged(RemindHandler<RemindType> remindHandler);

    /**
     * 当提醒数据发生变更
     */
    public abstract void notifyRemindDataChanged();

    /**
     * 当指定提醒数据发生变更
     * @param changedReminds 发生变更的提醒数据
     */
    public abstract void notifyRemindDataChanged(Iterable<? extends RemindType> changedReminds);
}
