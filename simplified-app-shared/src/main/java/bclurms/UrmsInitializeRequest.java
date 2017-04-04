package bclurms;

import android.content.Context;
import android.util.Log;

import com.sonydadc.urms.android.Urms;
import com.sonydadc.urms.android.UrmsError;

import static android.content.ContentValues.TAG;

class UrmsInitializeRequest {

    private int mInitializedState = 0;
    static String mMarlinURL = "";


    public void setMarlinURL(String marlinURL) {
        this.mMarlinURL = marlinURL;
    }

    public String getMarlinURL() {
        return mMarlinURL;
    }

    public void goWithProfileName(Context context, String profileName) {
        if (mInitializedState == 0) {
            if (mMarlinURL.length() == 0) {
                Log.e(TAG, "Error: No Marlin URL provided to URMS Initializer");
            }

            UrmsError error = Urms.initialize(context, 15); // context, apiTimeout

            if (error.isError()) {
                Log.e(TAG, "initialize failed: " + error.getErrorCode());
                Log.e(TAG, "Error: Urms.initialize() method returned an error.");
                return;
            }
            mInitializedState = 1;
            Log.i(TAG, "URMS initialized!");
        }
    }
}
