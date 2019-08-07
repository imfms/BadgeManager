package ms.imf.badge.manager;

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

import ms.imf.badge.entity.NodePath;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TreeModeBadgeWidgetManagerTest {

    private static class BadgeWidget extends ms.imf.badge.manager.BadgeWidget<Remind> {
        BadgeWidget(BadgeManager<Remind> badgegManager) { super(badgegManager); }
        @Override public void showReminds(Collection<? extends Remind> reminds) {}
    }

    @Mock
    RemindRepo<Remind> repo;

    private TreeModeBadgeManager<Remind> manager;
    private BadgeWidget widget;

    @Before
    public void setUp() {
        manager = new TreeModeBadgeManager<>(repo);
        widget = spy(new BadgeWidget(manager));
    }

    @Test
    public void attachWidget() {

        assertThat(
                manager.widgetAttached(widget),
                is(false)
        );

        manager.attachWidget(widget);

        assertThat(
                manager.widgetAttached(widget),
                is(true)
        );

        // 有path的widget附加到manager后会被刷新数据
        BadgeWidget widget2 = new BadgeWidget(manager);
        NodePath widget2Path = NodePath.instance("a", "b");
        widget2.setPath(widget2Path);
        manager.attachWidget(widget2);
        verify(repo).getMatchPathSubReminds(Collections.singleton(NodePath.instance("a", "b")));
    }

    @Test
    public void detachWidget() {

        manager.attachWidget(widget);
        assertThat(
                manager.widgetAttached(widget),
                is(true)
        );

        manager.detachWidget(widget);
        assertThat(
                manager.widgetAttached(widget),
                is(false)
        );
    }

    @Test
    public void widgetAttached() {
        assertThat(
                manager.widgetAttached(widget),
                is(false)
        );

        manager.attachWidget(widget);
        assertThat(
                manager.widgetAttached(widget),
                is(true)
        );

        manager.detachWidget(widget);
        assertThat(
                manager.widgetAttached(widget),
                is(false)
        );
    }

    @Test
    public void notifyWidgetChanged() {
        NodePath pathAb = NodePath.instance("a", "b");
        NodePath pathAbc = NodePath.instance("a", "b", "c");
        NodePath pathAbcd = NodePath.instance("a", "b", "c", "d");
        NodePath pathAe = NodePath.instance("a", "e");

        widget.attachToManager();

        // widget setPath后manager应有查询path对应消息及调用widget展示行为
        reset(repo, widget);
        widget.setPath(pathAb);
        verify(repo).getMatchPathSubReminds(Collections.singleton(pathAb));
        verify(widget).showReminds(Collections.<Remind>emptyList());

        // widget set多个path后应查找重合path中最短的path查询消息及调用widget展示行为
        reset(repo, widget);
        when(repo.getMatchPathSubReminds(any(Collection.class)))
                .then(new Returns(Collections.singletonMap(pathAb, Arrays.asList(
                        new Remind(pathAb),
                        new Remind(pathAbc),
                        new Remind(pathAbcd)
                ))));

        widget.setPath(Arrays.asList(
                pathAb, pathAbc, pathAe
        ));
        verify(repo).getMatchPathSubReminds(new HashSet<>(Arrays.asList(pathAb, pathAe)));
        verify(widget).showReminds(Arrays.asList(
                new Remind(pathAb),
                new Remind(pathAbc),
                new Remind(pathAbcd)
        ));

        // manager调用widget.showReminds应提供给其支持的remind
        reset(repo, widget);
        when(repo.getMatchPathSubReminds(ArgumentMatchers.<NodePath>anyCollection()))
                .then(new Returns(Collections.singletonMap(pathAbc, Arrays.asList(
                        new Remind(pathAb),
                        new Remind(pathAbc),
                        new Remind(pathAbcd)
                ))));
        widget.setPath(pathAbc);
        verify(repo).getMatchPathSubReminds(Collections.singleton(pathAbc));
        verify(widget).showReminds(Arrays.asList(
                new Remind(pathAbc),
                new Remind(pathAbcd)
        ));
    }

    @Test
    public void happenedWidget() {
        NodePath pathAb = NodePath.instance("a", "b");
        NodePath pathAbc = NodePath.instance("a", "b", "c");
        NodePath pathAbcd = NodePath.instance("a", "b", "c", "d");

        BadgeWidget widgetAb = spy(new BadgeWidget(manager));
        widgetAb.setPath(pathAb);
        widgetAb.attachToManager();

        BadgeWidget widgetAbc = spy(new BadgeWidget(manager));
        widgetAbc.setPath(pathAbc);
        widgetAbc.attachToManager();

        // widget happened后manager应从repo移除其path对应的消息
        reset(repo);
        when(repo.getMatchPathSubReminds(widgetAb.getPaths()))
                .then(new Returns(Collections.singletonMap(pathAb, Arrays.asList(
                        new Remind(pathAb),
                        new Remind(pathAbcd)
                ))));
        when(repo.removeMatchPathReminds(widgetAbc.getPaths())).thenReturn(1L);

        manager.happenedWidget(widgetAbc);

        // 消息被移除后将刷新路径上被影响的widget
        verify(repo).removeMatchPathReminds(widgetAbc.getPaths());
        verify(widgetAb).showReminds(Arrays.asList(
                new Remind(pathAb),
                new Remind(pathAbcd)
        ));
    }

    @Test
    public void happenedWidgetWithSubPathAll() {
        NodePath pathAb = NodePath.instance("a", "b");
        NodePath pathAbc = NodePath.instance("a", "b", "c");
        NodePath pathAbcd = NodePath.instance("a", "b", "c", "d");

        BadgeWidget widgetAb = spy(new BadgeWidget(manager));
        widgetAb.setPath(pathAb);
        widgetAb.attachToManager();

        BadgeWidget widgetAbc = spy(new BadgeWidget(manager));
        widgetAbc.setPath(pathAbc);
        widgetAbc.attachToManager();

        // widget happened后manager应从repo移除其path对应的消息
        reset(repo);
        when(repo.getMatchPathSubReminds(widgetAb.getPaths()))
                .then(new Returns(Collections.singletonMap(pathAb, Arrays.asList(
                        new Remind(pathAb)
                ))));
        when(repo.removeMatchPathSubReminds(widgetAbc.getPaths())).thenReturn(1L);

        manager.happenedWidgetWithSubPath(widgetAbc);

        // 消息被移除后将刷新路径上被影响的widget
        verify(repo).removeMatchPathSubReminds(widgetAbc.getPaths());
        verify(widgetAb).showReminds(Arrays.asList(
                new Remind(pathAb)
        ));
    }

    @Test
    public void notifyRemindDataChanged() {
        NodePath pathAb = NodePath.instance("a", "b");
        NodePath pathAbc = NodePath.instance("a", "b", "c");
        NodePath pathAbcd = NodePath.instance("a", "b", "c", "d");
        NodePath pathAbe = NodePath.instance("a", "b", "e");

        BadgeWidget widgetAb = spy(new BadgeWidget(manager));
        widgetAb.setPath(pathAb);
        widgetAb.attachToManager();

        BadgeWidget widgetAbc = spy(new BadgeWidget(manager));
        widgetAbc.setPath(pathAbc);
        widgetAbc.attachToManager();

        BadgeWidget widgetAbe = spy(new BadgeWidget(manager));
        widgetAbe.setPath(pathAbe);
        widgetAbe.attachToManager();

        BadgeWidget[] widgets = {widgetAb, widgetAbc, widgetAbe};

        reset(repo);
        reset(widgets);
        when(repo.getMatchPathSubReminds(ArgumentMatchers.<NodePath>anyCollection()))
                .then(new Returns(Collections.singletonMap(pathAb, Arrays.asList(
                        new Remind(pathAb),
                        new Remind(pathAbc),
                        new Remind(pathAbcd),
                        new Remind(pathAbe)
                ))));

        manager.notifyRemindDataChanged();

        for (BadgeWidget widget : widgets) {
            verify(widget).showReminds(ArgumentMatchers.<Remind>anyCollection());
        }
    }

    @Test
    public void notifyRemindDataChangedWithChangedReminds() {
        NodePath pathAb = NodePath.instance("a", "b");
        NodePath pathAbc = NodePath.instance("a", "b", "c");
        NodePath pathAbcd = NodePath.instance("a", "b", "c", "d");
        NodePath pathAbe = NodePath.instance("a", "b", "e");

        BadgeWidget widgetAb = spy(new BadgeWidget(manager));
        widgetAb.setPath(pathAb);
        widgetAb.attachToManager();

        BadgeWidget widgetAbc = spy(new BadgeWidget(manager));
        widgetAbc.setPath(pathAbc);
        widgetAbc.attachToManager();

        BadgeWidget widgetAbe = spy(new BadgeWidget(manager));
        widgetAbe.setPath(pathAbe);
        widgetAbe.attachToManager();

        BadgeWidget[] widgets = {widgetAb, widgetAbc, widgetAbe};

        reset(repo);
        reset(widgets);
        when(repo.getMatchPathSubReminds(ArgumentMatchers.<NodePath>anyCollection()))
                .then(new Returns(Collections.singletonMap(pathAb, Arrays.asList(
                        new Remind(pathAb),
                        new Remind(pathAbc),
                        new Remind(pathAbcd),
                        new Remind(pathAbe)
                ))));

        manager.notifyRemindDataChanged(Arrays.asList(
                new Remind(pathAbcd)
        ));
        verify(widgetAb).showReminds(ArgumentMatchers.<Remind>anyCollection());
        verify(widgetAbc).showReminds(ArgumentMatchers.<Remind>anyCollection());
        verify(widgetAbe, never()).showReminds(ArgumentMatchers.<Remind>anyCollection());

        reset(repo);
        reset(widgets);
        manager.notifyRemindDataChanged(Arrays.asList(
                new Remind(pathAbe)
        ));
        verify(widgetAb).showReminds(ArgumentMatchers.<Remind>anyCollection());
        verify(widgetAbc, never()).showReminds(ArgumentMatchers.<Remind>anyCollection());
        verify(widgetAbe).showReminds(ArgumentMatchers.<Remind>anyCollection());


        reset(repo);
        reset(widgets);
        manager.notifyRemindDataChanged(Arrays.asList(
                new Remind(pathAbe),
                new Remind(pathAbcd)
        ));
        verify(widgetAb).showReminds(ArgumentMatchers.<Remind>anyCollection());
        verify(widgetAbc).showReminds(ArgumentMatchers.<Remind>anyCollection());
        verify(widgetAbe).showReminds(ArgumentMatchers.<Remind>anyCollection());
    }
}