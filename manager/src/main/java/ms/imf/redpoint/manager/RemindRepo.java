package ms.imf.redpoint.manager;

import java.util.Collection;

import ms.imf.redpoint.entity.NodePath;

/**
 * 提醒数据仓库，提供提醒数据的读写存储功能
 *
 * @param <RemindType> 支持的提醒数据类型
 *
 * @author f_ms
 * @date 2019/3/25
 */
public interface RemindRepo<RemindType extends Remind> {

    /**
     * 获取所有提醒数据
     *
     * @return 提醒数据
     */
    Collection<? extends RemindType> getAllReminds();

    /**
     * 获取完全匹配指定节点路径的提醒数据
     *
     * @param nodePath 匹配的节点路径
     * @return 匹配的提醒数据
     */
    Collection<? extends RemindType> getMatchReminds(NodePath nodePath);

    /**
     * 获取匹配指定节点路径及其子路径的提醒数据
     *
     * @param nodePath 节点路径
     * @return 匹配的提醒数据
     */
    Collection<? extends RemindType> getMatchSubReminds(NodePath nodePath);

    /**
     * 移除指定提醒
     *
     * @param remind 要移除的提醒
     */
    void removeRemind(RemindType remind);

    /**
     * 移除指定提醒列表
     *
     * @param reminds 要移除的提醒列表
     */
    void removeReminds(Iterable<? extends RemindType> reminds);

    /**
     * 移除完全匹配指定节点路径的提醒
     *
     * @param nodePath 节点路径
     * @return 移除提醒的数量
     */
    long removeMatchReminds(NodePath nodePath);

    /**
     * 移除匹配指定节点路径及子路径的提醒
     *
     * @param nodePath 节点路径
     * @return 移除提醒的数量
     */
    long removeMatchSubReminds(NodePath nodePath);

    /**
     * 移除所有提醒
     *
     * @return 移除提醒的数量
     */
    long removeAllReminds();

    /**
     * 设置提醒数据改变改变监听器
     *
     * @param listener 要设置的提醒数据改变监听器
     */
    void setRemindDataChangedListener(RemindDataChangedListener<? super RemindType> listener);
}
