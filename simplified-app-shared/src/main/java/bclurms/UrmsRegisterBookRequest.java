package bclurms;

import android.util.Log;

import com.sonydadc.urms.android.Urms;
import com.sonydadc.urms.android.api.RegisterBookTask;
import com.sonydadc.urms.android.task.IFailedCallback;
import com.sonydadc.urms.android.task.IPostExecuteCallback;
import com.sonydadc.urms.android.task.ISucceededCallback;
import com.sonydadc.urms.android.task.IUrmsTask;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import static android.content.ContentValues.TAG;

public class UrmsRegisterBookRequest {

    public static void execute(String ccid, String path, ISucceededCallback succeededCallback,
                               IFailedCallback failedCallback) {

        Log.d(TAG, "[registerBookURMS] Begin with bookCCID: " + ccid);
        final RegisterBookTask rbt = Urms.createRegisterBookTask(ccid);

        try {
            rbt.setDownloadSource(new URL("http://www.google.com"));
        } catch (MalformedURLException e) {
            Log.e(TAG, "[registerBookURMS] malformed url!!");
        }
        rbt.setDestination(new File(path));
        rbt.setSucceededCallback(succeededCallback).setPostExecuteCallback(new IPostExecuteCallback() {
            @Override
            public void onPostExecute(IUrmsTask task) {
                Log.d(TAG, "[registerBookURMS] onPostExecute");
            }
        }).setFailedCallback(failedCallback);
        Log.d(TAG, "[registerBookURMS] Executing RegisterBookRequest...");
        Urms.executeBackground(rbt);
    }

}
