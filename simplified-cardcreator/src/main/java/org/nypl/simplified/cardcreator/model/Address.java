package org.nypl.simplified.cardcreator.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by aferditamuriqi on 8/29/16.
 *
 */

public class Address  implements Serializable {


    public String line_1;
    public String line_2;
    public String city;
    public String county;
    public String state;
    public String zip;
    public Boolean is_residential;

    public Address(JSONObject address) {


        try {
            line_1 = address.getString("line_1");
            line_2 = address.getString("line_2");
            city = address.getString("city");
            county = address.getString("county");
            state = address.getString("state");
            zip = address.getString("zip");
            Object obj = address.get("is_residential");
            if (obj instanceof Boolean) {
                is_residential = address.getBoolean("is_residential");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
