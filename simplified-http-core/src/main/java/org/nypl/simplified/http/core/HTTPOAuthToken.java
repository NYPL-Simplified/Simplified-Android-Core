package org.nypl.simplified.http.core;

import com.google.auto.value.AutoValue;

/**
 * The type of tokens used in OAuth authentication.
 *
 * @see <a href="http://oauth.net">http://oauth.net</a>
 */

@AutoValue
public abstract class HTTPOAuthToken {

  HTTPOAuthToken() {

  }

  /**
   * Create a new OAuth token.
   *
   * @param value The token value
   * @return A new token
   */

  public static HTTPOAuthToken create(String value) {
    return new AutoValue_HTTPOAuthToken(value);
  }

  /**
   * @return The token value
   */

  public abstract String value();
}
