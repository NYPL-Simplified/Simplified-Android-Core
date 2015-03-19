package org.nypl.simplified.http.core;

import java.io.Closeable;

public interface HTTPResultOKType<A> extends Closeable, HTTPResultType<A>
{
  String getMessage();

  int getStatus();

  A getValue();
}
