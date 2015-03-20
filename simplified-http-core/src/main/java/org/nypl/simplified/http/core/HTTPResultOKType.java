package org.nypl.simplified.http.core;

import java.io.Closeable;

public interface HTTPResultOKType<A> extends
  Closeable,
  HTTPResultConnectedType<A>
{
  A getValue();
}
