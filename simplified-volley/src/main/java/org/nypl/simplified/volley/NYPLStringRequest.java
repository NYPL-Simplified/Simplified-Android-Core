package org.nypl.simplified.volley;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.Nullable;

import net.iharder.Base64;

import org.nypl.simplified.books.accounts.AccountAuthenticationCredentials;
import org.nypl.simplified.books.core.AccountAuthToken;
import org.nypl.simplified.books.core.AccountBarcode;
import org.nypl.simplified.books.core.AccountPIN;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by aferditamuriqi on 10/13/16.
 *
 */

public class NYPLStringRequest extends StringRequest {

  private @Nullable AccountAuthenticationCredentials credentials;
  private @Nullable String username;
  private @Nullable String password;
  private @Nullable String content_type;
  private @Nullable String body;


  /**
   * @param method request method
   * @param url request url
   * @param in_credentials account credentials
   * @param listener response listener
   * @param error_listener error listener
   */
  public NYPLStringRequest(final int method,
                           final String url,
                           final AccountAuthenticationCredentials in_credentials,
                           final Response.Listener<String> listener,
                           final Response.ErrorListener error_listener) {
    super(method, url, listener, error_listener);
    this.credentials = in_credentials;
  }

  /**
   * @param method request method
   * @param url request url
   * @param in_credentials account credentials
   * @param in_content_type  header contetn type
   * @param listener response listener
   * @param error_listener error listener
   */
  public NYPLStringRequest(final int method,
                           final String url,
                           final AccountAuthenticationCredentials in_credentials,
                           final String in_content_type,
                           final String in_body,
                           final Response.Listener<String> listener,
                           final Response.ErrorListener error_listener) {
    super(method, url, listener, error_listener);
    this.credentials = in_credentials;
    this.content_type = in_content_type;
    this.body = in_body;
  }

  /**
   * @param method request method
   * @param url request url
   * @param in_credentials account credentials
   * @param in_content_type  header contetn type
   * @param listener response listener
   * @param error_listener error listener
   */
  public NYPLStringRequest(final int method,
                           final String url,
                           final AccountAuthenticationCredentials in_credentials,
                           final String in_content_type,
                           final Response.Listener<String> listener,
                           final Response.ErrorListener error_listener) {
    super(method, url, listener, error_listener);
    this.credentials = in_credentials;
    this.content_type = in_content_type;
  }

  /**
   * @param url request url
   * @param in_credentials account credentials
   * @param listener response listener
   * @param error_listener error listener
   */
  public NYPLStringRequest(final String url,
                           final AccountAuthenticationCredentials in_credentials,
                           final Response.Listener<String> listener,
                           final Response.ErrorListener error_listener) {
    super(url, listener, error_listener);
    this.credentials = in_credentials;

  }

  /**
   * @param method request method
   * @param url request url
   * @param in_username basic auth username
   * @param in_password basic auth password
   * @param listener response listener
   * @param error_listener error listener
   */
  public NYPLStringRequest(final int method,
                           final String url,
                           final String in_username,
                           final String in_password,
                           final Response.Listener<String> listener,
                           final Response.ErrorListener error_listener) {
    super(method, url, listener, error_listener);
    this.username = in_username;
    this.password = in_password;
  }

  @Override
  public byte[] getBody() throws AuthFailureError {
    return this.body != null ? this.body.getBytes() : null;
  }

  @Override
  public Map<String, String> getHeaders() throws AuthFailureError {

    final Map<String, String> params = new HashMap<String, String>();

    if (this.credentials != null) {

      if (this.credentials.getAuthToken().isSome()) {

        final AccountAuthToken token = ((Some<AccountAuthToken>) this.credentials.getAuthToken()).get();
        params.put("Authorization", "Bearer " + token);

      } else {

        final AccountBarcode barcode = this.credentials.getBarcode();
        final AccountPIN pin = this.credentials.getPin();

        final String text = barcode.toString() + ":" + pin.toString();
        final String encoded =
          Base64.encodeBytes(text.getBytes(Charset.forName("US-ASCII")));

        params.put("Authorization", "Basic " + encoded);
      }
    }
    else
    {
      final String text = this.username + ":" + this.password;
      final String encoded =
        Base64.encodeBytes(text.getBytes(Charset.forName("US-ASCII")));
      params.put("Authorization", "Basic " + encoded);

    }

    if (this.content_type != null)
    {
      params.put("Content-Type",  this.content_type);
    }

    return params;
  }

}
