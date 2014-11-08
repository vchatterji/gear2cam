package com.gear2cam.official;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.gear2cam.official.R;

import org.json.JSONObject;

public class PushReceiver extends BroadcastReceiver {
    public PushReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        try
        {
            JSONObject json = new JSONObject(intent.getExtras().getString("com.parse.Data"));
            int actionType = json.getInt("actionType");
            String actionData = json.getString("actionData");
            String text = json.getString("text");

            String subject = json.getString("subject");

            String title;
            try {
                title = json.getString("title");
                if(title == null || title.equalsIgnoreCase("")) {
                    title = context.getString(R.string.app_name);
                }
            }
            catch (Exception ex) {
                title = context.getString(R.string.app_name);
            }

            if(actionType == 1 && text != null && !text.equals("")) {
                Intent notificationIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(actionData));
                PendingIntent contentIntent = PendingIntent.getActivity(context.getApplicationContext(), 0, notificationIntent, 0);

                int nId = Settings.getNotificationId(context);

                Drawable blankDrawable = context.getResources().getDrawable(R.drawable.ic_launcher);
                Bitmap blankBitmap = ((BitmapDrawable)blankDrawable).getBitmap();

                // Constructs the Builder object.
                NotificationCompat.Builder builder =
                        new NotificationCompat.Builder(context.getApplicationContext())
                                .setSmallIcon(R.drawable.ic_launcher)
                                .setContentTitle(title)
                                .setContentText(subject)
                                .setContentIntent(contentIntent)
                                .setAutoCancel(true)
                                .setLargeIcon(blankBitmap)
                                .setPriority(NotificationCompat.PRIORITY_MAX)
                                .setDefaults(Notification.DEFAULT_ALL) // requires VIBRATE permission
        /*
         * Sets the big view "big text" style and supplies the
         * text (the user's reminder message) that will be displayed
         * in the detail area of the expanded notification.
         * These calls are ignored by the support library for
         * pre-4.1 devices.
         */
                                .setStyle(new NotificationCompat.BigTextStyle()
                                        .bigText(text));


                NotificationManager mNotificationManager =
                        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.notify(nId, builder.build());
            }
        }
        catch (Exception ex) {

        }
    }
}
