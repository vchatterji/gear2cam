package com.gear2cam.official;

import android.content.Context;
import android.content.Intent;

import com.samsung.android.sdk.accessory.RegisterUponInstallReceiver;

/**
 * Created by varun on 21/5/14.
 */
public class RegisterUponInstallReceiverExt extends RegisterUponInstallReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Settings.setGearAbsent(context.getApplicationContext(), false);
    }
}
