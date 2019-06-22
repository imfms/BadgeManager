package ms.imf.redpoint.manager;

public abstract class AbstractRemindHandlerControlCenter extends RemindHandlerControlCenter{

    public AbstractRemindHandlerControlCenter(RemindRepo repo) {
        super(repo);
    }

    @Override
    public void happenedRemindHandlers(Iterable<AbstractRemindHandler> handler) {
        for (AbstractRemindHandler remindHandler : handler) {
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
    public void happenedRemindHandlersWithSubNodeAll(Iterable<AbstractRemindHandler> handlers) {
        for (AbstractRemindHandler handler : handlers) {
            happenedRemindHandlerWithSubNodeAll(handler);
        }
    }

    @Override
    public void notifyRemindDataChanged(Iterable<Remind> changedReminds) {
        notifyRemindDataChanged();
    }

}
