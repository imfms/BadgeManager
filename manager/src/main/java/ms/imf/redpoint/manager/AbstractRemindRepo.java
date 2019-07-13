package ms.imf.redpoint.manager;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import ms.imf.redpoint.entity.Node;

public abstract class AbstractRemindRepo<RemindType extends Remind> implements RemindRepo<RemindType> {

    private RemindChangedListener mRemindChangedListener;

    @Override
    public Collection<RemindType> getMatchReminds(List<Node> path) {
        List<RemindType> results = new LinkedList<>();

        for (RemindType remind : getAllReminds()) {
            if (isMatched(path, remind.path.nodes())) {
                results.add(remind);
            }
        }

        return results;
    }

    @Override
    public Collection<RemindType> getMatchSubReminds(List<Node> path) {
        List<RemindType> results = new LinkedList<>();

        for (RemindType remind : getAllReminds()) {
            if (isMySubPathWithMe(path, remind.path.nodes())) {
                results.add(remind);
            }
        }

        return results;
    }

    @Override
    public void insertReminds(Iterable<? extends RemindType> reminds) {
        for (RemindType remind : reminds) {
            insertRemind(remind);
        }
    }

    @Override
    public void removeReminds(Iterable<? extends RemindType> reminds) {
        for (RemindType remind : reminds) {
            removeRemind(remind);
        }
    }

    @Override
    public long removeMatchReminds(List<Node> path) {

        List<RemindType> handledReminds = new LinkedList<>();

        for (RemindType remind : getAllReminds()) {
            if (isMatched(path, remind.path.nodes())) {
                handledReminds.add(remind);
            }
        }

        return removeReminds(handledReminds);
    }

    @Override
    public long removeMatchSubReminds(List<Node> path) {
        return removeReminds(getMatchSubReminds(path));
    }

    @Override
    public long removeAllReminds() {
        return removeReminds(getAllReminds());
    }

    @Override
    public void setRemindChangedListener(RemindChangedListener listener) {
        mRemindChangedListener = listener;
    }

    @Override
    public void notifyRepoRemindChanged() {
        if (mRemindChangedListener != null) {
            mRemindChangedListener.onRemindChanged();
        }
    }

    protected RemindChangedListener remindChangedListener() {
        return mRemindChangedListener;
    }

    private long removeReminds(Collection<? extends RemindType> reminds) {
        if (reminds.isEmpty()) {
            return 0;
        }
        removeReminds((Iterable<? extends RemindType>) reminds);
        return reminds.size();
    }

    private boolean isMySubPathWithMe(List<Node> me, List<Node> him) {
        if (him.size() < me.size()) {
            return false;
        }

        for (int i = 0; i < me.size(); i++) {
            Node myNode = me.get(i);
            Node hisNode = him.get(i);

            if (!hisNode.equals(myNode)) {
                return false;
            }
        }

        return true;
    }
    private boolean isMatched(List<Node> me, List<Node> him) {
        if (him.size() != me.size()) {
            return false;
        }

        for (int i = 0; i < me.size(); i++) {
            Node myNode = me.get(i);
            Node hisNode = him.get(i);

            if (!hisNode.equals(myNode)) {
                return false;
            }
        }

        return true;
    }
}
