/*    
 * Copyright (c) 2014 Samsung Electronics Co., Ltd.   
 * All rights reserved.   
 *   
 * Redistribution and use in source and binary forms, with or without   
 * modification, are permitted provided that the following conditions are   
 * met:   
 *   
 *     * Redistributions of source code must retain the above copyright   
 *        notice, this list of conditions and the following disclaimer.  
 *     * Redistributions in binary form must reproduce the above  
 *       copyright notice, this list of conditions and the following disclaimer  
 *       in the documentation and/or other materials provided with the  
 *       distribution.  
 *     * Neither the name of Samsung Electronics Co., Ltd. nor the names of its  
 *       contributors may be used to endorse or promote products derived from  
 *       this software without specific prior written permission.  
 *  
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS  
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT  
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR  
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT  
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,  
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT  
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,  
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY  
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT  
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE  
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.gear2cam.official.services;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.gear2cam.official.Settings;
import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.accessory.SA;
import com.samsung.android.sdk.accessory.SAAgent;
import com.samsung.android.sdk.accessory.SAAuthenticationToken;
import com.samsung.android.sdk.accessory.SAPeerAgent;
import com.samsung.android.sdk.accessory.SASocket;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.HashMap;

import javax.security.cert.X509Certificate;

public class CameraProviderService extends SAAgent {
    public static final String TAG = "CameraProviderService";

    public Boolean isAuthentication = false;
    public Context mContext = null;

    private final IBinder mBinder = new LocalBinder();
    private int authCount = 1;

    public class LocalBinder extends Binder {
        public CameraProviderService getService() {
            return CameraProviderService.this;
        }
    }

    public CameraProviderService() {
        super(TAG, Gear2camProviderConnection.class);
    }

    private static Gear2camProviderConnection currentConnection;

    public static Gear2camProviderConnection getCurrentConnection() {
        return currentConnection;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate of smart view Provider Service");

        SA mAccessory = new SA();
        try {
            mAccessory.initialize(this);
        } catch (SsdkUnsupportedException e) {
            // Error Handling
            Log.e(TAG, "Samsung SDK not present");
            Settings.setGearAbsent(this.getApplicationContext(), true);
            stopSelf();
        } catch (Exception e1) {
            Log.e(TAG, "Cannot initialize Accessory package.");
            e1.printStackTrace();
			/*
			 * Your application can not use Accessory package of Samsung
			 * Mobile SDK. You application should work smoothly without using
			 * this SDK, or you may want to notify user and close your app
			 * gracefully (release resources, stop Service threads, close UI
			 * thread, etc.)
			 */
            Settings.setGearAbsent(this.getApplicationContext(), true);
            stopSelf();
        }

    }

    @Override
    protected void onServiceConnectionRequested(SAPeerAgent peerAgent) {

        /*
        if(authCount%2 == 1)

            isAuthentication = false;
        else
            isAuthentication = true;
        authCount++;

        if(isAuthentication) {
            Toast.makeText(getBaseContext(), "Authentication On!", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Start authenticatePeerAgent");
            authenticatePeerAgent(peerAgent);
        }
        else {
            Toast.makeText(getBaseContext(), "Authentication Off!", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "acceptServiceConnectionRequest");
            acceptServiceConnectionRequest(peerAgent);
        }
        */
        if(!peerAgent.getAppName().equals("Gear2cam (original)")) {
            rejectServiceConnectionRequest(peerAgent);
        }
        else {
            Log.e(TAG, "acceptServiceConnectionRequest");
            acceptServiceConnectionRequest(peerAgent);
        }
    }

    protected void onAuthenticationResponse(SAPeerAgent uPeerAgent,
                                            SAAuthenticationToken authToken, int error) {

        Log.e(TAG, "onAuthenticationResponse : Entry");

        if (authToken.getAuthenticationType() == SAAuthenticationToken.AUTHENTICATION_TYPE_CERTIFICATE_X509) {
            mContext = getApplicationContext();
            byte[] myAppKey = getApplicationCertificate(mContext);

            if (authToken.getKey() != null) {
                boolean matched = true;
                if(authToken.getKey().length != myAppKey.length){
                    matched = false;
                }else{
                    for(int i=0; i<authToken.getKey().length; i++){
                        if(authToken.getKey()[i]!=myAppKey[i]){
                            matched = false;
                        }
                    }
                }
                if (matched) {
                    acceptServiceConnectionRequest(uPeerAgent);
                    Log.e(TAG, "Auth-certification matched");
                } else
                    Log.e(TAG, "Auth-certification not matched");

            }
        } else if (authToken.getAuthenticationType() == SAAuthenticationToken.AUTHENTICATION_TYPE_NONE)
            Log.e(TAG, "onAuthenticationResponse : CERT_TYPE(NONE)");
    }

    private static byte[] getApplicationCertificate(Context context) {
        if(context == null) {
            Log.e(TAG, "getApplicationCertificate ERROR, context input null");
            return null;
        }
        Signature[] sigs;
        byte[] certificat = null;
        String packageName = context.getPackageName();
        if (context != null) {
            try {
                PackageInfo pkgInfo = null;
                pkgInfo = context.getPackageManager().getPackageInfo(
                        packageName, PackageManager.GET_SIGNATURES);
                if (pkgInfo == null) {
                    Log.e(TAG, "PackageInfo was null!");
                    return null;
                }
                sigs = pkgInfo.signatures;
                if (sigs == null) {
                    Log.e(TAG, "Signature obtained was null!");
                } else {
                    CertificateFactory cf = CertificateFactory
                            .getInstance("X.509");
                    ByteArrayInputStream stream = new ByteArrayInputStream(
                            sigs[0].toByteArray());
                    X509Certificate cert;
                    cert = X509Certificate.getInstance(stream);
                    certificat = cert.getPublicKey().getEncoded();
                }
            } catch (NameNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (CertificateException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (javax.security.cert.CertificateException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
        return certificat;
    }

    @Override
    protected void onFindPeerAgentResponse(SAPeerAgent arg0, int arg1) {
        // TODO Auto-generated method stub
        Log.d(TAG, "onFindPeerAgentResponse  arg1 =" + arg1);
    }

    @Override
    protected void onServiceConnectionResponse(SASocket thisConnection,
                                               int result) {
        if (result == CONNECTION_SUCCESS) {

            if (thisConnection != null) {
                Gear2camProviderConnection myConnection = (Gear2camProviderConnection) thisConnection;

                HashMap<Integer, Gear2camProviderConnection> mConnectionsMap = myConnection.getmConnectionsMap();

                myConnection.mConnectionId = (int) (System.currentTimeMillis() & 255);

                Log.d(TAG, "onServiceConnection connectionID = "
                        + myConnection.mConnectionId);

                mConnectionsMap.put(myConnection.mConnectionId, myConnection);

                currentConnection = myConnection;

                myConnection.setService(this);

                Log.e(TAG, "Connection Success");
            } else {
                Log.e(TAG, "SASocket object is null");
            }
        } else if (result == CONNECTION_ALREADY_EXIST) {
            Log.e(TAG, "onServiceConnectionResponse, CONNECTION_ALREADY_EXIST");
        } else {
            Log.e(TAG, "onServiceConnectionResponse result error =" + result);
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }
}