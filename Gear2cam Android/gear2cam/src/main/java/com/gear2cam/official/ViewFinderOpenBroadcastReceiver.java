package com.gear2cam.official;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ViewFinderOpenBroadcastReceiver extends BroadcastReceiver {
    public ViewFinderOpenBroadcastReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent activity = new Intent(context, CameraActivity.class);
        activity.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(activity);
    }
}
