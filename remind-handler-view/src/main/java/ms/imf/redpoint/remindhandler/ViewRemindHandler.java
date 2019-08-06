package ms.imf.redpoint.remindhandler;

import android.os.Build;
import android.util.Log;
import android.view.View;

import ms.imf.redpoint.manager.Remind;
import ms.imf.redpoint.manager.RemindHandler;
import ms.imf.redpoint.manager.RemindHandlerManager;

/**
 * 一个通过{@link View#addOnAttachStateChangeListener(View.OnAttachStateChangeListener)}管理自己attach/detach生命周期的RemindHandler
 * <p>
 * 使用此RemindHandler你无需再调用{@link RemindHandler#attachToManager()}和{@link RemindHandler#detachFromManager()}方法
 *
 * @see View#isAttachedToWindow()
 * @see View#addOnAttachStateChangeListener(View.OnAttachStateChangeListener)
 *
 * @author f_ms
 * @date 2019/07/25
 */
public abstract class ViewRemindHandler<RemindType extends Remind, BadgeView extends View> extends RemindHandler<RemindType> {

    private static final String TAG = ViewRemindHandler.class.getSimpleName();

    private final BadgeView badgeView;

    /**
     * @param remindHandleManager see {@link RemindHandler}
     * @param badgeView           badgeView
     * @see RemindHandler
     */
    protected ViewRemindHandler(RemindHandlerManager<RemindType> remindHandleManager, BadgeView badgeView) {
        super(remindHandleManager);

        if (badgeView == null) {
            throw new IllegalArgumentException("badgeView can't be null");
        }

        this.badgeView = badgeView;

        if (isAttachedToWindow(badgeView)) {
            ViewRemindHandler.super.attachToManager();
        }

        badgeView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                ViewRemindHandler.super.attachToManager();
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                ViewRemindHandler.super.detachFromManager();
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
