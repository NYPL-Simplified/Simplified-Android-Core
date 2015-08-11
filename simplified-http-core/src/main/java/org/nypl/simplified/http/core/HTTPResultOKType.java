package org.nypl.simplified.http.core;

import java.io.Closeable;

/**
 * The type of results that do not indicate errors.
 *
 * @param <A> The type of result values
 */

public interface HTTPResultOKType<A>
  extends Closeable, HTTPResultConnectedType<A>
{
  /**
   * @return The result value
   */

  A getValue();
}
