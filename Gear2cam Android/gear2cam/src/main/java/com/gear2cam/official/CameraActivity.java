package com.gear2cam.official;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.WindowManager;

import com.gear2cam.official.R;
import com.gear2cam.official.services.CameraProviderService;
import com.gear2cam.official.services.ErrorReasons;
import com.gear2cam.official.services.Gear2camProviderConnection;
import com.gear2cam.official.services.Intents;


public class CameraActivity extends Activity {
    private static final String TAG = "CameraActivity";
    WindowManager.LayoutParams params;

    PowerManager.WakeLock wakeLock;
    private BroadcastReceiver mPublishImageFacebookReceiver;
    private BroadcastReceiver mDisconnectReceiver;
    private BroadcastReceiver mSwitchcamReceiver;

    private boolean useFFC = false;

    private ViewFinderFragment std=null;
    private ViewFinderFragment ffc=null;
    private ViewFinderFragment current=null;

    private boolean isStarted = false;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String action = intent.getAction();

        if(action.equals(Intents.INTENT_CONNECT)) {
            //Nothing to do this should launch this activity
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(isStarted) {
            unregisterReceiver(mPublishImageFacebookReceiver);
            unregisterReceiver(mDisconnectReceiver);
            unregisterReceiver(mSwitchcamReceiver);
            wakeLock.release();
            Gear2camProviderConnection conn = CameraProviderService.getCurrentConnection();
            if(conn != null)
                conn.disconnect();
            isStarted = false;
        }

        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Gear2camProviderConnection conn = CameraProviderService.getCurrentConnection();

        if(conn==null || !conn.isConnected()) {
            isStarted = false;
            finish();
            return;
        }


        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.ON_AFTER_RELEASE,
                "MyWakelockTag");
        wakeLock.acquire();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

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

        this.registerReceiver(mPublishImageFacebookReceiver, filter);

        filter = new IntentFilter(Intents.INTENT_DISCONNECT);
        mDisconnectReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                finish();
            }
        };
        registerReceiver(mDisconnectReceiver, filter);

        filter = new IntentFilter(Intents.INTENT_SWITCH_CAM);
        mSwitchcamReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                useFFC = !useFFC;
                setFragment();
                CameraProviderService.getCurrentConnection().sendSwitched();
            }
        };
        registerReceiver(mSwitchcamReceiver, filter);

        isStarted = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_POWER) {
            finish();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        if (savedInstanceState == null) {
            setFragment();
        }
    }

    private void setFragment() {
        FragmentManager fm = getFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();

        ViewFinderFragment toSet;

        if(useFFC) {
            if(ffc == null) {
                ffc = ViewFinderFragment.newInstance(useFFC);
            }
            toSet = ffc;
        }
        else {
            if(std == null) {
                std = ViewFinderFragment.newInstance(useFFC);
            }
            toSet = std;
        }

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack
        transaction.replace(R.id.cam_fragment_container, toSet).commit();
        current = toSet;
    }

    protected void onDestroy() {
        super.onDestroy();
    }
}
