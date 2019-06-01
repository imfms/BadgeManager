package ms.imf.redpoint.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import ms.imf.redpoint.entity.Node;

public abstract class AbstractRemindRepo implements RemindRepo {

    private RemindChangedListener mRemindChangedListener;

    @Override
    public Collection<Remind> getSubPathRemind(List<Node> path) {
        List<Remind> results = new LinkedList<>();

        for (Remind remind : getAllReminds()) {
            if (isMySubPathWithMe(path, remind.path.nodes())) {
                results.add(remind);
            }
        }

        return results;
    }

    @Override
    public void insertReminds(Iterable<Remind> reminds) {
        for (Remind remind : reminds) {
            insertRemind(remind);
        }
    }

    @Override
    public void removeReminds(Iterable<Long> remindIds) {
        for (Long remindId : remindIds) {
            removeRemind(remindId);
        }
    }

    @Override
    public long removeMatchReminds(List<Node> path) {

        List<Remind> handledReminds = new LinkedList<>();

        for (Remind remind : getAllReminds()) {
            if (isMatched(path, remind.path.nodes())) {
                handledReminds.add(remind);
            }
        }

        return removeReminds(handledReminds);
    }

    @Override
    public long removeMatchSubReminds(List<Node> path) {
        return removeReminds(getSubPathRemind(path));
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

    private long removeReminds(Collection<Remind> reminds) {
        if (reminds.isEmpty()) {
            return 0;
        }
        List<Long> remindIds = new ArrayList<>(reminds.size());
        for (Remind remind : reminds) {
            remindIds.add(remind.id);
        }
        removeReminds(remindIds);
        return remindIds.size();
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
