package org.nypl.simplified.http.core;

import java.io.Serializable;
import java.net.HttpURLConnection;

public interface HTTPAuthType extends Serializable
{
  void setConnectionParameters(
    final HttpURLConnection c);
}
