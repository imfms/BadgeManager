package ms.imf.redpoint.manager;

public interface RemindChangedListener {
    void onRemindChanged();
    void onRemindChanged(Iterable<Remind> changedReminds);
}
