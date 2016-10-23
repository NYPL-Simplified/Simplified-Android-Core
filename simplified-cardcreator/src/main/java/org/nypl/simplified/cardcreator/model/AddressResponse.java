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


    public String type;
    public String card_type;
    public String message;
    public JSONArray addresses;
    public Address address;
    public Address original_address;

    public AddressResponse(InputStream inputStream) {


        try {
            JSONObject json = new JSONObject(convertStreamToString(inputStream));

            type = json.getString("type");
            card_type = json.getString("card_type");
            message = json.getString("message");

            if (!json.isNull("addresses")) {
                addresses = json.getJSONArray("addresses");
            }
            if (!json.isNull("address")){
                address = new Address(json.getJSONObject("address"));
            }
            original_address = new Address(json.getJSONObject("original_address"));

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private static String convertStreamToString(java.io.InputStream is) {
        final Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
