package ms.imf.redpoint.manager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.internal.stubbing.answers.Returns;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import ms.imf.redpoint.entity.NodePath;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TreeModeRemindHandlerManagerTest {

    private static class RemindHandler extends ms.imf.redpoint.manager.RemindHandler<Remind> {
        RemindHandler(RemindHandlerManager<Remind> remindHandleManager) { super(remindHandleManager); }
        @Override public void showReminds(Collection<? extends Remind> reminds) {}
    }

    @Mock RemindRepo<Remind> repo;

    private TreeModeRemindHandlerManager<Remind> manager;
    private RemindHandler handler;

    @Before
    public void setUp() {
        manager = new TreeModeRemindHandlerManager<>(repo);
        handler = spy(new RemindHandler(manager));
    }

    @Test
    public void attachRemindHandler() {

        assertThat(
                manager.remindHandlerAttached(handler),
                is(false)
        );

        manager.attachRemindHandler(handler);

        assertThat(
                manager.remindHandlerAttached(handler),
                is(true)
        );

        // 有path的handler附加到manager后会被刷新数据
        RemindHandler handler2 = new RemindHandler(manager);
        NodePath handler2Path = NodePath.instance("a", "b");
        handler2.setPath(handler2Path);
        manager.attachRemindHandler(handler2);
        verify(repo).getMatchSubReminds(Collections.singleton(NodePath.instance("a", "b")));
    }

    @Test
    public void detachRemindHandler() {

        manager.attachRemindHandler(handler);
        assertThat(
                manager.remindHandlerAttached(handler),
                is(true)
        );

        manager.detachRemindHandler(handler);
        assertThat(
                manager.remindHandlerAttached(handler),
                is(false)
        );
    }

    @Test
    public void remindHandlerAttached() {
        assertThat(
                manager.remindHandlerAttached(handler),
                is(false)
        );

        manager.attachRemindHandler(handler);
        assertThat(
                manager.remindHandlerAttached(handler),
                is(true)
        );

        manager.detachRemindHandler(handler);
        assertThat(
                manager.remindHandlerAttached(handler),
                is(false)
        );
    }

    @Test
    public void notifyRemindHandlerChanged() {
        NodePath pathAb = NodePath.instance("a", "b");
        NodePath pathAbc = NodePath.instance("a", "b", "c");
        NodePath pathAbcd = NodePath.instance("a", "b", "c", "d");
        NodePath pathAe = NodePath.instance("a", "e");

        handler.attachToManager();

        // handler setPath后manager应有查询path对应消息及调用handler展示行为
        reset(repo, handler);
        handler.setPath(pathAb);
        verify(repo).getMatchSubReminds(Collections.singleton(pathAb));
        verify(handler).showReminds(Collections.<Remind>emptyList());

        // handler set多个path后应查找重合path中最短的path查询消息及调用handler展示行为
        reset(repo, handler);
        when(repo.getMatchSubReminds(any(Collection.class)))
                .then(new Returns(Collections.singletonMap(pathAb, Arrays.asList(
                        new Remind(pathAb),
                        new Remind(pathAbc),
                        new Remind(pathAbcd)
                ))));

        handler.setPath(Arrays.asList(
                pathAb, pathAbc, pathAe
        ));
        verify(repo).getMatchSubReminds(new HashSet<>(Arrays.asList(pathAb, pathAe)));
        verify(handler).showReminds(Arrays.asList(
                new Remind(pathAb),
                new Remind(pathAbc),
                new Remind(pathAbcd)
        ));

        // manager调用handler.showReminds应提供给其支持的remind
        reset(repo, handler);
        when(repo.getMatchSubReminds(ArgumentMatchers.<NodePath>anyCollection()))
                .then(new Returns(Collections.singletonMap(pathAbc, Arrays.asList(
                        new Remind(pathAb),
                        new Remind(pathAbc),
                        new Remind(pathAbcd)
                ))));
        handler.setPath(pathAbc);
        verify(repo).getMatchSubReminds(Collections.singleton(pathAbc));
        verify(handler).showReminds(Arrays.asList(
                new Remind(pathAbc),
                new Remind(pathAbcd)
        ));
    }

    @Test
    public void happenedRemindHandler() {
        NodePath pathAb = NodePath.instance("a", "b");
        NodePath pathAbc = NodePath.instance("a", "b", "c");
        NodePath pathAbcd = NodePath.instance("a", "b", "c", "d");

        RemindHandler handlerAb = spy(new RemindHandler(manager));
        handlerAb.setPath(pathAb);
        handlerAb.attachToManager();

        RemindHandler handlerAbc = spy(new RemindHandler(manager));
        handlerAbc.setPath(pathAbc);
        handlerAbc.attachToManager();

        // handler happened后manager应从repo移除其path对应的消息
        reset(repo);
        when(repo.getMatchSubReminds(handlerAb.getPaths()))
                .then(new Returns(Collections.singletonMap(pathAb, Arrays.asList(
                        new Remind(pathAb),
                        new Remind(pathAbcd)
                ))));
        when(repo.removeMatchReminds(handlerAbc.getPaths())).thenReturn(1L);

        manager.happenedRemindHandler(handlerAbc);

        // 消息被移除后将刷新路径上被影响的handler
        verify(repo).removeMatchReminds(handlerAbc.getPaths());
        verify(handlerAb).showReminds(Arrays.asList(
                new Remind(pathAb),
                new Remind(pathAbcd)
        ));
    }

    @Test
    public void happenedRemindHandlerWithSubPathAll() {
        NodePath pathAb = NodePath.instance("a", "b");
        NodePath pathAbc = NodePath.instance("a", "b", "c");
        NodePath pathAbcd = NodePath.instance("a", "b", "c", "d");

        RemindHandler handlerAb = spy(new RemindHandler(manager));
        handlerAb.setPath(pathAb);
        handlerAb.attachToManager();

        RemindHandler handlerAbc = spy(new RemindHandler(manager));
        handlerAbc.setPath(pathAbc);
        handlerAbc.attachToManager();

        // handler happened后manager应从repo移除其path对应的消息
        reset(repo);
        when(repo.getMatchSubReminds(handlerAb.getPaths()))
                .then(new Returns(Collections.singletonMap(pathAb, Arrays.asList(
                        new Remind(pathAb)
                ))));
        when(repo.removeMatchSubReminds(handlerAbc.getPaths())).thenReturn(1L);

        manager.happenedRemindHandlerWithSubPathAll(handlerAbc);

        // 消息被移除后将刷新路径上被影响的handler
        verify(repo).removeMatchSubReminds(handlerAbc.getPaths());
        verify(handlerAb).showReminds(Arrays.asList(
                new Remind(pathAb)
        ));
    }

    @Test
    public void notifyRemindDataChanged() {
        NodePath pathAb = NodePath.instance("a", "b");
        NodePath pathAbc = NodePath.instance("a", "b", "c");
        NodePath pathAbcd = NodePath.instance("a", "b", "c", "d");
        NodePath pathAbe = NodePath.instance("a", "b", "e");

        RemindHandler handlerAb = spy(new RemindHandler(manager));
        handlerAb.setPath(pathAb);
        handlerAb.attachToManager();

        RemindHandler handlerAbc = spy(new RemindHandler(manager));
        handlerAbc.setPath(pathAbc);
        handlerAbc.attachToManager();

        RemindHandler handlerAbe = spy(new RemindHandler(manager));
        handlerAbe.setPath(pathAbe);
        handlerAbe.attachToManager();

        RemindHandler[] handlers = {handlerAb, handlerAbc, handlerAbe};

        reset(repo);
        reset(handlers);
        when(repo.getMatchSubReminds(ArgumentMatchers.<NodePath>anyCollection()))
                .then(new Returns(Collections.singletonMap(pathAb, Arrays.asList(
                        new Remind(pathAb),
                        new Remind(pathAbc),
                        new Remind(pathAbcd),
                        new Remind(pathAbe)
                ))));

        manager.notifyRemindDataChanged();

        for (RemindHandler handler : handlers) {
            verify(handler).showReminds(ArgumentMatchers.<Remind>anyCollection());
        }
    }

    @Test
    public void notifyRemindDataChangedWithChangedReminds() {
        NodePath pathAb = NodePath.instance("a", "b");
        NodePath pathAbc = NodePath.instance("a", "b", "c");
        NodePath pathAbcd = NodePath.instance("a", "b", "c", "d");
        NodePath pathAbe = NodePath.instance("a", "b", "e");

        RemindHandler handlerAb = spy(new RemindHandler(manager));
        handlerAb.setPath(pathAb);
        handlerAb.attachToManager();

        RemindHandler handlerAbc = spy(new RemindHandler(manager));
        handlerAbc.setPath(pathAbc);
        handlerAbc.attachToManager();

        RemindHandler handlerAbe = spy(new RemindHandler(manager));
        handlerAbe.setPath(pathAbe);
        handlerAbe.attachToManager();

        RemindHandler[] handlers = {handlerAb, handlerAbc, handlerAbe};

        reset(repo);
        reset(handlers);
        when(repo.getMatchSubReminds(ArgumentMatchers.<NodePath>anyCollection()))
                .then(new Returns(Collections.singletonMap(pathAb, Arrays.asList(
                        new Remind(pathAb),
                        new Remind(pathAbc),
                        new Remind(pathAbcd),
                        new Remind(pathAbe)
                ))));

        manager.notifyRemindDataChanged(Arrays.asList(
                new Remind(pathAbcd)
        ));
        verify(handlerAb).showReminds(ArgumentMatchers.<Remind>anyCollection());
        verify(handlerAbc).showReminds(ArgumentMatchers.<Remind>anyCollection());
        verify(handlerAbe, never()).showReminds(ArgumentMatchers.<Remind>anyCollection());

        reset(repo);
        reset(handlers);
        manager.notifyRemindDataChanged(Arrays.asList(
                new Remind(pathAbe)
        ));
        verify(handlerAb).showReminds(ArgumentMatchers.<Remind>anyCollection());
        verify(handlerAbc, never()).showReminds(ArgumentMatchers.<Remind>anyCollection());
        verify(handlerAbe).showReminds(ArgumentMatchers.<Remind>anyCollection());


        reset(repo);
        reset(handlers);
        manager.notifyRemindDataChanged(Arrays.asList(
                new Remind(pathAbe),
                new Remind(pathAbcd)
        ));
        verify(handlerAb).showReminds(ArgumentMatchers.<Remind>anyCollection());
        verify(handlerAbc).showReminds(ArgumentMatchers.<Remind>anyCollection());
        verify(handlerAbe).showReminds(ArgumentMatchers.<Remind>anyCollection());
    }
}