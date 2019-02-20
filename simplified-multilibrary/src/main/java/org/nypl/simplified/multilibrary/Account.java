package org.nypl.simplified.multilibrary;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import com.io7m.junreachable.UnreachableCodeException;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.Serializable;

public class Account implements Serializable, Comparable<Account> {

  private Integer id;
  private String path_component;
  private String name;
  private String subtitle;
  private String logo;
  private boolean needs_auth;
  private boolean supports_simplye_sync;
  private boolean supports_barcode_scanner;
  private boolean supports_barcode_display;
  private boolean supports_reservations;
  private boolean supports_card_creator;
  private boolean supports_help_center;
  private String support_email;
  private String card_creator_url;
  private String catalog_url;
  private String main_color;
  private String eula;
  private String content_license;
  private String privacy_policy;
  private String catalog_url_under_13;
  private String catalog_url_13_and_over;
  private Integer pin_length;
  private KeyboardType login_keyboard;
  private KeyboardType pin_keyboard;

  public String getPrivacyPolicy() {
    return privacy_policy;
  }
  public String getContentLicense() {
    return content_license;
  }
  public String getSupportEmail() {
    return support_email;
  }
  public String getEula() {
    return eula;
  }
  public Integer getPinLength() { return pin_length; }
  public boolean pinRequired() { return this.pin_keyboard != KeyboardType.NONE; }
  public KeyboardType getLoginKeyboard() { return login_keyboard; }
  public KeyboardType getPinKeyboard() { return pin_keyboard; }

  public void setContentLicense(String in_contentLicense) {
    this.content_license = in_contentLicense;
  }
  public void setSupportEmail(String in_support_email) {
    this.support_email = in_support_email;
  }

  public void setEula(String in_eula) {
    this.eula = in_eula;
  }

