/*  Copyright (c) 2014, Leo Kuznetsov <Leo.Kuznetsov@gmail.com>
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

 * Neither the name of the {organization} nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.benchapp;

import android.content.*;
import android.content.pm.*;
import android.net.*;
import android.os.*;
import android.util.*;
import com.google.android.gms.common.*;
import com.google.android.gms.gcm.*;

import java.io.*;
import java.util.concurrent.atomic.*;

public class GCM {

    private static final String TAG = Act.class.getSimpleName();

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String PROPERTY_REG_ID = "GCM_REG_ID";
    private static final String PROPERTY_APP_VERSION = "GCM_APP_VERSION";
    private static final AtomicInteger msgId = new AtomicInteger();
    private static final String SENDER_ID = "Your-Sender-ID";

    private static GoogleCloudMessaging gcm;
    private static String regId;

    public static boolean create(Act a) {
        if (checkPlayServices(a)) {
            gcm = GoogleCloudMessaging.getInstance(a);
            regId = getRegistrationId(a);
            if (regId.isEmpty()) {
                registerInBackground(a);
            }
            return true;
        } else {
            return false;
        }
    }

    /** Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings. */
    public static boolean checkPlayServices(Act a) {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(a);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, a, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                // TODO: prompt user to get valid Play Services APK.
                Log.i(TAG, "This device is not supported.");
                Uri uri = Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.gms");
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
                a.startActivity(browserIntent);
                a.finish();
            }
            return false;
        }
        return true;
    }

    /** Gets the current registration ID for application on GCM service.
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     * registration ID. */
    public static String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    /** @return Application's {@code SharedPreferences}. */
    private static SharedPreferences getGCMPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return context.getSharedPreferences(Act.class.getSimpleName(), Context.MODE_PRIVATE);
    }

    /** @return Application's version code from the {@code PackageManager}. */
    private static int getAppVersion(Context context) {
        try {
            String pn = context.getPackageName();
            PackageManager pm = context.getPackageManager();
            assert pm != null;
            PackageInfo packageInfo = pm.getPackageInfo(pn, 0);
            return packageInfo.versionCode;
        } catch (Throwable e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /** Registers the application with GCM servers asynchronously.
     * Stores the registration ID and app versionCode in the application's
     * shared preferences. */
    public static void registerInBackground(final Act a) {
        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void... params) {
                String msg;
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(a);
                    }
                    regId = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regId;
                    // You should send the registration ID to your server over HTTP,
                    // so it can use GCM/HTTP or CCS to send messages to your app.
                    // The request to your server should be authenticated if your app
                    // is using accounts.
                    sendRegistrationIdToBackend();
                    // For this demo: we don't need to send it because the device
                    // will send upstream messages to a server that echo back the
                    // message using the 'from' address in the message.
                    // Persist the regID - no need to register again.
                    storeRegistrationId(a, regId);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }

            protected void onPostExecute(String msg) {
                Log.i(TAG, msg + "\n");
            }

        }.execute();
    }

    /** Sends the registration ID to your server over HTTP, so it can use GCM/HTTP
     * or CCS to send messages to your app. Not needed for this demo since the
     * device sends upstream messages to a server that echoes back the message
     * using the 'from' address in the message. */
    private static void sendRegistrationIdToBackend() {
        // Your implementation here.
    }

    /** Stores the registration ID and app versionCode in the application's
     * {@code SharedPreferences}.
     * @param context application's context.
     * @param regId   registration ID */
    private static void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGCMPreferences(context);
        int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    public static void send() {

        new AsyncTask<Void, Void, String>() {

            protected String doInBackground(Void... params) {
                String msg;
                try {
                    Bundle data = new Bundle();
                    data.putString("my_message", "Hello World");
                    data.putString("my_action", "com.benchapp.ECHO_NOW");
                    String id = Integer.toString(msgId.incrementAndGet());
                    gcm.send(SENDER_ID + "@gcm.googleapis.com", id, data);
                    msg = "Sent message";
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                }
                return msg;
            }

            protected void onPostExecute(String msg) {
                Log.i(TAG, msg + "\n");
            }

        }.execute(null, null, null);

    }

}
