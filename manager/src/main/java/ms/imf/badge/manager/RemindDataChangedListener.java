package ms.imf.badge.manager;

/**
 * 徽标提醒数据变更监听器
 *
 * @param <RemindType> 支持的徽标提醒数据类型
 */
public interface RemindDataChangedListener<RemindType extends Remind> {

    /**
     * 当徽标提醒数据发生变更时
     */
    void onRemindDataChanged();

    /**
     * 当指定徽标提醒数据发生变更时
     * @param changedReminds 变更的徽标提醒数据
     */
    void onRemindDataChanged(Iterable<RemindType> changedReminds);
}
