package com.gear2cam.official;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * Created by varun on 14/5/14.
 */
public class RotationAwareImageView extends ImageView {
    OrientationEventListener myOrientationEventListener;

    private static final String TAG = "RotationAwareTextView";

    private int currentOrientation = 0;

    private int angle = 0;

    public RotationAwareImageView(Context context) {
        super(context);
    }

    public RotationAwareImageView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }
    public RotationAwareImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    int deviceDefault = -1;

    public int getDeviceDefaultOrientation() {

        WindowManager windowManager =  (WindowManager) getContext().getSystemService(getContext().WINDOW_SERVICE);

        Configuration config = getResources().getConfiguration();

        int rotation = windowManager.getDefaultDisplay().getRotation();

        if ( ((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) &&
                config.orientation == Configuration.ORIENTATION_LANDSCAPE)
                || ((rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) &&
                config.orientation == Configuration.ORIENTATION_PORTRAIT)) {
            return Configuration.ORIENTATION_LANDSCAPE;
        } else {
            return Configuration.ORIENTATION_PORTRAIT;
        }
    }

    public Quadrant getDeviceDefaultOrientation(int sensorAngle) throws Exception{
        if(deviceDefault == -1) {
            deviceDefault = getDeviceDefaultOrientation();
        }
        return new Quadrant(sensorAngle, deviceDefault);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // View is now attached
        myOrientationEventListener
                = new OrientationEventListener(getContext(), SensorManager.SENSOR_DELAY_NORMAL) {

            @Override
            public void onOrientationChanged(int orientation) {
                int newOrientation = 0;

                try {
                    Quadrant q = getDeviceDefaultOrientation(orientation);
                    newOrientation = q.getOrientation();
                    angle = q.getRotation();
                }
                catch (Exception ex) {

                }

                if(newOrientation != 0 && newOrientation != currentOrientation) {
                    Log.d(TAG, "Orientation:" + newOrientation);
                    postInvalidate();
                    currentOrientation = newOrientation;
                }
            }
        };

        if (myOrientationEventListener.canDetectOrientation()) {
            myOrientationEventListener.enable();
        } else {
            Toast.makeText(getContext(), "Can't DetectOrientation", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // View is now detached, and about to be destroyed
        myOrientationEventListener.disable();
        Log.e(TAG, "Detached orientation listener");
    }



    @Override
    protected void onDraw(Canvas canvas) {
        canvas.rotate(angle, canvas.getWidth()/2f, canvas.getHeight()/2f);
        super.onDraw(canvas);
    }
}
