package ms.imf.redpoint.manager;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import ms.imf.redpoint.entity.Node;
import ms.imf.redpoint.entity.NodePath;

/**
 * 消息数据仓库方法的简单封装
 *
 * @param <RemindType> {@link RemindRepo}
 */
public abstract class AbstractRemindRepo<RemindType extends Remind> implements RemindRepo<RemindType> {

    private RemindDataChangedListener mRemindDataChangedListener;

    @Override
    public Collection<RemindType> getMatchReminds(NodePath nodePath) {
        List<RemindType> results = new LinkedList<>();

        for (RemindType remind : getAllReminds()) {
            if (isMatched(nodePath.nodes(), remind.path().nodes())) {
                results.add(remind);
            }
        }

        return results;
    }

    @Override
    public Collection<RemindType> getMatchSubReminds(NodePath nodePath) {
        List<RemindType> results = new LinkedList<>();

        for (RemindType remind : getAllReminds()) {
            if (isMySubPathWithMe(nodePath.nodes(), remind.path().nodes())) {
                results.add(remind);
            }
        }

        return results;
    }

    @Override
    public void removeReminds(Iterable<? extends RemindType> reminds) {
        for (RemindType remind : reminds) {
            removeRemind(remind);
        }
    }

    @Override
    public long removeMatchReminds(NodePath nodePath) {

        List<RemindType> handledReminds = new LinkedList<>();

        for (RemindType remind : getAllReminds()) {
            if (isMatched(nodePath.nodes(), remind.path().nodes())) {
                handledReminds.add(remind);
            }
        }

        return removeReminds(handledReminds);
    }

    @Override
    public long removeMatchSubReminds(NodePath nodePath) {
        return removeReminds(getMatchSubReminds(nodePath));
    }

    @Override
    public long removeAllReminds() {
        return removeReminds(getAllReminds());
    }

    @Override
    public void setRemindDataChangedListener(RemindDataChangedListener listener) {
        mRemindDataChangedListener = listener;
    }

    protected RemindDataChangedListener remindChangedListener() {
        return mRemindDataChangedListener;
    }

    private long removeReminds(Collection<? extends RemindType> reminds) {
        if (reminds.isEmpty()) {
            return 0;
        }
        removeReminds((Iterable<? extends RemindType>) reminds);
        return reminds.size();
    }

    public static boolean isMySubPathWithMe(List<Node> me, List<Node> him) {
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
    public static boolean isMatched(List<Node> me, List<Node> him) {
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
