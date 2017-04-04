package bclurms;

import android.content.Context;

public class UrmsInitializer {
    public static void initializeWithMarlinURL(Context context, String marlinURL) {
        UrmsInitializeRequest urmsInitializeRequest = new UrmsInitializeRequest();
        urmsInitializeRequest.setMarlinURL(marlinURL);
        urmsInitializeRequest.goWithProfileName(context, "default");
    }
}
