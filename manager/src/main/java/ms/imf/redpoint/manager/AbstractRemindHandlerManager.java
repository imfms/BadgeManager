package ms.imf.redpoint.manager;

public abstract class AbstractRemindHandlerManager extends RemindHandlerManager {

    public AbstractRemindHandlerManager(RemindRepo repo) {
        super(repo);
    }

    @Override
    public void happenedRemindHandlers(Iterable<RemindHandler> handler) {
        for (RemindHandler remindHandler : handler) {
            happenedRemindHandler(remindHandler);
        }
    }

    @Override
    public void happenedReminds(Iterable<Remind> reminds) {
        for (Remind remind : reminds) {
            happenedRemind(remind);
        }
    }

    @Override
    public void happenedRemindHandlersWithSubNodeAll(Iterable<RemindHandler> handlers) {
        for (RemindHandler handler : handlers) {
            happenedRemindHandlerWithSubNodeAll(handler);
        }
    }

    @Override
    public void notifyRemindDataChanged(Iterable<Remind> changedReminds) {
        notifyRemindDataChanged();
    }

}
