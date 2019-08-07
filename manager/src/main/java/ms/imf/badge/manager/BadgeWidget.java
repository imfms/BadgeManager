package ms.imf.badge.manager;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ms.imf.badge.entity.NodePath;

/**
 * 提醒消息处理器，用于管理指定节点路径消息的展示、消费
 *
 * @param <RemindType> 支持的消息数据类型
 * @author f_ms
 */
public abstract class BadgeWidget<RemindType extends Remind> {

    private final BadgeManager<RemindType> badgeManager;
    private final Set<NodePath> paths = new HashSet<>();

    /**
     * 由于{@link BadgeWidget}在程序中为高频使用的类，所以本构造的{@link BadgeManager}参数的指定会挺麻烦
     * <p>
     * 所以推荐开发者继承此类时重写构造方法并对该参数进行封装，直接指向一个可以获取到{@link BadgeManager}的固定方式
     * <p>
     * 之所以没有对推荐进行封装是为了不限制开发者的使用场景，可能开发者会有两套不同消息机制同时存在的情况
     *
     * @param badgeManager 要依附的{@link BadgeManager}
     */
    protected BadgeWidget(BadgeManager<RemindType> badgeManager) {
        if (badgeManager == null) {
            throw new IllegalArgumentException("badgeManager can't be null");
        }
        this.badgeManager = badgeManager;
    }

    /**
     * 依附到{@link BadgeManager}
     */
    public void attachToManager() {
        badgeManager.attachWidget(this);
    }

    /**
     * 从{@link BadgeManager}解除依附关系
     */
    public void detachFromManager() {
        badgeManager.detachWidget(this);
    }

    /**
     * 已经附加到{@link BadgeManager}了吗？
     *
     * @return true == 是的, false == 不，还没
     */
    public boolean isAttachedManager() {
        return badgeManager.widgetAttached(this);
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
            badgeManager.notifyWidgetChanged(this);
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
            badgeManager.notifyWidgetChanged(this);
        }
    }

    /**
     * 获取支持的节点路径
     * @return 支持的节点路径
     */
    public Set<NodePath> getPaths() {
        return paths;
    }

    /**
     * 展示消息
     *
     * @param reminds 要展示的消息
     */
    public abstract void showReminds(Collection<? extends RemindType> reminds);

    /**
     * 当消息处理器被消费，消费范围为支持的节点路径
     */
    public void onHappend() {
        badgeManager.happenedWidget(this);
    }

    /**
     * 当消息处理器被消费，消费范围为支持的节点路径及其的子路径
     * <p>
     * 一般用于消息被用户直接移除的情况，例如用户以拖拽的方式消除红点，此时子路径的消息也应该被清空
     */
    public void onHappednWithSubPath() {
        badgeManager.happenedWidgetWithSubPath(this);
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
        return "BadgeWidget{" +
                "paths=" + paths +
                '}';
    }

}
