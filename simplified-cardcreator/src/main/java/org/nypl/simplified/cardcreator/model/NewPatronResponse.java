package org.nypl.simplified.cardcreator.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Scanner;

/**
 * Created by aferditamuriqi on 8/29/16.
 *
 */

public class NewPatronResponse  implements Serializable {


    private String type;
    private String username;
    private String barcode;
    private String pin;
    private Boolean temporary;
    private String message;

    /**
     * @return type
     */
    public String getType() {
        return this.type;
    }

    /**
     * @return username
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * @return barcode
     */
    public String getBarcode() {
        return this.barcode;
    }

    /**
     * @return pin
     */
    public String getPin() {
        return this.pin;
    }

    /**
     * @return temporary
     */
    public Boolean getTemporary() {
        return this.temporary;
    }

    /**
     * @return message
     */
    public String getMessage() {
        return this.message;
    }

    /**
     * @param inptut_stream input stream
     */
    public NewPatronResponse(final InputStream inptut_stream) {


        try {
            final JSONObject json = new JSONObject(convertStreamToString(inptut_stream));

            this.type = json.getString("type");
            this.username = json.getString("username");
            this.barcode = json.getString("barcode");
            this.pin = json.getString("pin");
            this.message = json.getString("message");
            final Object obj = json.get("temporary");
            if (obj instanceof Boolean) {
                this.temporary = json.getBoolean("temporary");
            }


        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private static String convertStreamToString(final InputStream is) {
        final Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
