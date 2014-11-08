package com.gear2cam.official;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.commonsware.cwac.camera.CameraFragment;
import com.commonsware.cwac.camera.CameraView;
import com.commonsware.cwac.camera.PictureTransaction;
import com.commonsware.cwac.camera.SimpleCameraHost;
import com.gear2cam.official.R;
import com.gear2cam.official.services.CameraProviderService;
import com.gear2cam.official.services.Gear2camProviderConnection;
import com.gear2cam.official.services.Intents;

import java.io.File;
import java.util.List;

/**
 * Created by sent.ly on 13/5/14.
 */
public class ViewFinderFragment extends CameraFragment  {
    private static final String TAG = "ViewFinderFragment";

    private BroadcastReceiver clickReceiver;
    private BroadcastReceiver flashModeReceiver;

    private String flashMode = Camera.Parameters.FLASH_MODE_AUTO;

    CameraView cameraView;

    private static final String KEY_USE_FFC=
            "com.gear2cam.USE_FFC";

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ViewFinderFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ViewFinderFragment newInstance(boolean useFFC) {
        ViewFinderFragment fragment = new ViewFinderFragment();

        Bundle args=new Bundle();

        args.putBoolean(KEY_USE_FFC, useFFC);
        fragment.setArguments(args);

        return fragment;
    }

    ViewFinderCameraHost host;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        host = new ViewFinderCameraHost(getActivity());

        SimpleCameraHost.Builder builder=
                new SimpleCameraHost.Builder(host);
        setHost(builder.useFullBleedPreview(false).build());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        cameraView =  (CameraView) super.onCreateView(inflater,container,savedInstanceState);
        LinearLayout rootView = (LinearLayout) inflater.inflate(R.layout.fragment_viewfinder, container, false);
        ((ViewGroup)rootView.findViewById(R.id.camera)).addView(cameraView);

        clickReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try
                {
                    takePicture();
                }
                catch (Exception ex) {

                }
            }
        };
        IntentFilter filter = new IntentFilter(Intents.INTENT_CLICK);
        this.getActivity().registerReceiver(clickReceiver, filter);

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
        this.getActivity().registerReceiver(flashModeReceiver, filter);


        host.start();

        return rootView;
    }


    @Override
    public void onDestroyView() {
        getActivity().unregisterReceiver(clickReceiver);
        getActivity().unregisterReceiver(flashModeReceiver);
        ViewFinderCameraHost host = (ViewFinderCameraHost) getHost();
        host.destroy();
        super.onDestroyView();
    }


    public class ViewFinderCameraHost extends SimpleCameraHost
    {
        OrientationEventListener myOrientationEventListener;

        @Override
        public boolean useFrontFacingCamera() {
            if (getArguments() == null) {
                return(false);
            }

            return(getArguments().getBoolean(KEY_USE_FFC));
        }

        @Override
        public Camera.ShutterCallback getShutterCallback() {
            return new Camera.ShutterCallback() {
                @Override
                public void onShutter() {
                }
            };
        }

        private int currentOrientation = 0;

        int angle = 0;

        public void start() {
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
                            Log.d(TAG, (useFrontFacingCamera() ? "FRONT" : "BACK") + ", " + newOrientation);
                            currentOrientation = newOrientation;
                        }
                    }
                    catch (Exception ex) {
                        Log.e(TAG, "Orientation exception", ex);
                    }
                }
            };

            myOrientationEventListener.enable();
        }

        public void destroy() {
            myOrientationEventListener.disable();
        }

        private Context context;

        int deviceDefault = -1;

        public int getDeviceDefaultOrientation() {

            WindowManager windowManager =  (WindowManager) context.getSystemService(context.WINDOW_SERVICE);

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

        public ViewFinderCameraHost(Context _ctxt) {
            super(_ctxt);

            context = _ctxt;
        }

        @Override
        protected File getPhotoDirectory() {
            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM), "Gear2cam");

            if (! mediaStorageDir.exists()){
                if (! mediaStorageDir.mkdirs()){
                    Log.d(TAG, "failed to create directory");
                    return null;
                }
            }

            return mediaStorageDir;
        }

        File lastPhoto;
        @Override
        protected File getPhotoPath() {
            synchronized (context) {
                lastPhoto = super.getPhotoPath();
                return lastPhoto;
            }
        }

        @Override
        public void saveImage(PictureTransaction xact, byte[] frame) {
            synchronized (context) {
                super.saveImage(xact, frame);

                String base64 = FacebookHelper.getWatchPreview(lastPhoto.getAbsolutePath());

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
                    CameraProviderService.getCurrentConnection().sendClicked(lastPhoto.getAbsolutePath(), base64);
                }
            }
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

        @Override
        public Camera.Size getPreviewSize(int displayOrientation, int width, int height, Camera.Parameters parameters) {
            return getBestPreviewSize(parameters.getSupportedPreviewSizes());
        }

        @Override
        public Camera.Size getPictureSize(PictureTransaction xact, Camera.Parameters parameters) {
            return getBestPictureSize(parameters.getSupportedPictureSizes());
        }

        boolean shutterSoundOff = false;

        @Override
        public Camera.Parameters adjustPreviewParameters(Camera.Parameters parameters) {
            List<String> focusModes = parameters.getSupportedFocusModes();
            if(focusModes != null) {
                if(focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                }
                else if(focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
                else if(focusModes.contains(Camera.Parameters.FOCUS_MODE_INFINITY)) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
                }
            }


            if(!shutterSoundOff) {
                String locale = context.getResources().getConfiguration().locale.getCountry();
                if(!locale.equals("JP")) {
                    Camera cam = cameraView.getCamera();
                    if(cam != null) {
                        cam.enableShutterSound(false);
                    }
                }
                shutterSoundOff = true;
            }

            if(useFrontFacingCamera()) {
                parameters.setRecordingHint(false);
            }

            return parameters;
        }

        @Override
        public Camera.Parameters adjustPictureParameters(PictureTransaction xact, Camera.Parameters parameters) {
            List<String> focusModes = parameters.getSupportedFocusModes();
            if(focusModes != null) {
                if(focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                }
                else if(focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
                else if(focusModes.contains(Camera.Parameters.FOCUS_MODE_INFINITY)) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
                }
            }

            List<String> flashModes = parameters.getSupportedFlashModes();
            if(!useFrontFacingCamera()) {
                if(flashModes != null) {
                    if(flashModes.contains(flashMode)) {
                        parameters.setFlashMode(flashMode);
                    }
                    else if(flashModes.contains(Camera.Parameters.FLASH_MODE_AUTO)) {
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                    }
                    else if(flashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    }
                }
            }
            else {
                if(flashModes != null && flashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                }
            }
            return parameters;
        }

        @Override
        public float maxPictureCleanupHeapUsage() {
            return 0.0f;
        }

        @Override
        public void onPreviewFrame(byte [] rawData, Camera camera) {
            int w = camera.getParameters().getPreviewSize().width;
            int h = camera.getParameters().getPreviewSize().height;
            int format = camera.getParameters().getPreviewFormat();
            Gear2camProviderConnection.setCurrentFrame(rawData, angle, w, h, format, useFrontFacingCamera());
        }
    }
}
