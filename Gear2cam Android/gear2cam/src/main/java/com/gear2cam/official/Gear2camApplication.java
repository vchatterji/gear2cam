package com.gear2cam.official;

import android.app.Application;
import android.content.pm.PackageManager;
import android.os.Build;

import com.gear2cam.official.R;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseInstallation;
import com.parse.ParseUser;
import com.parse.PushService;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

/**
 * Created by varun on 11/5/14.
 */
public class Gear2camApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CalligraphyConfig.initDefault("fonts/fontawesome-webfont.ttf");

        //Parse.setLogLevel(Parse.LOG_LEVEL_VERBOSE);

        Parse.enableLocalDatastore(this);
        //Initialize Parse
        Parse.initialize(this, "YOUR_APPLICATION_ID",
                "YOUR_CLIENT_KEY");

        // Set your Facebook App Id in strings.xml
        ParseFacebookUtils.initialize(getString(R.string.app_id));


        ParseUser user = ParseUser.getCurrentUser();

        // Save the current Installation to Parse.
        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        installation.getInstallationId();
        //installation.getInstallationId();
        installation.put("Product", Build.PRODUCT);
        installation.put("Model", Build.MODEL);
        installation.put("Android", Build.VERSION.SDK_INT);
        if(user != null) {
            installation.put("User", user);
        }

        installation.increment("RunCount");
        installation.saveEventually();
    }
}
