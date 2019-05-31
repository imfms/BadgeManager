package ms.imf.redpoint.manager;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import ms.imf.redpoint.entity.Node;
import ms.imf.redpoint.entity.NodePath;


public class RemindHandlerControlCenter {

    private static volatile RemindHandlerControlCenter INSTANCE;

    public static void init(RemindRepo repo) {
        if (INSTANCE == null) {
            synchronized (RemindHandlerControlCenter.class) {
                if (INSTANCE == null) {
                    INSTANCE = new RemindHandlerControlCenter(repo);
                }
            }
        }
    }

    public static RemindHandlerControlCenter instance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("please init " + RemindHandlerControlCenter.class.getSimpleName() + " first");
        }
        return INSTANCE;
    }

    private final RemindRepo mRemindRepo;
    private final Set<RemindHandler> remindHandlers = new LinkedHashSet<>();

    public RemindHandlerControlCenter(RemindRepo repo) {
        if (repo == null) { throw new IllegalArgumentException("repo can't be null"); }
        mRemindRepo = repo;
        repo.setRepoRemindChangedListener(new RemindChangedListener() {
            @Override
            public void onRepoRemindChanged() {
                notifyRemindchanged();
            }
        });
    }

    public void addRemindHandler(RemindHandler handler) {
        if (handler == null) { throw new IllegalArgumentException("handler can't be null"); }
        if (remindHandlers.contains(handler)) {
            return;
        }
        remindHandlers.add(handler);
        notifyRemindchanged(handler);
    }

    public void removeRemindHandler(RemindHandler handler) {
        if (handler == null) {
            return;
        }
        remindHandlers.remove(handler);
    }

    public void happened(RemindHandler handler) {

        if (!remindHandlerAttached(handler)) {
            return;
        }

        List<Remind> handledReminds = new LinkedList<>();

        List<Remind> handlerReminds = getReminds(handler);

        for (NodePath happenedPath : handler.getPaths()) {
            Iterator<Remind> iterator = handlerReminds.iterator();
            while (iterator.hasNext()) {
                Remind remind = iterator.next();
                if (isMyParentPathWithMe(happenedPath, remind.path)) {
                    handledReminds.add(remind);
                    iterator.remove();
                }
            }
        }

        mRemindRepo.handledRemind(handledReminds);
        notifyRemindchanged();
    }

    /**
     * 消费指定handler节点下所有消息(包括handler节点本身)
     */
    public void happededAll(RemindHandler handler) {
        if (!remindHandlerAttached(handler)) {
            return;
        }

        List<Remind> handledReminds = getReminds(handler);
        mRemindRepo.handledRemind(handledReminds);
        notifyRemindchanged();
    }

    public void notifyRemindchanged() {
        for (RemindHandler handler : remindHandlers) {
            notifyRemindchanged(handler);
        }
    }

    public void notifyRemindchanged(RemindHandler handler) {
        handler.showReminds(getReminds(handler));
    }

    public List<Remind> getReminds(RemindHandler remindHandler) {
        List<Remind> results = new LinkedList<>();

        for (Remind remind : mRemindRepo.getReminds()) {
            for (NodePath path : remindHandler.getPaths()) {
                if (isMySubPathWithMe(path, remind.path)) {
                    results.add(remind);
                    break;
                }
            }
        }

        return results;
    }

    public boolean remindHandlerAttached(RemindHandler handler) {
        if (handler == null) {
            return false;
        }
        return remindHandlers.contains(handler);
    }

    private boolean isMySubPathWithMe(NodePath me, NodePath him) {
        if (him.nodes().size() < me.nodes().size()) {
            return false;
        }

        for (int i = 0; i < me.nodes().size(); i++) {
            Node myNode = me.nodes().get(i);
            Node hisNode = him.nodes().get(i);

            if (!hisNode.equals(myNode)) {
                return false;
            }
        }

        return true;
    }

    private boolean isMyParentPathWithMe(NodePath me, NodePath him) {
        if (him.nodes().size() > me.nodes().size()) {
            return false;
        }

        for (int i = 0; i < him.nodes().size(); i++) {
            Node myNode = me.nodes().get(i);
            Node hisNode = him.nodes().get(i);

            if (!hisNode.equals(myNode)) {
                return false;
            }
        }

        return true;
    }

}
