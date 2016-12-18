package org.nypl.simplified.multilibrary;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by aferditamuriqi on 8/29/16.
 *
 */

public class Account implements Serializable {

  private Integer id;
  private String path_component;
  private String name;
  private String subtitle;
  private String logo;
  private Boolean needs_auth;
  private Boolean supports_reservations;
  private Boolean supports_card_creator;
  private String catalog_url;
  private String main_color;


  /**
   * @return the ID
   */
  public int getId() {
    return this.id;
  }

  /**
   * @param in_id The ID
   */
  public void setId(final int in_id) {
    this.id = in_id;
  }

  /**
   * @return the Path Component
   */
  public String getPathComponent() {
    return this.path_component;
  }

  /**
   * @param in_path_component The Path Component
   */
  public void setPathComponent(final String in_path_component) {
    this.path_component = in_path_component;
  }

  /**
   * @return The Name
   */
  public String getName() {
    return this.name;
  }

  /**
   * @param in_name The Name
   */
  public void setName(final String in_name) {
    this.name = in_name;
  }

  /**
   * @return The Logo
   */
  public String getLogo() {
    return this.logo;
  }

  /**
   * @param in_logo The Logo
   */
  public void setLogo(final String in_logo) {
    this.logo = in_logo;
  }

  /**
   * @return Needs Authentication
   */
  public boolean isNeedsAuth() {
    return this.needs_auth;
  }

  /**
   * @param in_needs_auth Needs Authentication
   */
  public void setNeedsAuth(final boolean in_needs_auth) {
    this.needs_auth = in_needs_auth;
  }

  /**
   * @return The Catalog Main URL
   */
  public String getCatalogUrl() {
    return this.catalog_url;
  }

  /**
   * @param in_catalog_url The Catalog Main URL
   */
  public void setCatalogUrl(final String in_catalog_url) {
    this.catalog_url = in_catalog_url;
  }

  /**
   * @return The Main Color
   */
  public String getMainColor() {
    return this.main_color;
  }

  /**
   * @param in_main_color The Main Color
   */
  public void setMainColor(final String in_main_color) {
    this.main_color = in_main_color;
  }

  /**
   * @return
   */
  public boolean supportsCardCreator() {
    return supports_card_creator;
  }

  /**
   * @param supports_card_creator
   */
  public void setSupportsCardCreator(final boolean supports_card_creator) {
    this.supports_card_creator = supports_card_creator;
  }

  /**
   * @return
   */
  public boolean supportsReservations() {
    return supports_reservations;
  }

  /**
   * @param supports_reservations
   */
  public void setSupportsReservations(final boolean supports_reservations) {
    this.supports_reservations = supports_reservations;
  }

  /**
   * @return
   */
  public String getSubtitle() {
    return subtitle;
  }

  /**
   * @param subtitle
   */
  public void setSubtitle(final String subtitle) {
    this.subtitle = subtitle;
  }



  /**
   * @param account The Json Account
   */
  public Account(final JSONObject account) {


    try {
      this.id = account.getInt("id");
      this.path_component = account.getString("pathComponent");
      this.name = account.getString("name");
      this.subtitle = account.getString("subtitle");
      this.logo = account.getString("logo");
      this.catalog_url = account.getString("catalogUrl");
      this.needs_auth = account.getBoolean("needsAuth");
      this.supports_reservations = account.getBoolean("supportsReservations");
      this.supports_card_creator = account.getBoolean("supportsCardCreator");

      if (!account.isNull("mainColor")) {
        this.main_color = account.getString("mainColor");
      }

    } catch (JSONException e) {
      e.printStackTrace();
    }

  }

  /**
   * @return account as json object
   */
  public JSONObject getJsonObject()
  {
    final JSONObject object = new JSONObject();
    try {
      object.put("id",this.id);
      object.put("pathComponent",this.path_component);
      object.put("name",this.name);
      object.put("subtitle",this.subtitle);
      object.put("logo",this.logo);
      object.put("catalogUrl",this.catalog_url);
      object.put("needsAuth",this.needs_auth);
      object.put("supportsReservations",this.supports_reservations);
      object.put("supportsCardCreator",this.supports_card_creator);
      object.put("mainColor",this.main_color);
    } catch (JSONException e) {
      e.printStackTrace();
    }

    return object;
  }
}
