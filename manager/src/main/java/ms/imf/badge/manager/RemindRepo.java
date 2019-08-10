package ms.imf.badge.manager;

import java.util.Collection;
import java.util.Map;

import ms.imf.badge.entity.NodePath;

/**
 * 徽标提醒数据仓库，提供对数据的存储、读、写功能
 *
 * @param <RemindType> 支持的提醒数据类型
 *
 * @author f_ms
 * @date 2019/3/25
 */
public interface RemindRepo<RemindType extends Remind> {

    /**
     * 获取所有徽标提醒
     */
    Collection<? extends RemindType> getAllReminds();

    /**
     * 获取完全匹配指定节点路径的徽标提醒
     * <p>
     * 例如有消息 'a', 'a>b' 和 'a>b>c', 查询路径为 'a>b', 则只有消息 'a>b' 能够被匹配
     *
     * @param nodePaths 用于匹配的节点路径集
     * @return 指定节点路径匹配到的徽标提醒
     * @see #removeMatchPathReminds(Collection)
     */
    Map<NodePath, ? extends Collection<? extends RemindType>> getMatchPathReminds(Collection<NodePath> nodePaths);

    /**
     * 获取匹配指定节点路径及其子路径的徽标提醒
     * <p>
     * 例如有消息 'a', 'a>b' 和 'a>b>c', 查询路径为 'a>b', 则消息 'a>b' 和 'a>b>c' 能够被匹配
     *
     * @param nodePaths 用于匹配的节点路径集
     * @return 指定路径集合匹配到的徽标提醒
     * @see #removeMatchPathSubReminds(Collection)
     */
    Map<NodePath, ? extends Collection<? extends RemindType>> getMatchPathSubReminds(Collection<NodePath> nodePaths);

    /**
     * 移除指定徽标提醒
     *
     * @param reminds 要移除的徽标提醒
     */
    void removeReminds(Iterable<? extends RemindType> reminds);

    /**
     * 移除完全匹配指定节点路径的徽标提醒
     *
     * @param nodePaths 用于匹配徽标提醒的节点路径
     * @return 被移除徽标提醒的数量
     * @see #getMatchPathReminds(Collection)
     */
    long removeMatchPathReminds(Collection<NodePath> nodePaths);

    /**
     * 移除匹配指定节点路径及子路径的徽标提醒
     *
     * @param nodePaths 用于匹配的节点路径集
     * @return 被移除徽标提醒的数量
     * @see #getMatchPathSubReminds(Collection)
     */
    long removeMatchPathSubReminds(Collection<NodePath> nodePaths);

    /**
     * 移除所有徽标提醒
     *
     * @return 被移除徽标提醒的数量
     */
    long removeAllReminds();

    /**
     * 设置用于监听徽标提醒数据改变的监听器
     */
    void setRemindDataChangedListener(RemindDataChangedListener<? super RemindType> listener);
}
