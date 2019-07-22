package ms.imf.redpoint.manager;

/**
 * 红点消息获取、状态处理器
 *
 * @author f_ms
 * @date 2019/5/29
 */
public interface RemindDaemon {

    void start();
    void stop();
    boolean started();
    void resume();
    void pause();
    boolean resumed();

    void refresh();

}
