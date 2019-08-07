package ms.imf.badge.manager;

/**
 * 徽标管理器
 * <p>
 * 管理所有的{@link BadgeWidget}，以{@link RemindRepo}为提醒数据源，为徽标控件支持路径提供对应提醒数据的获取/更新/消费
 * <p>
 * 推荐使用以节点树结构管理控件、提醒的徽标管理器： {@link TreeModeBadgeManager}
 *
 * @param <RemindType> 支持提醒的类型
 * @author f_ms
 */
public abstract class BadgeManager<RemindType extends Remind> {

    private final RemindRepo<RemindType> mRemindRepo;

    public BadgeManager(RemindRepo<RemindType> repo) {
        if (repo == null) { throw new IllegalArgumentException("repo can't be null"); }
        mRemindRepo = repo;
        repo.setRemindDataChangedListener(new RemindDataChangedListener<RemindType>() {
            @Override
            public void onRemindDataChanged() {
                notifyRemindDataChanged();
            }

            @Override
            public void onRemindDataChanged(Iterable<RemindType> changedReminds) {
                BadgeManager.this.notifyRemindDataChanged(changedReminds);
            }
        });
    }

    public RemindRepo<RemindType> remindRepo() {
        return mRemindRepo;
    }

    /**
     * 添加要被管理的{@link BadgeWidget}
     *
     * @param badgeWidget 被管理的{@link BadgeWidget}
     */
    public abstract void attachWidget(BadgeWidget<RemindType> badgeWidget);

    /**
     * 解除对{@link BadgeWidget}的管理
     *
     * @param badgeWidget 被解除管理的{@link BadgeWidget}
     */
    public abstract void detachWidget(BadgeWidget<RemindType> badgeWidget);

    /**
     * 指定{@link BadgeWidget}是否被附加？
     *
     * @param badgeWidget 被查询的{@link BadgeWidget}
     * @return true == 是的，我在管理它, false==不，它没有被我管理
     */
    public abstract boolean widgetAttached(BadgeWidget<RemindType> badgeWidget);

    /**
     * 当{@link BadgeWidget}支持的节点路径发生变更
     *
     * @param badgeWidget 支持节点路径发生变更的{@link BadgeWidget}
     */
    public abstract void notifyWidgetChanged(BadgeWidget<RemindType> badgeWidget);

    /**
     * {@link BadgeWidget}被消费，消费范围为支持的节点路径
     * @param badgeWidget 被消费的{@link BadgeWidget}
     */
    public abstract void happenedWidget(BadgeWidget<RemindType> badgeWidget);

    /**
     * {@link BadgeWidget}被消费，消费范围为消息处理器支持的节点路径及其的子路径
     *
     * @param badgeWidget 被消费的{@link BadgeWidget}
     */
    public abstract void happenedWidgetWithSubPath(BadgeWidget<RemindType> badgeWidget);

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
