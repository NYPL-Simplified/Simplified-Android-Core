package org.nypl.simplified.http.core;

import java.net.HttpURLConnection;

public interface HTTPAuthType
{
  void setConnectionParameters(
    final HttpURLConnection c);
}
