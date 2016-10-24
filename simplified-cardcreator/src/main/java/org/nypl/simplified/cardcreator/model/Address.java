package org.nypl.simplified.cardcreator.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by aferditamuriqi on 8/29/16.
 *
 */

public class Address  implements Serializable {


    private String line_1;
    private String line_2;
    private String city;
    private String county;
    private String state;
    private String zip;
    private Boolean is_residential;

    /**
     * @return address line 1
     */
    public String getLine_1() {
        return this.line_1;
    }

    /**
     * @return address line 2
     */
    public String getLine_2() {
        return this.line_2;
    }

    /**
     * @return address city
     */
    public String getCity() {
        return this.city;
    }

    /**
     * @return address country
     */
    public String getCounty() {
        return this.county;
    }

    /**
     * @return address state
     */
    public String getState() {
        return this.state;
    }

    /**
     * @return address zip
     */
    public String getZip() {
        return this.zip;
    }

    /**
     * @return boolean is residential
     */
    public Boolean getIs_residential() {
        return this.is_residential;
    }

    /**
     * @param address address json object
     */
    public Address(final JSONObject address) {


        try {
            this.line_1 = address.getString("line_1");
            this.line_2 = address.getString("line_2");
            this.city = address.getString("city");
            this.county = address.getString("county");
            this.state = address.getString("state");
            this.zip = address.getString("zip");
            final Object obj = address.get("is_residential");
            if (obj instanceof Boolean) {
                this.is_residential = address.getBoolean("is_residential");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
