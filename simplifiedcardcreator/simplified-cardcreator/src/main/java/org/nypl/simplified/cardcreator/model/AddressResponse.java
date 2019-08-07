package org.nypl.simplified.cardcreator.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Scanner;

/**
 * Created by aferditamuriqi on 8/29/16.
 *
 */

public class AddressResponse implements Serializable {


    private String type;
    private String card_type;
    private String message;
    private JSONArray addresses;
    private Address address;
    private Address original_address;



    /**
     * @return original address
     */
    public Address getOriginal_address() {
        return this.original_address;
    }

    /**
     * @return type
     */
    public String getType() {
        return this.type;
    }

    /**
     * @return message
     */
    public String getMessage() {
        return this.message;
    }

    /**
     * @return addresses
     */
    public JSONArray getAddresses() {
        return this.addresses;
    }

    /**
     * @return address
     */
    public Address getAddress() {
        return this.address;
    }

    /**
     * @return card type
     */
    public String getCard_type() {
        return this.card_type;
    }

    /**
     * @param input_stream input stream
     */
    public AddressResponse(final InputStream input_stream) {


        try {
            final JSONObject json = new JSONObject(convertStreamToString(input_stream));

            this.type = json.getString("type");
            this.card_type = json.getString("card_type");
            this.message = json.getString("message");

            if (!json.isNull("addresses")) {
                this.addresses = json.getJSONArray("addresses");
            }
            if (!json.isNull("address")) {
                this.address = new Address(json.getJSONObject("address"));
            }
            this.original_address = new Address(json.getJSONObject("original_address"));

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private static String convertStreamToString(final InputStream is) {
        final Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
