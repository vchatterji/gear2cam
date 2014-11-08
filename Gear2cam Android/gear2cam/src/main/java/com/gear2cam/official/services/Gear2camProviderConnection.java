package com.gear2cam.official.services;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Base64;
import android.util.Log;

import com.gear2cam.official.CameraMonitor;
import com.gear2cam.official.DeviceProfile;
import com.gear2cam.official.Settings;
import com.samsung.android.sdk.accessory.SASocket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Created by varun on 11/5/14.
 */
public class Gear2camProviderConnection extends SASocket {
    HashMap<Integer, Gear2camProviderConnection> mConnectionsMap = null;

    public static final int GEAR2CAM_CHANNEL_ID = 127;

    private static final String TAG = "Gear2camProviderConnection";
    int mConnectionId;

    private static Object lock = new Object();
    private static byte[] currentFrame = null;
    private static int currentAngle = 0;
    private static int currentWidth = 0;
    private static int currentHeight = 0;
    private static int currentFormat = 0;
    private static boolean useFFC = false;

    private static int currentUploadCount = 0;


    boolean inProgress = false;



    public static byte[] getCurrentFrame(boolean resizeForGear) {
        synchronized (lock) {
            YuvImage image = new YuvImage(currentFrame, currentFormat, currentWidth, currentHeight, null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            int newWidth = 320;
            int newHeight = (int) ((float) currentHeight * ((float) 320 / (float) currentWidth));

            Rect area = new Rect(0, 0, currentWidth, currentHeight);
            image.compressToJpeg(area, 100, out);
            byte[] imageBytes = out.toByteArray();
            Bitmap bitImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

            if(resizeForGear && (newWidth != currentWidth || newHeight != currentHeight)) {
                bitImage =  Bitmap.createScaledBitmap(bitImage, newWidth, newHeight, true);
            }
            else {
                newHeight = currentHeight;
                newWidth = currentWidth;
            }

            Matrix matrix = new Matrix();


            if(useFFC) {
                //mirror the images
                matrix.preScale(-1.0f, 1.0f);
            }


            matrix.postRotate(currentAngle);
            bitImage =  Bitmap.createBitmap(bitImage, 0, 0, newWidth, newHeight, matrix, true);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitImage.compress(Bitmap.CompressFormat.JPEG, 50, stream);
            byte[] byteArray = stream.toByteArray();

            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            bitImage.recycle();

            return byteArray;
        }
    }

    public static void setCurrentFrame(byte[] frame, int angle, int width, int height, int format, boolean ffc) {
        synchronized (lock) {
            if(angle == 270) {
                angle = 90;
            }
            else if(angle == 90) {
                angle = 270;
            }

            currentFrame = frame;
            currentAngle = angle;
            currentWidth = width;
            currentHeight = height;
            currentFormat = format;
            useFFC = ffc;

            if(currentFrame != null) {
                String orientation = "";
                if(currentAngle == 0 || currentAngle == 180) {
                    orientation = "LANDSCAPE";
                }
                else {
                    orientation = "PORTRAIT";
                }
            }
        }
    }



    public Gear2camProviderConnection() {
        super(Gear2camProviderConnection.class.getName());
    }

    public HashMap<Integer, Gear2camProviderConnection> getmConnectionsMap() {
        if (mConnectionsMap == null) {
            mConnectionsMap = new HashMap<Integer, Gear2camProviderConnection>();
        }

        return mConnectionsMap;
    }

    Handler mHandler;

    CameraProviderService service;
    public void setService(CameraProviderService service) {
        this.service = service;
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onError(int channelId, String errorString, int error) {
        Log.e(TAG, "Connection is not alive ERROR: " + errorString + "  "
                + error);
    }

    boolean isconnected = false;

    public boolean isConnected() {
        return isconnected;
    }


    public void sendClicked(final String filePath, final String thumbnailBase64) {
        if(isconnected) {
            final Gear2camProviderConnection uHandler = mConnectionsMap.get(Integer
                    .parseInt(String.valueOf(mConnectionId)));
            new Thread(new Runnable() {
                public void run() {
                    try {
                        boolean isPublishingEnabled = Settings.isPublishingEnabled(service.getApplicationContext());
                        int p = 0;
                        if(isPublishingEnabled)
                            p = 1;

                        uHandler.send(GEAR2CAM_CHANNEL_ID, ("CLICKED," + p + "," + filePath + "," + thumbnailBase64).getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    public void sendCamError() {

            final Gear2camProviderConnection uHandler = mConnectionsMap.get(Integer
                    .parseInt(String.valueOf(mConnectionId)));
            new Thread(new Runnable() {
                public void run() {
                    try {
                        uHandler.send(GEAR2CAM_CHANNEL_ID, "CAMERROR".getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
    }

    public void sendClicked() {
        if(isconnected) {
            inProgress = false;
            final Gear2camProviderConnection uHandler = mConnectionsMap.get(Integer
                    .parseInt(String.valueOf(mConnectionId)));
            new Thread(new Runnable() {
                public void run() {
                    try {
                        uHandler.send(GEAR2CAM_CHANNEL_ID, "CLICKACK".getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    public void sendSwitched() {
        if(isconnected) {
            inProgress = false;
            final Gear2camProviderConnection uHandler = mConnectionsMap.get(Integer
                    .parseInt(String.valueOf(mConnectionId)));
            new Thread(new Runnable() {
                public void run() {
                    try {
                        uHandler.send(GEAR2CAM_CHANNEL_ID, "SWITCHEDCAM".getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    public void disconnect() {
        if(isconnected) {
            final Gear2camProviderConnection uHandler = mConnectionsMap.get(Integer
                    .parseInt(String.valueOf(mConnectionId)));
            new Thread(new Runnable() {
                public void run() {
                    try {
                        uHandler.send(GEAR2CAM_CHANNEL_ID, "DISCONNECTED".getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    public synchronized void decrementUploadCount() {
        currentUploadCount--;
    }

    public synchronized void incrementUploadCount() {
        currentUploadCount++;
    }

    public void sendFailed(final String reason) {
        if(isconnected) {
            final Gear2camProviderConnection uHandler = mConnectionsMap.get(Integer
                    .parseInt(String.valueOf(mConnectionId)));
            new Thread(new Runnable() {
                public void run() {
                    try {

                        uHandler.send(GEAR2CAM_CHANNEL_ID, ("ERROR," + reason).getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    CameraMonitor mon;
    @Override
    public synchronized void onReceive(int channelId, byte[] data) {
        String received = new String(data);
        onReceive(received);
    }

    public synchronized void onReceive(String received) {
        Log.d(TAG, "onReceive");

        final Gear2camProviderConnection uHandler = mConnectionsMap.get(Integer
                .parseInt(String.valueOf(mConnectionId)));
        if(uHandler == null){
            Log.e(TAG,"Error, can not get Gear2camProviderConnection handler");
            return;
        }

        if(received.equals("CONNECT")) {
            if(!Settings.isEulaAccepted(service)) {
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            uHandler.send(GEAR2CAM_CHANNEL_ID, "NOTLOGGEDIN".getBytes());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
            else {
                PowerManager powerManager = (PowerManager) service.getSystemService(service.POWER_SERVICE);
                KeyguardManager km = (KeyguardManager) service.getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);

                boolean isScreenOn = powerManager.isScreenOn();
                boolean isKeyguardLocked = km.isKeyguardLocked();
                //boolean isForeground = isForeground();


                if(!DeviceProfile.isBackgroundCameraSupported() || !Settings.isBackgroundSupported(service.getApplicationContext())) {
                    if(!isScreenOn || isKeyguardLocked) {
                        try {
                            uHandler.send(GEAR2CAM_CHANNEL_ID, "SCREENOFF".getBytes());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return;
                    }
                }

                if(DeviceProfile.isBackgroundCameraSupported() && Settings.isBackgroundSupported(service.getApplicationContext())) {
                    mon = new CameraMonitor(service);
                    mon.start();
                }
                else {
                    service.sendBroadcast(new Intent(Intents.INTENT_CONNECT));
                }

                if(!isconnected) {
                    new Thread(new Runnable() {
                        public void run() {
                            isconnected = true;
                            while(isconnected) {
                                try {
                                    synchronized (lock)
                                    {
                                        if(currentFrame != null && !inProgress) {
                                            String orientation = "";
                                            if(currentAngle == 0 || currentAngle == 180) {
                                                orientation = "LANDSCAPE";
                                            }
                                            else {
                                                orientation = "PORTRAIT";
                                            }

                                            byte[] byteArray = getCurrentFrame(true);

                                            String base64Encoding = "FRAME," + orientation + ","  + Base64.encodeToString(byteArray, Base64.NO_WRAP) + "," + currentUploadCount;
                                            uHandler.send(GEAR2CAM_CHANNEL_ID, base64Encoding.getBytes());
                                        }
                                    }

                                    if(inProgress) {
                                        Thread.sleep(1000, 0);
                                    }
                                    else {
                                        Thread.sleep(60, 0);
                                    }
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            currentFrame = null;
                        }
                    }).start();
                }
            }
        }
        else if(received.equals("DISCONNECT")) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        uHandler.send(GEAR2CAM_CHANNEL_ID, "DISCONNECTED".getBytes());
                        if(isconnected) {
                            if(DeviceProfile.isBackgroundCameraSupported() && Settings.isBackgroundSupported(service.getApplicationContext())) {
                                mon.stop();
                                mon = null;
                            }
                            else {
                                service.sendBroadcast(new Intent(Intents.INTENT_DISCONNECT));
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
        else if(received.startsWith("CLICK")) {
            String[] parts = received.split(",");
            final int seconds = Integer.parseInt(parts[1]);
            if(seconds == 0) {
                inProgress = true;
                service.sendBroadcast(new Intent(Intents.INTENT_CLICK));
            }
            else {
                new Thread(new Runnable() {
                    public void run() {
                        int localSeconds = seconds;
                        try {
                            while(localSeconds > 0) {
                                Thread.sleep(1000);
                                localSeconds--;
                            }
                            inProgress = true;
                            service.sendBroadcast(new Intent(Intents.INTENT_CLICK));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        }
        else if(received.startsWith("FLASH")) {
            String[] parts = received.split(",");
            final int mode = Integer.parseInt(parts[1]);

            Intent intent = new Intent(Intents.INTENT_FLASH_MODE);
            intent.putExtra(Intents.EXTRA_FLASH_MODE, mode);
            service.sendBroadcast(intent);
        }
        else if(received.startsWith("PUBLISH")) {
            if(currentUploadCount >= 5) {

            }
            else {
                String[] parts = received.split(",");
                String fileName = parts[1];
                Intent intent = new Intent(Intents.INTENT_PUBLISH);
                intent.putExtra(Intents.EXTRA_FILE_PATH, fileName);
                service.sendBroadcast(intent);
            }
        }
        else if(received.startsWith("SWITCHCAM")) {
            inProgress = true;
            Intent intent = new Intent(Intents.INTENT_SWITCH_CAM);
            service.sendBroadcast(intent);
        }
    }

    @Override
    protected void onServiceConnectionLost(int errorCode) {
        Log.e(TAG, "onServiceConectionLost  for peer = " + mConnectionId
                + "error code =" + errorCode);

        if (mConnectionsMap != null) {
            mConnectionsMap.remove(mConnectionId);
            if(isconnected) {
                if(DeviceProfile.isBackgroundCameraSupported()) {
                    if(mon != null) {
                        mon.stop();
                        mon = null;
                    }
                }
                else {
                    service.sendBroadcast(new Intent(Intents.INTENT_DISCONNECT));
                }
                isconnected = false;
            }
        }
    }

    public void switchStrategy() {
        if(isconnected) {
            Settings.setBackgroundSupported(service.getApplicationContext(), false);
            mon.stop();
            mon = null;
            onReceive("CONNECT");
        }
    }
}
