package org.nypl.simplified.http.core;

import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;

public interface HTTPAuthType extends Serializable
{
  <A, E extends Exception> A matchAuthType(
    final HTTPAuthMatcherType<A, E> m)
    throws E;

  void setConnectionParameters(
    final HttpURLConnection c)
    throws IOException;
}
