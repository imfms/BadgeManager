package ms.imf.redpoint.manager;

import java.util.Collection;
import java.util.List;

import ms.imf.redpoint.entity.Node;

/**
 * 消息仓库
 *
 * @author imf_m
 * @date 2019/3/25
 */
public interface RemindRepo {

    Collection<Remind> getAllReminds();

    Collection<Remind> getSubPathRemind(List<Node> path);

    void insertRemind(Remind remind);
    void insertReminds(Iterable<Remind> reminds);

    void removeRemind(Long remindId);
    void removeReminds(Iterable<Long> remindIds);

    long removeMatchReminds(List<Node> path);
    long removeMatchSubReminds(List<Node> path);

    long removeAllReminds();

    void setRemindChangedListener(RemindChangedListener listener);

    void notifyRepoRemindChanged();
}
