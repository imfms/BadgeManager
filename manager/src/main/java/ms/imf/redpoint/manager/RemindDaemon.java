package ms.imf.redpoint.manager;

/**
 * 消息获取守护着
 * <p>
 * 对消息守护者常用方法的基本定义，消息守护者用于在后端持续性的获取消息的变更并同步到消息仓库
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
     * 刷新数据、同步消息
     */
    void refresh();

}
