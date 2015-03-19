package org.nypl.simplified.http.core;

import java.net.HttpURLConnection;

import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnimplementedCodeException;

public final class HTTPAuthBasic implements HTTPAuthType
{
  private final String password;
  private final String user;

  public HTTPAuthBasic(
    final String in_user,
    final String in_password)
  {
    this.user = NullCheck.notNull(in_user);
    this.password = NullCheck.notNull(in_password);
  }

  @Override public boolean equals(
    final Object obj)
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
    return this.password.equals(other.password)
      && this.user.equals(other.user);
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
  {
    NullCheck.notNull(c);
    throw new UnimplementedCodeException();
  }
}
