package ms.imf.redpoint.manager;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import ms.imf.redpoint.entity.NodePath;

public abstract class RemindHandler<RemindType extends Remind> {

    private final RemindHandlerManager<RemindType> remindController = RemindHandlerManager.instance();
    private final List<NodePath> paths = new LinkedList<>();

    public void setPath(NodePath... paths) {
        setPath(Arrays.asList(paths));
    }
    public void setPath(NodePath path) {
        if (path == null) {
            clearPath();
            return;
        }
        setPath(Collections.singletonList(path));
    }
    public void setPath(List<NodePath> paths) {
        this.paths.clear();
        addPath(paths);
    }
    public void addPath(NodePath path) {
        if (path == null) {
            return;
        }
        addPath(Collections.singletonList(path));
    }
    public void addPath(NodePath... paths) {
        addPath(Arrays.asList(paths));
    }
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

        if (attachedRepo()) {
            remindController.notifyRemindHandlerChanged(this);
        }
    }

    public void clearPath() {
        this.paths.clear();
        if (attachedRepo()) {
            remindController.notifyRemindHandlerChanged(this);
        }
    }

    public List<NodePath> getPaths() {
        return paths;
    }

    public boolean isPathsEmpty() {
        return paths.isEmpty();
    }

    public boolean attachedRepo() {
        return remindController.remindHandlerAttached(this);
    }

    public void attachRepo() {
        remindController.addRemindHandler(this);
    }

    public void detachRepo() {
        remindController.removeRemindHandler(this);
    }

    public abstract void showReminds(List<? extends RemindType> reminds);

    public void onHappend() {
        remindController.happenedRemindHandler(this);
    }

    public void onHappednAll() {
        remindController.happenedRemindHandlerWithSubNodeAll(this);
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
