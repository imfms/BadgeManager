package ms.imf.badge.manager;

/**
 * 提醒数据变更监听监听器
 *
 * @param <RemindType> 支持的提醒数据类型
 */
public interface RemindDataChangedListener<RemindType extends Remind> {

    /**
     * 当消息数据变更时
     */
    void onRemindDataChanged();

    /**
     * 当指定消息数据变更时
     * @param changedReminds 变更的消息数据
     */
    void onRemindDataChanged(Iterable<RemindType> changedReminds);
}
