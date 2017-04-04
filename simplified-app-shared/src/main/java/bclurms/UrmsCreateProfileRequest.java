package bclurms;


import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.sonydadc.urms.android.Urms;
import com.sonydadc.urms.android.UrmsError;
import com.sonydadc.urms.android.api.CreateProfileTask;
import com.sonydadc.urms.android.task.EmptyResponse;
import com.sonydadc.urms.android.task.IFailedCallback;
import com.sonydadc.urms.android.task.ISucceededCallback;
import com.sonydadc.urms.android.task.IUrmsTask;
import com.sonydadc.urms.android.task.UrmsTaskStatus;
import com.sonydadc.urms.android.type.UrmsConfig;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static android.content.ContentValues.TAG;


public class UrmsCreateProfileRequest {



    public static void requestAuthToken() throws SignatureException {

        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try  {

                    String userID = "google-110495186711904557779";
                    String path = "/store/v2/users/" + userID + "/authtoken/generate";
                    String sessionURL = "http://urms-967957035.eu-west-1.elb.amazonaws.com" + path;

                    String timestamp = Long.toString(System.currentTimeMillis());
                    String hmacMessage = path + timestamp;
                    String secretKey = "ucj0z3uthspfixtba5kmwewdgl7s1prm";


                    String hmac = hashMac(hmacMessage, secretKey);
                    String authHash = Base64.encodeToString(hmac.getBytes(), Base64.DEFAULT);

                    String storeID = "129";
                    String authString = storeID + "-" + timestamp + "-" + authHash;

                    // Create a new HttpClient and Post Header
                    HttpClient httpclient = new DefaultHttpClient();
                    HttpPost httppost = new HttpPost(sessionURL);


                    try {
                        // Set headers
                        httppost.setHeader("Content-Type", "application/x-www-form-urlencoded");

                        // Data to POST
                        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                        nameValuePairs.add(new BasicNameValuePair("authString", authString));
                        nameValuePairs.add(new BasicNameValuePair("timestamp", timestamp));

                        // Set content length header
                        UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(nameValuePairs);
                        long urlEncodedFormEntityLength = urlEncodedFormEntity.getContentLength();
                        httppost.setHeader("Content-Length", Long.toString(urlEncodedFormEntityLength));

                        // Set data body
                        httppost.setEntity(urlEncodedFormEntity);



                        // Execute HTTP Post Request
                        HttpResponse httpResponse = httpclient.execute(httppost);
                        HttpEntity responseEntity = httpResponse.getEntity();
                        String response = "";
                        if(responseEntity!=null) {
                            response = EntityUtils.toString(responseEntity);
                        }

                        JSONObject responseJson;
                        try {
                            responseJson = new JSONObject(response);
                            String authToken = responseJson.getString("authToken");

                            initialize(authToken, "default");

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }



                    } catch (ClientProtocolException e) {
                        // TODO Auto-generated catch block
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                    }



                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();


    }

    public static void initialize(String authToken, String profileName) {


        if (profileName == "") {
            profileName = "default";
        }

        UrmsConfig config = new UrmsConfig(
                "https://urms-sdk.codefusion.technology/sdk/",			// cgp.api
                "https://urms-marlin-us.codefusion.technology/bks/",	// marlin.api
                "urn:marlin:organization:sne:service-provider:2",		// marlin.service_id
                true 													// marlin.use_ssl
        );

        CreateProfileTask createProfile = Urms.createCreateProfileTask(authToken, profileName, null, config);
        Log.d(TAG, "!@!@! Token: " + authToken);

        createProfile.setSucceededCallback(new ISucceededCallback<EmptyResponse>() {
            @Override
            public void onSucceeded(IUrmsTask task, EmptyResponse result) {
                Log.d(TAG, "Success creating profile.");
            }
        });

        createProfile.setFailedCallback(new IFailedCallback() {
            @Override
            public void onFailed(IUrmsTask task, UrmsTaskStatus status, UrmsError error) {
                if (error.getErrorType() == UrmsError.RegisterUserDeviceCapacityReached) {
                    Log.e(TAG, "RegisterUserDeviceCapacityReached");
                } else if ( error.getErrorType() == UrmsError.NetworkError ||
                        error.getErrorType() == UrmsError.NetworkTimeout) {
                    Log.e(TAG, "Network error or network timeout.");
                } else if(error.getErrorType() == UrmsError.UrmsNotInitialized){
                    Log.e(TAG, "URMS not initialized.");
                } else if (error.getErrorType() == UrmsError.LoseTime) {
                    Log.e(TAG, "Please ensure the time on your device is correct.");
                } else if (error.getErrorType() == UrmsError.OutdatedVersion) {
                    Log.e(TAG, "Outdated version");
                } else if (error.getErrorCode().endsWith("04")) {
                    Log.e(TAG, "Potential server/client configuration mismatch.");
                } else {
                    Log.e(TAG, "Other error: " + error.getErrorCode());
                    Log.e(TAG, "Error type: " + new Integer(error.getErrorType()).toString());
                }
                Log.e(TAG, "Error creating profile.");
            }
        });
        Urms.executeAsync(createProfile);
    }


    /**
     * Interface for handling post-user registration calls
     * @author Bluefire
     *
     */
    public interface OnCreateProfile {
        public void onCreateProfileComplete(boolean success);
    }


    /**
     * Method for registering a URMS User
     * @param context
     * @param urmsToken
     * @param onProfileCreated
     */
    public static void createProfile(Context context, String urmsToken, final OnCreateProfile onProfileCreated) {
        Log.e(TAG, "[registerURMSUser] About to register URMS User. ");
        Log.e(TAG, "[registerURMSUser] urmsToken: " + urmsToken);
        UrmsCreateProfileRequest createProfileRequest = new UrmsCreateProfileRequest();
        createProfileRequest.initialize(urmsToken, "default");
    }


    /**
     * Encryption of a given text using the provided secretKey
     *
     * @param text
     * @param secretKey
     * @return the encoded string
     * @throws SignatureException
     */
    public static String hashMac(String text, String secretKey)
            throws SignatureException {

        try {
            Key sk = new SecretKeySpec(secretKey.getBytes(), HASH_ALGORITHM);
            Mac mac = Mac.getInstance(sk.getAlgorithm());
            mac.init(sk);
            final byte[] hmac = mac.doFinal(text.getBytes());
            return toHexString(hmac);
        } catch (NoSuchAlgorithmException e1) {
            // throw an exception or pick a different encryption method
            throw new SignatureException(
                    "error building signature, no such algorithm in device "
                            + HASH_ALGORITHM);
        } catch (InvalidKeyException e) {
            throw new SignatureException(
                    "error building signature, invalid key " + HASH_ALGORITHM);
        }
    }

    private static final String HASH_ALGORITHM = "HmacSHA256";

    public static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);

        Formatter formatter = new Formatter(sb);
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }

        return sb.toString();
    }



}
