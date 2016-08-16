package org.nypl.simplified.http.core;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * HTTP Basic Auth functions.
 */

public final class HTTPAuthOAuth implements HTTPAuthType
{
  private static final long serialVersionUID = 1L;
//  private final String password;
//  private final String user;
  private final String token;

  /**
   * Construct a basic auth value.
   *
   * @param in_token The Token
   */

  public HTTPAuthOAuth(
    final String in_token)
  {
    this.token = NullCheck.notNull(in_token);
  }

  @Override public boolean equals(
    final @Nullable Object obj)
  {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (this.getClass() != obj.getClass()) {
      return false;
    }
    final HTTPAuthOAuth other = (HTTPAuthOAuth) obj;
    return this.token.equals(other.token);
  }

  /**
   * @return The token
   */

  public String getToken()
  {
    return this.token;
  }

  @Override public int hashCode()
  {
    final int prime = 31;
    int result = 1;
//    result = (prime * result) + this.password.hashCode();
//    result = (prime * result) + this.user.hashCode();
    result = (prime * result) + this.token.hashCode();
    return result;
  }

  @Override public void setConnectionParameters(
    final HttpURLConnection c)
    throws IOException
  {
    NullCheck.notNull(c);

//    final String text = this.user + ":" + this.password;
//    final String encoded =
//      Base64.encodeBytes(text.getBytes(Charset.forName("US-ASCII")));

    c.addRequestProperty("Authorization", "Bearer " + this.token);

    //Authorization: Bearer 0b79bab50daca910b000d4f1a2b675d604257e42
  }

  @Override public <A, E extends Exception> A matchAuthType(
    final HTTPAuthMatcherType<A, E> m)
    throws E
  {
    return m.onAuthOAuth(this);
  }
}
