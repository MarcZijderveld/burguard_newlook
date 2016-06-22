package com.example.sleepyz.burgar.app;

import android.content.SharedPreferences;

/**
 * Created by Sleepyz on 6/1/2016.
 */
public class AppConfig {
    // Server user login url
    public static String URL_LOGIN = "http://project.cmi.hr.nl/2015_2016/emedia_mt2b_t3/burguard_api/login.php";

    // Server user register url
    public static String URL_REGISTER = "http://project.cmi.hr.nl/2015_2016/emedia_mt2b_t3/burguard_api/register.php";

    public static SharedPreferences preferenceSettings;
    public static SharedPreferences.Editor preferenceEditor;

    public static final int PREFERENCE_MODE_PRIVATE = 0;

    public static String PREFERENCE_NAME = "crownstone";
}
