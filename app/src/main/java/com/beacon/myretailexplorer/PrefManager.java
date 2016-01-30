package com.beacon.myretailexplorer;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Sumod on 29-Jan-16.
 */
public class PrefManager {

    // Shared Preferences
    SharedPreferences pref;

    // Editor for Shared preferences
    SharedPreferences.Editor editor;

    // Context
    Context _context;

    // Shared pref mode
    int PRIVATE_MODE = 0;

    // Shared pref file name
    private static final String PREF_NAME = "DebugCity";

    // All Shared Preferences Keys
    private static final String IS_LOGIN = "IsLoggedIn";

    // Email address (make variable public to access from outside)
    public static final String KEY_NAME = "name";

    public static final String KEY_EMAIL = "email";

    public static final String KEY_DEVICE_ID = "device_id";

    public static final String LOGIN_SESSION_CODE = "login_session_code";

    public final int EMAIL_LOGIN_SESSION = 0;

    public final int GOOGLE_LOGIN_SESSION = 1;

    public final int FB_LOGIN_SESSION = 2;



    // Constructor
    public PrefManager(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    /**
     * Create login session
     */
    public void createLoginSession(String person_name, String email, int loginSessionCode, String device_id) {
        // Storing login value as TRUE
        editor.putBoolean(IS_LOGIN, true);

        // Storing name in pref
        editor.putString(KEY_NAME, person_name);

        // Storing email in pref
        editor.putString(KEY_EMAIL, email);

        //Storing login session code
        editor.putInt(LOGIN_SESSION_CODE, loginSessionCode);

        //Storing Device ID
        editor.putString(KEY_DEVICE_ID, device_id);

        // commit changes
        editor.commit();
    }

    public void putLoginSessionCode(int code){
        editor.putInt(LOGIN_SESSION_CODE, code);
    }

    public int getLoginSessionCode(){
        return pref.getInt(LOGIN_SESSION_CODE, -1);
    }


    public void putDeviceID(String device_id){
        editor.putString(KEY_DEVICE_ID, device_id);
    }

    public String getDeviceID(){
        return pref.getString(KEY_DEVICE_ID, null);
    }

    public String getEmail() {
        return pref.getString(KEY_EMAIL, null);
    }

    public String getName() {
        return pref.getString(KEY_NAME, null);
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(IS_LOGIN, false);
    }

    public void logout() {
        editor.clear();
        editor.commit();
    }



}
