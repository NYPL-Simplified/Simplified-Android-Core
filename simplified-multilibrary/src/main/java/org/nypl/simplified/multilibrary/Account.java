package org.nypl.simplified.multilibrary;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by aferditamuriqi on 8/29/16.
 */

public class Account implements Serializable {


  private Integer id;
  private String path_component;
  private String name;
  private String logo;
  private Boolean needs_auth;
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
   * @param account The Json Account
   */
  public Account(final JSONObject account) {


    try {
      this.id = account.getInt("id");
      this.path_component = account.getString("pathComponent");
      this.name = account.getString("name");
      this.logo = account.getString("logo");
      this.catalog_url = account.getString("catalogUrl");
      this.needs_auth = account.getBoolean("needsAuth");

      if (!account.isNull("mainColor")) {
        this.main_color = account.getString("mainColor");
      }

    } catch (JSONException e) {
      e.printStackTrace();
    }

  }
}
