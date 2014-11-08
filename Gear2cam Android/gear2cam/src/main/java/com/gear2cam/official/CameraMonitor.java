package com.gear2cam.official;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.WindowManager;

import com.gear2cam.official.services.CameraProviderService;
import com.gear2cam.official.services.ErrorReasons;
import com.gear2cam.official.services.Gear2camProviderConnection;
import com.gear2cam.official.services.Intents;
import com.parse.ParseInstallation;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by varun on 27/6/14.
 */
public class CameraMonitor implements Camera.PreviewCallback {
    private Context context;
    private OrientationEventListener myOrientationEventListener;
    private int deviceDefault = -1;
    private int angle = 0;
    private int currentOrientation = 0;
    private boolean isStarted = false;
    private static final String[] SCAN_TYPES= { "image/jpeg" };

    private static final String TAG = "CameraMonitor";

    private BroadcastReceiver mPublishImageFacebookReceiver;
    private BroadcastReceiver mDisconnectReceiver;
    private BroadcastReceiver mSwitchcamReceiver;
    private BroadcastReceiver clickReceiver;
    private BroadcastReceiver flashModeReceiver;

    private String flashMode;

    private boolean useFFC = false;

    private Camera camera;

    CameraMonitor monitor;

    public CameraMonitor(Context context) {
        this.context = context;
        monitor = this;
    }

    public int getDeviceDefaultOrientation() {

        WindowManager windowManager =  (WindowManager) context.getSystemService(context.WINDOW_SERVICE);

        Configuration config = context.getResources().getConfiguration();

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

    Object previewLock = new Object();
    public void switchCamera() {
        deinit();
        init();
    }

    protected String getPhotoFilename() {
        String ts=
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());

        return("Photo_" + ts + ".jpg");
    }

    int lastPictureOrientation = 0;

    boolean isClicking = false;
    public void takePicture() {
        isClicking = true;
        Camera.Parameters params = camera.getParameters();

        final List<String> flashModes = params.getSupportedFlashModes();
        if(flashModes != null) {
            if(flashModes.contains(flashMode)) {
                params.setFlashMode(flashMode);
            }
            else if(flashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
                params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            }
        }

        if(!useFFC) {
            if(currentOrientation == Quadrant.ORIENTATION_PORTRAIT) {
                params.setRotation(90);
            }
            else if(currentOrientation == Quadrant.ORIENTATION_LANDSCAPE_INVERSE) {
                params.setRotation(180);
            }
            else if(currentOrientation == Quadrant.ORIENTATION_PORTRAIT_INVERSE) {
                params.setRotation(270);
            }
            else {
                params.setRotation(0);
            }
        }
        else {
            if(currentOrientation == Quadrant.ORIENTATION_PORTRAIT) {
                params.setRotation(270);
            }
            else if(currentOrientation == Quadrant.ORIENTATION_LANDSCAPE_INVERSE) {
                params.setRotation(180);
            }
            else if(currentOrientation == Quadrant.ORIENTATION_PORTRAIT_INVERSE) {
                params.setRotation(90);
            }
            else {
                params.setRotation(0);
            }
        }

        lastPictureOrientation = currentOrientation;


        if(useFFC) {
            //outputOrientation = (360 - angle) % 360;
        }

        camera.setParameters(params);

        camera.takePicture(new Camera.ShutterCallback() {
            @Override
            public void onShutter() {

            }
        }, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {

            }
        }, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {
                //Jpeg image
                File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DCIM), "Gear2cam");

                if (! mediaStorageDir.exists()){
                    if (! mediaStorageDir.mkdirs()){
                        Log.d(TAG, "failed to create directory");
                    }
                }

                File photo = new File(mediaStorageDir, getPhotoFilename());
                if (photo.exists()) {
                    photo.delete();
                }

                try {
                    FileOutputStream fos=new FileOutputStream(photo.getPath());
                    BufferedOutputStream bos=new BufferedOutputStream(fos);

                    bos.write(bytes);
                    bos.flush();
                    fos.getFD().sync();
                    bos.close();

                    MediaScannerConnection.scanFile(context,
                            new String[]{photo.getPath()},
                            SCAN_TYPES, null);
                }
                catch (java.io.IOException e) {
                    Log.e(TAG, "Error saving file");
                }

                String base64 = FacebookHelper.getWatchPreview(photo.getAbsolutePath());