  public void setPrivacyPolicy(String in_privacyPolicy) {
    this.privacy_policy = in_privacyPolicy;
  }

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
   * @return The logo image as a bitmap
   */
  public Bitmap getLogoBitmap()
    throws IllegalArgumentException
  {
    try {
      final String substring = this.logo.replace("data:image/png;base64,", "");
      final byte[] byteArray = Base64.decode(substring, Base64.DEFAULT);
      final Bitmap imageBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
      if (imageBitmap != null) {
        return imageBitmap;
      } else {
        throw new IllegalArgumentException();
      }
    } catch (Exception e) {
      throw new IllegalArgumentException();
    }
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
  public boolean needsAuth() {
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

  public String getCatalogUrlUnder13() {
    return this.catalog_url_under_13;
  }

  public String getCatalogUrl13AndOver() {
    return this.catalog_url_13_and_over;
  }

  public String getCardCreatorUrl() {
    return this.card_creator_url;
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
  public boolean supportsHelpCenter() {
    return supports_help_center;
  }

  /**
   * @param supports_card_creator
   */
  public void setSupportsCardCreator(final boolean supports_card_creator) {
    this.supports_card_creator = supports_card_creator;
  }

  public void setSupportsHelpCenter(final boolean supports_help_center) {
    this.supports_help_center = supports_help_center;
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
  public boolean supportsBarcodeScanner() {
    return supports_barcode_scanner;
  }

  /**
   * @param supports_barcode_scanner
   */
  public void setSupportsBarcodeScanner(final boolean supports_barcode_scanner) {
    this.supports_barcode_scanner = supports_barcode_scanner;
  }

  /**
   * @return
   */
  public boolean supportsBarcodeDisplay() {
    return supports_barcode_display;
  }

  /**
   * @param supports_barcode_display
   */
  public void setSupportsBarcodeDisplay(final boolean supports_barcode_display) {
    this.supports_barcode_display = supports_barcode_display;
  }

  /**
   * @return
   */
  public boolean supportsSimplyESync() {
    return supports_simplye_sync;
  }

  /**
   * @param supports_simplye_sync
   */
  public void setSupportsSimplyESync(final boolean supports_simplye_sync) {
    this.supports_simplye_sync = supports_simplye_sync;
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
      this.id = account.getInt("id_numeric");
      this.path_component = this.id.toString();
      this.name = account.getString("name");
      this.catalog_url = account.getString("catalogUrl");
      if (!account.isNull("subtitle")) {
        this.subtitle = account.getString("subtitle");
      }
      if (!account.isNull("logo")) {
        this.logo = account.getString("logo");
      }
      if (!account.isNull("catalogUrlUnder13")) {
        this.catalog_url_under_13 = account.getString("catalogUrlUnder13");
      }
      if (!account.isNull("catalogUrl13")) {
        this.catalog_url_13_and_over = account.getString("catalogUrl13");
      }
      if (!account.isNull("cardCreatorUrl")) {
        this.card_creator_url = account.getString("cardCreatorUrl");
      }
      if (!account.isNull("needsAuth")) {
        this.needs_auth = account.getBoolean("needsAuth");
      }
      if (!account.isNull("supportsReservations")) {
        this.supports_reservations = account.getBoolean("supportsReservations");
      }
      if (!account.isNull("supportsCardCreator")) {
        this.supports_card_creator = account.getBoolean("supportsCardCreator");
      }
      if (!account.isNull("supportsSimplyESync")) {
        this.supports_simplye_sync = account.getBoolean("supportsSimplyESync");
      }
      if (!account.isNull("supportsHelpCenter")) {
        this.supports_help_center = account.getBoolean("supportsHelpCenter");
      }
      if (!account.isNull("supportsBarcodeScanner")) {
        this.supports_barcode_scanner = account.getBoolean("supportsBarcodeScanner");
      }
      if (!account.isNull("supportsBarcodeDisplay")) {
        this.supports_barcode_display = account.getBoolean("supportsBarcodeDisplay");
      }
      if (!account.isNull("supportEmail")) {
        this.support_email = account.getString("supportEmail");
      }
      if (!account.isNull("mainColor")) {
        this.main_color = account.getString("mainColor");
      }
      if (!account.isNull("eulaUrl")) {
        this.eula = account.getString("eulaUrl");
      }
      if (!account.isNull("privacyUrl")) {
        this.privacy_policy = account.getString("privacyUrl");
      }
      if (!account.isNull("licenseUrl")) {
        this.content_license = account.getString("licenseUrl");
      }
      if (!account.isNull("authPasscodeLength")) {
        this.pin_length = account.getInt("authPasscodeLength");
      } else {
        this.pin_length = 0;
      }
      if (!account.isNull("loginKeyboard")) {
        switch (account.getString("loginKeyboard")) {
          case "Default":
            this.login_keyboard = KeyboardType.STANDARD;
            break;
          case "Email address":
            this.login_keyboard = KeyboardType.EMAIL;
            break;
          case "Number pad":
            this.login_keyboard = KeyboardType.NUMERIC;
            break;
          case "No input":
            this.login_keyboard = KeyboardType.NONE;
            break;
          default:
            throw new JSONException("Invalid login keyboard type.");
        }
      } else {
        this.login_keyboard = KeyboardType.STANDARD;
      }
      if (!account.isNull("pinKeyboard")) {
        switch (account.getString("pinKeyboard")) {
          case "Default":
            this.pin_keyboard = KeyboardType.STANDARD;
            break;
          case "Email address":
            this.pin_keyboard = KeyboardType.EMAIL;
            break;
          case "Number pad":
            this.pin_keyboard = KeyboardType.NUMERIC;
            break;
          case "No input":
            this.pin_keyboard = KeyboardType.NONE;
            break;
          default:
            throw new JSONException("Invalid pin keyboard type.");
        }
      } else {
        this.pin_keyboard = KeyboardType.NONE;
      }

    } catch (JSONException e) {
      throw new UnreachableCodeException(e);
    }
  }

  public enum KeyboardType {
    STANDARD("Default"),
    EMAIL("Email address"),
    NUMERIC("Number pad"),
    NONE("No input");

    private String value;
    public String getValue() {
      return this.value;
    }

    KeyboardType(String value) {
      this.value = value;
    }
  }

  /**
   * @return account as json object
   */
  public JSONObject getJsonObject() {
    final JSONObject object = new JSONObject();
    try {
      object.put("id_numeric", this.id);
      object.put("pathComponent", this.path_component);
      object.put("name", this.name);
      object.put("subtitle", this.subtitle);
      object.put("logo", this.logo);
      object.put("catalogUrl", this.catalog_url);
      object.put("supportEmail", this.support_email);
      object.put("eulaUrl", this.eula);
      object.put("licenseUrl", this.content_license);
      object.put("privacyUrl", this.privacy_policy);
      object.put("catalogUrlUnder13", this.catalog_url_under_13);
      object.put("catalogUrl13", this.catalog_url_13_and_over);
      object.put("needsAuth", this.needs_auth);
      object.put("supportsReservations", this.supports_reservations);
      object.put("supportsCardCreator", this.supports_card_creator);
      object.put("supportsHelpCenter", this.supports_help_center);
      object.put("cardCreatorUrl", this.card_creator_url);
      object.put("supportsSimplyESync", this.supports_simplye_sync);
      object.put("supportsBarcodeScanner", this.supports_barcode_scanner);
      object.put("supportsBarcodeDisplay", this.supports_barcode_display);
      object.put("authPasscodeLength", this.pin_length);
      object.put("mainColor", this.main_color);
      object.put("loginKeyboard", this.login_keyboard.getValue());
      object.put("pinKeyboard", this.pin_keyboard.getValue());
    } catch (JSONException e) {
      e.printStackTrace();
    }

    return object;
  }

  @Override
  public int compareTo(Account o) {
    return this.getName().compareTo(o.getName());
  }
}
