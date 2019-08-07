package ms.imf.badge.manager;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ms.imf.badge.entity.Node;
import ms.imf.badge.entity.NodePath;

/**
 * 消息数据仓库方法的简单封装
 *
 * @param <RemindType> {@link RemindRepo}
 */
public abstract class AbstractRemindRepo<RemindType extends Remind> implements RemindRepo<RemindType> {

    private RemindDataChangedListener mRemindDataChangedListener;

    @Override
    public Map<NodePath, ? extends Collection<? extends RemindType>> getMatchPathReminds(Collection<NodePath> nodePaths) {
        HashMap<NodePath, Collection<? extends RemindType>> result = new HashMap<>(nodePaths.size());

        for (NodePath nodePath : nodePaths) {
            result.put(nodePath, getMatchRemind(nodePath));
        }

        return result;
    }

    private Collection<RemindType> getMatchRemind(NodePath nodePath) {
        List<RemindType> results = new LinkedList<>();

        for (RemindType remind : getAllReminds()) {
            if (isMatched(nodePath.nodes(), remind.path().nodes())) {
                results.add(remind);
            }
        }

        return results;
    }

    @Override
    public Map<NodePath, ? extends Collection<? extends RemindType>> getMatchPathSubReminds(Collection<NodePath> nodePaths) {
        HashMap<NodePath, Collection<? extends RemindType>> result = new HashMap<>(nodePaths.size());

        for (NodePath nodePath : nodePaths) {
            result.put(nodePath, getMatchSubReminds(nodePath));
        }

        return result;
    }

    private Collection<RemindType> getMatchSubReminds(NodePath nodePath) {
        List<RemindType> results = new LinkedList<>();

        for (RemindType remind : getAllReminds()) {
            if (isMySubPathWithMe(nodePath.nodes(), remind.path().nodes())) {
                results.add(remind);
            }
        }

        return results;
    }

    @Override
    public long removeMatchPathReminds(Collection<NodePath> nodePaths) {
        long result = 0;
        for (NodePath nodePath : nodePaths) {
            result += removeMatchReminds(nodePath);
        }
        return result;
    }

    private long removeMatchReminds(NodePath nodePath) {
        List<RemindType> handledReminds = new LinkedList<>();

        for (RemindType remind : getAllReminds()) {
            if (isMatched(nodePath.nodes(), remind.path().nodes())) {
                handledReminds.add(remind);
            }
        }

        return removeReminds(handledReminds);
    }

    @Override
    public long removeMatchPathSubReminds(Collection<NodePath> nodePaths) {
        long result = 0;
        for (NodePath nodePath : nodePaths) {
            result += removeMatchSubReminds(nodePath);
        }
        return result;
    }

    private long removeMatchSubReminds(NodePath nodePath) {
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
