package org.nypl.simplified.opds.core;

import com.io7m.jnull.NullCheck;

import java.io.IOException;

/**
 * An {@link IOException} wrapper.
 */

public final class OPDSFeedTransportIOException
  extends OPDSFeedTransportException
{
  private final IOException cause;

  /**
   * Construct an exception.
   *
   * @param message  The message
   * @param in_cause The cause
   */
  public OPDSFeedTransportIOException(
    final String message,
    final IOException in_cause)
  {
    super(message, in_cause);
    this.cause = NullCheck.notNull(in_cause);
  }

  @Override public IOException getCause()
  {
    return this.cause;
  }
}
