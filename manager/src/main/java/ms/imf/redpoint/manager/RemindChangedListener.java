package ms.imf.redpoint.manager;

public interface RemindChangedListener<RemindType extends Remind> {
    void onRemindChanged();
    void onRemindChanged(Iterable<RemindType> changedReminds);
}
