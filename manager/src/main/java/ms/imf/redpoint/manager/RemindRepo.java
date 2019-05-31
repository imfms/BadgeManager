package ms.imf.redpoint.manager;

import java.util.Collection;
import java.util.List;

/**
 * 消息仓库
 *
 * @author imf_m
 * @date 2019/3/25
 */
public interface RemindRepo extends Iterable<Remind> {

    void clearAllRemind();

    void handledRemind(Collection<Remind> reminds);

    Collection<Remind> getReminds();

    List<Long> getUnHandledRemindIds();

    List<Long> getHandledRemindIds();

    void insertReminds(Iterable<Remind> reminds);

    void removeHandledReminds(Iterable<Long> remindIds);

    void setRepoRemindChangedListener(RemindChangedListener listener);

    void notifyRepoRemindChanged();
}
