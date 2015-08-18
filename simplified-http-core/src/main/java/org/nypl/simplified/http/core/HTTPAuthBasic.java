package org.nypl.simplified.http.core;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import net.iharder.Base64;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;

/**
 * HTTP Basic Auth functions.
 */

public final class HTTPAuthBasic implements HTTPAuthType
{
  private static final long serialVersionUID = 1L;
  private final String password;
  private final String user;

  /**
   * Construct a basic auth value.
   *
   * @param in_user     The username
   * @param in_password The password
   */

  public HTTPAuthBasic(
    final String in_user,
    final String in_password)
  {
    this.user = NullCheck.notNull(in_user);
    this.password = NullCheck.notNull(in_password);
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
    final HTTPAuthBasic other = (HTTPAuthBasic) obj;
    return this.password.equals(other.password) && this.user.equals(other.user);
  }

  /**
   * @return The password
   */

  public String getPassword()
  {
    return this.password;
  }

  /**
   * @return The user
   */

  public String getUser()
  {
    return this.user;
  }

  @Override public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = (prime * result) + this.password.hashCode();
    result = (prime * result) + this.user.hashCode();
    return result;
  }

  @Override public void setConnectionParameters(
    final HttpURLConnection c)
    throws IOException
  {
    NullCheck.notNull(c);

    final String text = this.user + ":" + this.password;
    final String encoded =
      Base64.encodeBytes(text.getBytes(Charset.forName("US-ASCII")));

    c.addRequestProperty("Authorization", "Basic " + encoded);
  }

  @Override public <A, E extends Exception> A matchAuthType(
    final HTTPAuthMatcherType<A, E> m)
    throws E
  {
    return m.onAuthBasic(this);
  }
}
