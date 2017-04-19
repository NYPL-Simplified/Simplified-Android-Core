package bclurms;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.sonydadc.urms.android.IUrmsTaskModifier;
import com.sonydadc.urms.android.Urms;
import com.sonydadc.urms.android.UrmsError;
import com.sonydadc.urms.android.task.IFailedCallback;
import com.sonydadc.urms.android.task.IUrmsTask;
import com.sonydadc.urms.android.task.UrmsTaskStatus;

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

            UrmsError error = Urms.initialize(context, 60); // context, apiTimeout
            final Context ctx = context;

            // Set default error handler
            Urms.setTaskModifier(new IUrmsTaskModifier() {
                @Override
                public void onTaskCreated(IUrmsTask task) {
                    task.setFailedCallback(new IFailedCallback() {
                        @Override
                        public void onFailed(IUrmsTask task, UrmsTaskStatus status, UrmsError error) {
                            Log.e(TAG, "URMS error: " + error.getErrorCode());
                            Toast.makeText(ctx, error.getErrorCode(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });

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
