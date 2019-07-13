package ms.imf.redpoint.manager;

public abstract class AbstractRemindHandlerManager<RemindType extends Remind> extends RemindHandlerManager<RemindType> {

    public AbstractRemindHandlerManager(RemindRepo<RemindType> repo) {
        super(repo);
    }

    @Override
    public void happenedRemindHandlers(Iterable<RemindHandler<RemindType>> handler) {
        for (RemindHandler<RemindType> remindHandler : handler) {
            happenedRemindHandler(remindHandler);
        }
    }

    @Override
    public void happenedReminds(Iterable<RemindType> reminds) {
        for (RemindType remind : reminds) {
            happenedRemind(remind);
        }
    }

    @Override
    public void happenedRemindHandlersWithSubNodeAll(Iterable<RemindHandler<RemindType>> handlers) {
        for (RemindHandler<RemindType> handler : handlers) {
            happenedRemindHandlerWithSubNodeAll(handler);
        }
    }

    @Override
    public void notifyRemindDataChanged(Iterable<RemindType> changedReminds) {
        notifyRemindDataChanged();
    }

}
