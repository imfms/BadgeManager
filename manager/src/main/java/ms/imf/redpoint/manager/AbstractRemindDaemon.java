package ms.imf.redpoint.manager;

public abstract class AbstractRemindDaemon<Repo extends RemindRepo> implements RemindDaemon {

    private final Repo repo;

    public AbstractRemindDaemon(Repo remindRepo) {
        if (remindRepo == null) {
            throw new IllegalArgumentException("repo can't be null");
        }
        this.repo = remindRepo;
    }

    protected Repo repo() {
        return repo;
    }

    @Override
    public void resume() {
        requiredStarted();
    }

    @Override
    public void pause() {
        requiredStarted();
    }

    protected void requiredStarted() {
        if (!started()) {
            throw new IllegalStateException("daemon not started");
        }
    }

}
