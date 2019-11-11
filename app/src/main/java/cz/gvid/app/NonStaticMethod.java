package cz.gvid.app;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.view.Display;
import android.view.Surface;

@SuppressWarnings("WeakerAccess")
public class NonStaticMethod {

    private Context context;
    private Activity activity;

    NonStaticMethod(Context context, Activity activity) {
        this.context = context;
        this.activity = activity;
    }

    public void setScreenOrientationUnlock() {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    public void setScreenOrientationLock() {
        Display display = activity.getWindowManager().getDefaultDisplay();
        int rotation = display.getRotation();

        Point size = new Point();
        display.getSize(size);

        int lock;

        if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
            // if rotation is 0 or 180 and width is greater than height, we have
            // a tablet
            if (size.x > size.y) {
                if (rotation == Surface.ROTATION_0) {
                    lock = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                } else {
                    lock = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                }
            } else {
                // we have a phone
                if (rotation == Surface.ROTATION_0) {
                    lock = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                } else {
                    lock = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                }
            }
        } else {
            // if rotation is 90 or 270 and width is greater than height, we
            // have a phone
            if (size.x > size.y) {
                if (rotation == Surface.ROTATION_90) {
                    lock = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                } else {
                    lock = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                }
            } else {
                // we have a tablet
                if (rotation == Surface.ROTATION_90) {
                    lock = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                } else {
                    lock = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                }
            }
        }
        activity.setRequestedOrientation(lock);
    }
}
