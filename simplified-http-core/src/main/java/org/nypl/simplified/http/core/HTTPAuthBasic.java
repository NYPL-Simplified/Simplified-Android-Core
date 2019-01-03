package org.nypl.simplified.http.core;

import com.google.auto.value.AutoValue;
import com.io7m.jnull.NullCheck;

import net.iharder.Base64;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;

/**
 * HTTP Basic Auth functions.
 */

@AutoValue
public abstract class HTTPAuthBasic implements HTTPAuthType {

  HTTPAuthBasic() {

  }

  /**
   * Construct a basic auth value.
   *
   * @param user     The username
   * @param password The password
   */

  public static HTTPAuthBasic create(String user, String password) {
    return new AutoValue_HTTPAuthBasic(user, password);
  }

  /**
   * @return The user
   */

  public abstract String user();

  /**
   * @return The password
   */

  public abstract String password();

  @Override
  public final void setConnectionParameters(
      final HttpURLConnection c)
      throws IOException {
    NullCheck.notNull(c);

    final String text = this.user() + ":" + this.password();
    final String encoded = Base64.encodeBytes(text.getBytes(Charset.forName("US-ASCII")));
    c.addRequestProperty("Authorization", "Basic " + encoded);
  }

  @Override
  public final <A, E extends Exception> A matchAuthType(
      final HTTPAuthMatcherType<A, E> m)
      throws E {
    return m.onAuthBasic(this);
  }
}
