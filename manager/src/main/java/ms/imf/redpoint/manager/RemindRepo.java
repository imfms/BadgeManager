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
public interface RemindRepo<RemindType extends Remind> {

    Collection<? extends RemindType> getAllReminds();

    Collection<? extends RemindType> getMatchReminds(List<Node> path);
    Collection<? extends RemindType> getMatchSubReminds(List<Node> path);

    void insertRemind(RemindType remind);
    void insertReminds(Iterable<? extends RemindType> reminds);

    void removeRemind(RemindType remind);
    void removeReminds(Iterable<? extends RemindType> reminds);

    long removeMatchReminds(List<Node> path);
    long removeMatchSubReminds(List<Node> path);

    long removeAllReminds();

    void setRemindChangedListener(RemindChangedListener<? super RemindType> listener);

    void notifyRepoRemindChanged();
}