                try {
                    //Create a Parse Analytics event
                    AppAnalytics.trackAppEvent("click");

                    if(CameraProviderService.getCurrentConnection() != null) {
                        CameraProviderService.getCurrentConnection().sendClicked();
                    }
                }
                catch (Exception ex) {
                    //Should not fail if sound cannot be played
                }

                //Tell connection about the file
                if(CameraProviderService.getCurrentConnection() != null) {
                    CameraProviderService.getCurrentConnection().sendClicked(photo.getAbsolutePath(), base64);
                }

                isClicking = false;
                deinit();
                init();
            }
        });
    }

    private double aspectRatio = 1.33;
    private double tolerance = 0.0034;

    public Camera.Size getBestPreviewSize(List<Camera.Size> mSupportedPreviewSizes) {
        //Minimum preview size with specified aspect ratio
        int index = 0;
        int width = Integer.MAX_VALUE;

        for(int i=0; i<mSupportedPreviewSizes.size(); i++) {
            Camera.Size test = mSupportedPreviewSizes.get(i);
            if (test.width >= 320 && test.width < width && ((double) test.width / (double) test.height - aspectRatio) <= tolerance) {
                width = test.width;
                index = i;
            }
        }

        return mSupportedPreviewSizes.get(index);
    }

    public Camera.Size getBestPictureSize(List<Camera.Size> mSupportedPictureSize) {
        //Maximum picture size with given aspect ratio
        int index = 0;
        int res = 0;
        for(int i=0; i<mSupportedPictureSize.size(); i++) {
            Camera.Size test = mSupportedPictureSize.get(i);
            if(test.width * test.height > res && ((double) test.width / (double) test.height - aspectRatio) <= tolerance) {
                res = test.width * test.height;
                index = i;
            }
        }

        return mSupportedPictureSize.get(index);
    }

    boolean previewStarted = false;
    boolean isFirstTime = true;

    //SurfaceView dummy;
    SurfaceTexture dummy;
    public void initializeCamera() {
        previewStarted = false;

        // get Camera parameters
        Camera.Parameters params = camera.getParameters();

        camera.setDisplayOrientation(0);

        String locale = context.getResources().getConfiguration().locale.getCountry();
        if(!locale.equals("JP")) {
            camera.enableShutterSound(false);
        }

        Camera.Size psize = getBestPreviewSize(params.getSupportedPreviewSizes());
        params.setPreviewSize(psize.width, psize.height);

        psize = getBestPictureSize(params.getSupportedPictureSizes());
        params.setPictureSize(psize.width, psize.height);

        List<String> focusModes = params.getSupportedFocusModes();
        if(focusModes != null) {
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                // set the focus mode
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
            else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)){
                // set the focus mode
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
            else if(focusModes.contains(Camera.Parameters.FOCUS_MODE_INFINITY)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
            }
        }

        if(flashMode == null)  {
            flashMode = Camera.Parameters.FLASH_MODE_AUTO;
        }

        camera.setParameters(params);

        /*
        if(dummy == null) {
            dummy = new SurfaceView(context);
            try {
                WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                WindowManager.LayoutParams wparams = new WindowManager.LayoutParams();
                wparams.type= WindowManager.LayoutParams.TYPE_PHONE;
                wparams.flags= WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                wparams.gravity=Gravity.LEFT|Gravity.TOP;
                wparams.x=0;
                wparams.y=0;
                wparams.width=320;
                wparams.height=240;

                wm.addView(dummy, wparams);

                camera.setPreviewDisplay(dummy.getHolder());
            } catch (IOException e) {

            }
        }


        WindowManager.LayoutParams wparams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED|WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.RGBA_8888);
                WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        */


        camera.setPreviewCallback(this);
        //preview = new CamPreview(context, camera);
        //preview.setSurfaceTextureListener(preview);
        if(dummy == null) {
            dummy = new SurfaceTexture(456);
        }


        //View
        try {
            camera.setPreviewTexture(dummy);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //wm.addView(preview, wparams);
        camera.startPreview();

        if(isFirstTime && !Settings.isBackgroundCheckCompleted(context)) {
            isFirstTime = false;
            new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(9800);
                        if(isStarted && !previewStarted) {
                            ParseInstallation installation = ParseInstallation.getCurrentInstallation();
                            installation.increment("LaunchFailure");
                            installation.saveEventually();
                            CameraProviderService.getCurrentConnection().switchStrategy();
                        }
                        if(previewStarted) {
                            Settings.setBackgroundCheckCompleted(context, true);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    private void init() {
        try {
            if(!useFFC) {
                camera = Camera.open();
            }
            else {
                camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
            }

            initializeCamera();
        }
        catch (Exception ex) {
            Log.e(TAG, "Error opening camera", ex);
            CameraProviderService.getCurrentConnection().sendCamError();
            return;
        }
    }

    public synchronized void start() {
        if(!isStarted) {
            init();
            // View is now attached
            myOrientationEventListener
                    = new OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) {

                @Override
                public void onOrientationChanged(int orientation) {
                    if(deviceDefault == -1) {
                        deviceDefault = getDeviceDefaultOrientation();
                    }

                    try {
                            Quadrant q = new Quadrant(orientation, deviceDefault);
                            int newOrientation = q.getOrientation();
                            angle = q.getRotation();
                            if(newOrientation != 0 && newOrientation != currentOrientation) {
                                currentOrientation = newOrientation;
                            }
                    }
                    catch (Exception ex) {
                        Log.e(TAG, "Orientation exception", ex);
                    }
                }
            };

            myOrientationEventListener.enable();

            //Register Facebook Publish Receiver
            mPublishImageFacebookReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String path = intent.getExtras().getString(Intents.EXTRA_FILE_PATH);

                    //Create a Parse Analytics event
                    AppAnalytics.trackAppEvent("facebook");

                    FacebookHelper.publishImageToFacebook(context,path, new FacebookHelper.FacebookHelperCallback() {
                        @Override
                        public void onFacebookSaveImage(boolean success) {
                            if(!success && CameraProviderService.getCurrentConnection() != null) {
                                CameraProviderService.getCurrentConnection().sendFailed(ErrorReasons.ERR_FB_PUBLISH_FAILED);
                            }
                        }
                    });
                }
            };

            IntentFilter filter = new IntentFilter(Intents.INTENT_PUBLISH);
            context.registerReceiver(mPublishImageFacebookReceiver, filter);


            //Disconnect Receiver
            filter = new IntentFilter(Intents.INTENT_DISCONNECT);
            mDisconnectReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    stop();
                }
            };
            context.registerReceiver(mDisconnectReceiver, filter);

            filter = new IntentFilter(Intents.INTENT_SWITCH_CAM);
            mSwitchcamReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    new Thread(new Runnable() {
                        public void run() {
                            useFFC = !useFFC;
                            switchCamera();
                            CameraProviderService.getCurrentConnection().sendSwitched();
                        }
                    }).start();
                }
            };
            context.registerReceiver(mSwitchcamReceiver, filter);

            clickReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if(!isClicking) {
                        new Thread(new Runnable() {
                            public void run() {
                                takePicture();
                            }
                        }).start();
                    }
                }
            };
            filter = new IntentFilter(Intents.INTENT_CLICK);
            context.registerReceiver(clickReceiver, filter);

            flashModeReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    int mode = intent.getIntExtra(Intents.EXTRA_FLASH_MODE, 0);
                    switch (mode) {
                        case 0:
                            flashMode = Camera.Parameters.FLASH_MODE_AUTO;
                            break;
                        case 1:
                            flashMode = Camera.Parameters.FLASH_MODE_OFF;
                            break;
                        case 2:
                            flashMode = Camera.Parameters.FLASH_MODE_ON;
                            break;
                    }
                }
            };
            filter = new IntentFilter(Intents.INTENT_FLASH_MODE);
            context.registerReceiver(flashModeReceiver, filter);

            isStarted = true;
        }
    }

    private void deinit() {
        camera.setPreviewCallback(null);
        camera.stopPreview();
        camera.release();
    }

    public synchronized void stop() {
        if(isStarted) {
            context.unregisterReceiver(mPublishImageFacebookReceiver);
            context.unregisterReceiver(mDisconnectReceiver);
            context.unregisterReceiver(mSwitchcamReceiver);
            context.unregisterReceiver(flashModeReceiver);
            context.unregisterReceiver(clickReceiver);
            Gear2camProviderConnection conn = CameraProviderService.getCurrentConnection();
            if(conn != null)
            myOrientationEventListener.disable();

            deinit();

            isStarted = false;
        }
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        previewStarted = true;
        synchronized (previewLock) {
            try {
                if(!isClicking) {
                    int w = camera.getParameters().getPreviewSize().width;
                    int h = camera.getParameters().getPreviewSize().height;
                    int format = camera.getParameters().getPreviewFormat();
                    Gear2camProviderConnection.setCurrentFrame(bytes, angle, w, h, format, useFFC);
                }
            } catch (Exception ex) {
                Log.e(TAG, "Preview frame exception occurred", ex);
            }
        }
    }
}
