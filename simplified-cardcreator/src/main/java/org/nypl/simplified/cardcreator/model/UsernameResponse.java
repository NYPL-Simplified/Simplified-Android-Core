package org.nypl.simplified.cardcreator.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.Serializable;

/**
 * Created by aferditamuriqi on 8/29/16.
 *
 */

public class UsernameResponse  implements Serializable {


    public String type;
    public String card_type;
    public String message;

    public UsernameResponse(InputStream inputStream) {

        try {
            JSONObject json = new JSONObject(convertStreamToString(inputStream));

            type = json.getString("type");
            card_type = json.getString("card_type");
            message = json.getString("message");

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
