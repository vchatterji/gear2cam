package com.gear2cam.official;

import com.parse.ParseAnalytics;
import com.parse.ParseInstallation;
import com.parse.ParseUser;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by varun on 16/6/14.
 */
public class AppAnalytics {
    public static void trackAppEvent(String event) {
        //Create a Parse Analytics event
        try {
            Map<String, String> dimensions = new HashMap<String, String>();

            ParseUser user = ParseUser.getCurrentUser();
            ParseInstallation installation = ParseInstallation.getCurrentInstallation();
            if(user != null) {
                dimensions.put("user", user.getObjectId());
                dimensions.put("installation", installation.getObjectId());
                ParseAnalytics.trackEvent(event, dimensions);
            }
            else {
                dimensions.put("installation", installation.getObjectId());
                ParseAnalytics.trackEvent(event, dimensions);
            }
        }
        catch (Exception ex) {

        }
    }
}
