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

public class UsernameResponse  implements Serializable {


    private String type;
    private String card_type;
    private String message;

    /**
     * @return type
     */
    public String getType() {
        return this.type;
    }

    /**
     * @return card type
     */
    public String getCard_type() {
        return this.card_type;
    }

    /**
     * @return message
     */
    public String getMessage() {
        return this.message;
    }

    /**
     * @param input_stream input stream
     */
    public UsernameResponse(final InputStream input_stream) {

        try {
            final JSONObject json = new JSONObject(convertStreamToString(input_stream));

            this.type = json.getString("type");
            this.card_type = json.getString("card_type");
            this.message = json.getString("message");

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private static String convertStreamToString(final InputStream is) {
        final Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
