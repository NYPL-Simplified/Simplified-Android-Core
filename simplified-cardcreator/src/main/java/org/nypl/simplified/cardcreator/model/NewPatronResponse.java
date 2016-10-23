package org.nypl.simplified.cardcreator.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.Serializable;

/**
 * Created by aferditamuriqi on 8/29/16.
 *
 */

public class NewPatronResponse  implements Serializable {


    public String type;
    public String username;
    public String barcode;
    public String pin;
    public Boolean temporary;
    public String message;

    public NewPatronResponse(InputStream inputStream) {


        try {
            JSONObject json = new JSONObject(convertStreamToString(inputStream));

            type = json.getString("type");
            username = json.getString("username");
            barcode = json.getString("barcode");
            pin = json.getString("pin");
            message = json.getString("message");
            Object obj = json.get("temporary");
            if (obj instanceof Boolean) {
                temporary = json.getBoolean("temporary");
            }


        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public static String convertStreamToString(InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
