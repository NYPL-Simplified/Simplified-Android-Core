package org.nypl.simplified.volley;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;

import net.iharder.Base64;

import org.json.JSONObject;
import org.nypl.simplified.books.accounts.AccountAuthenticationCredentials;
import org.nypl.simplified.books.accounts.AccountBarcode;
import org.nypl.simplified.books.accounts.AccountPIN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by aferditamuriqi on 10/13/16.
 */

public class NYPLJsonObjectRequest extends JsonObjectRequest {

  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(JsonObjectRequest.class);
  }

  private AccountAuthenticationCredentials credentials;
  private String username;
  private String password;
  private Map<String, String> parameters;

  /**
   * @param method         request method
   * @param url            request url
   * @param body           request body
   * @param in_credentials account credentials
   * @param listener       response listener
   * @param error_listener error listener
   */
  public NYPLJsonObjectRequest(final int method,
                               final String url,
                               final String body,
                               final AccountAuthenticationCredentials in_credentials,
                               final Response.Listener<JSONObject> listener,
                               final Response.ErrorListener error_listener) {
    super(method, url, body, listener, error_listener);
    this.credentials = in_credentials;
  }

  /**
   * @param method         request method
   * @param url            request url
   * @param in_credentials account credentials
   * @param listener       response listener
   * @param error_listener error listener
   */
  public NYPLJsonObjectRequest(final int method,
                               final String url,
                               final AccountAuthenticationCredentials in_credentials,
                               final Response.Listener<JSONObject> listener,
                               final Response.ErrorListener error_listener) {
    super(method, url, listener, error_listener);
    this.credentials = in_credentials;
  }

  /**
   * @param method         request method
   * @param url            request url
   * @param body           request body
   * @param in_credentials account credentials
   * @param listener       response listener
   * @param error_listener error listener
   */
  public NYPLJsonObjectRequest(final int method,
                               final String url,
                               final JSONObject body,
                               final AccountAuthenticationCredentials in_credentials,
                               final Response.Listener<JSONObject> listener,
                               final Response.ErrorListener error_listener) {
    super(method, url, body, listener, error_listener);
    this.credentials = in_credentials;

  }

  /**
   * @param method         request method
   * @param url            request url
   * @param in_username    basic auth username
   * @param in_password    basic auth password
   * @param body           json body
   * @param listener       response listener
   * @param error_listener error listener
   */
  public NYPLJsonObjectRequest(final int method,
                               final String url,
                               final String in_username,
                               final String in_password,
                               final JSONObject body,
                               final Response.Listener<JSONObject> listener,
                               final Response.ErrorListener error_listener) {
    super(method, url, body, listener, error_listener);
    this.username = in_username;
    this.password = in_password;
  }

  /**
   * @param method         request method
   * @param url            request url
   * @param in_username    basic auth username
   * @param in_password    basic auth password
   * @param body           json body
   * @param parameters     any additional header parameters
   * @param listener       response listener
   * @param error_listener error listener
   */
  public NYPLJsonObjectRequest(final int method,
                               final String url,
                               final String in_username,
                               final String in_password,
                               final JSONObject body,
                               final Map<String, String> parameters,
                               final Response.Listener<JSONObject> listener,
                               final Response.ErrorListener error_listener) {
    super(method, url, body, listener, error_listener);
    this.username = in_username;
    this.password = in_password;
    this.parameters = parameters;
  }

  @Override
  public Map<String, String> getHeaders() throws AuthFailureError {

    final Map<String, String> params = new HashMap<String, String>();

    if (this.credentials != null) {

//      if (this.credentials.getAuthToken().isSome()) {
//        final AccountAuthToken token = ((Some<AccountAuthToken>) this.credentials.getAuthToken()).get();
//        params.put("Authorization", "Bearer " + token);
//        throw new UnimplementedCodeException();
//      }

      final AccountBarcode barcode = this.credentials.barcode();
      final AccountPIN pin = this.credentials.pin();

      final String text = barcode.toString() + ":" + pin.toString();
      final String encoded = Base64.encodeBytes(text.getBytes(Charset.forName("US-ASCII")));
      params.put("Authorization", "Basic " + encoded);
    } else {
      final String text = this.username + ":" + this.password;
      final String encoded = Base64.encodeBytes(text.getBytes(Charset.forName("US-ASCII")));
      params.put("Authorization", "Basic " + encoded);
    }

    if (this.parameters != null) {
      try {
        params.putAll(this.parameters);
      } catch (Exception e) {
        LOG.error("Abandoning request: Error putting parameters into JSON object request.");
      }
    }

    return params;
  }
}
