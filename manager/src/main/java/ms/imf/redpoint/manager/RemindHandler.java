package ms.imf.redpoint.manager;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import ms.imf.redpoint.entity.NodePath;

/**
 * 提醒消息处理器，用于管理指定节点路径消息的展示、触发
 *
 * @param <RemindType> 支持的消息数据类型
 * @author f_ms
 */
public abstract class RemindHandler<RemindType extends Remind> {

    private final RemindHandlerManager<RemindType> remindHandleManager = RemindHandlerManager.instance();
    private final List<NodePath> paths = new LinkedList<>();

    /**
     * 依附到RemindHandlerManager
     */
    public void attachToManager() {
        remindHandleManager.addRemindHandler(this);
    }

    /**
     * 从RemindHandlerManager解除依附关系
     */
    public void detachToManager() {
        remindHandleManager.removeRemindHandler(this);
    }

    /**
     * 已经附加到RemindHandlerManager吗？
     *
     * @return true == 是的, false == 不，还没
     */
    public boolean isAttachedManager() {
        return remindHandleManager.remindHandlerAttached(this);
    }

    /**
     * 设置支持的节点路径
     *
     * @param paths 节点路径
     */
    public void setPath(NodePath... paths) {
        setPath(Arrays.asList(paths));
    }

    /**
     * 设置支持的节点路径
     * @param path 节点路径
     */
    public void setPath(NodePath path) {
        if (path == null) {
            clearPath();
            return;
        }
        setPath(Collections.singletonList(path));
    }

    /**
     * 设置支持的节点路径
     * @param paths 节点路径
     */
    public void setPath(List<NodePath> paths) {
        this.paths.clear();
        addPath(paths);
    }

    /**
     * 添加支持的节点路径
     * @param path 节点路径
     */
    public void addPath(NodePath path) {
        if (path == null) {
            return;
        }
        addPath(Collections.singletonList(path));
    }

    /**
     * 添加支持的节点路径
     * @param paths 节点路径
     */
    public void addPath(NodePath... paths) {
        addPath(Arrays.asList(paths));
    }

    /**
     * 添加支持的节点路径
     * @param paths 节点路径
     */
    public void addPath(List<NodePath> paths) {
        if (paths == null) { throw new IllegalArgumentException("paths can't be null"); }
        if (paths.isEmpty()) {
            return;
        }

        int nullIndex = paths.indexOf(null);
        if (nullIndex >= 0) {
            throw new IllegalArgumentException("paths can't contain null value '" + nullIndex +  "'");
        }

        this.paths.addAll(paths);

        if (isAttachedManager()) {
            remindHandleManager.notifyRemindHandlerChanged(this);
        }
    }

    /**
     * 有支持的节点路径吗？
     *
     * @return true == 有的, false == 没有
     */
    public boolean isPathsEmpty() {
        return paths.isEmpty();
    }

    /**
     * 清空支持的节点路径
     */
    public void clearPath() {
        this.paths.clear();
        if (isAttachedManager()) {
            remindHandleManager.notifyRemindHandlerChanged(this);
        }
    }

    /**
     * 获取支持的节点路径
     * @return 支持的节点路径
     */
    public List<NodePath> getPaths() {
        return paths;
    }

    /**
     * 展示消息
     *
     * @param reminds 要展示的消息
     */
    protected abstract void showReminds(List<? extends RemindType> reminds);

    /**
     * 当消息处理器被触发，触发范围为支持的节点路径
     */
    public void onHappend() {
        remindHandleManager.happenedRemindHandler(this);
    }

    /**
     * 当消息处理器被触发，触发范围为消息处理器支持的节点路径及其的子路径
     * <p>
     * 一般用于消息被用户直接移除的情况，例如用户以拖拽的方式消除红点，此时子路径的消息也应该被清空
     */
    public void onHappednWithSubPath() {
        remindHandleManager.happenedRemindHandlerWithSubNodeAll(this);
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return "RemindHandler{" +
                "paths=" + paths +
                '}';
    }

}
