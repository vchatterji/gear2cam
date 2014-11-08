package com.gear2cam.official;

import android.os.Build;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by varun on 3/7/14.
 */
public class DeviceProfile {
    public static boolean isBackgroundCameraSupported() {
        /*
        ArrayList<String> unsupported = new ArrayList<String>(
                Arrays.asList(
                        "SC-04F", //Model SC-04F
                        "SCL22", //Model SCL22
                        "SCL23", //Model SCL23
                        "hltedx", //Model SM-N9005
                        "hlteskt", //Model SM-N900S
                        "hltespr", //Model SM-N900P
                        "hlteub", //Model SM-N900W8
                        "hltevl", //Model SM-N900W8
                        "hltevzw", //Model SM-N900V
                        "hltexx", //Model SM-N9005
                        "jfltevj", //Model GT-I9505
                        "jfltevzw", //Model SCH-I545
                        "jfltexx", //Model GT-I9505
                        "kltedv", //Model SM-G900I
                        "kltektt", //Model SM-G900K
                        "kltelgt", //Model SM-G900L
                        "klteuc", //Model SAMSUNG-SM-G900A
                        "kltevl", //Model SM-G900W8
                        "kltevzw", //Model SM-G900V
                        "kltexx", //Model S5 Korea
                        "kltezh", //Model SM-G900F
                        "occam", //Model SAMSUNG-SM-G900A
                        "t03gxx" //Model GT-N7100
                ));

        if(unsupported.contains(Build.PRODUCT)) {
            return false;
        }
        else {
            //S4 Singapore
            return true;
        }
        */
        return true;
    }
}
