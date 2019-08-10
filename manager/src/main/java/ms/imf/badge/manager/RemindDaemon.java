package ms.imf.badge.manager;

/**
 * 徽标提醒数据守护者
 * <p>
 * 简单定义守护者的常用状态控制行为，用于与后端持续性对徽标提醒数据进行交互时对守护者的状态控制
 *
 * @author f_ms
 * @date 2019/5/29
 */
public interface RemindDaemon {

    /**
     * 启动
     */
    void start();

    /**
     * 停止
     */
    void stop();

    /**
     * 启动了吗？
     * @return true == 是的，已经启动了, false == 不，没有启动
     */
    boolean started();

    /**
     * 进入正常工作状态
     */
    void resume();

    /**
     * 暂停工作
     */
    void pause();

    /**
     * 在正常工作的状态吗？
     * @return true == 是的, false == 不，不是
     */
    boolean resumed();

    /**
     * 刷新、同步提醒数据
     */
    void refresh();

}
