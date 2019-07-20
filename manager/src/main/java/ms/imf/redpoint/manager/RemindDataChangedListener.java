package ms.imf.redpoint.manager;

public interface RemindDataChangedListener<RemindType extends Remind> {
    void onRemindChanged();
    void onRemindChanged(Iterable<RemindType> changedReminds);
}
