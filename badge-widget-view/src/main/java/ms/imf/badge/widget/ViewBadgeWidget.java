package ms.imf.badge.widget;

import android.os.Build;
import android.util.Log;
import android.view.View;

import ms.imf.badge.manager.Remind;
import ms.imf.badge.manager.BadgeWidget;
import ms.imf.badge.manager.BadgeManager;

/**
 * 一个通过{@link View#addOnAttachStateChangeListener(View.OnAttachStateChangeListener)}管理自己attach/detach生命周期的{@link BadgeWidget}
 * <p>
 * 开发者无需再手动调用{@link BadgeWidget#attachToManager()}和{@link BadgeWidget#detachFromManager()}方法
 *
 * @see View#isAttachedToWindow()
 * @see View#addOnAttachStateChangeListener(View.OnAttachStateChangeListener)
 *
 * @author f_ms
 * @date 2019/07/25
 */
public abstract class ViewBadgeWidget<RemindType extends Remind, BadgeView extends View> extends BadgeWidget<RemindType> {

    private static final String TAG = ViewBadgeWidget.class.getSimpleName();

    private final BadgeView badgeView;

    /**
     * @param badgeManager see {@link BadgeWidget}
     * @param badgeView           badgeView
     * @see BadgeWidget
     */
    protected ViewBadgeWidget(BadgeManager<RemindType> badgeManager, BadgeView badgeView) {
        super(badgeManager);

        if (badgeView == null) {
            throw new IllegalArgumentException("badgeView can't be null");
        }

        this.badgeView = badgeView;

        if (isAttachedToWindow(badgeView)) {
            ViewBadgeWidget.super.attachToManager();
        }

        badgeView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                ViewBadgeWidget.super.attachToManager();
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                ViewBadgeWidget.super.detachFromManager();
            }
        });
    }

    public BadgeView badgeView() {
        return badgeView;
    }

    @Override
    public void attachToManager() {
        // super.attachToManager();
        Log.i(TAG, "I don't need you manage my lifecycle, I can do it myself.");
    }

    @Override
    public void detachFromManager() {
        // super.detachFromManager();
        Log.i(TAG, "I don't need you manage my lifecycle, I can do it myself.");
    }

    /**
     * see https://developer.android.com/reference/android/support/v4/view/ViewCompat.html#isAttachedToWindow(android.view.View)
     */
    private static boolean isAttachedToWindow(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return view.isAttachedToWindow();
        } else {
            return view.getWindowToken() != null;
        }
    }
}
