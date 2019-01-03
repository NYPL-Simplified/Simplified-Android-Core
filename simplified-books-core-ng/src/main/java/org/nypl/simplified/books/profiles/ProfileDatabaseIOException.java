package org.nypl.simplified.books.profiles;

import com.io7m.jnull.NullCheck;

import java.io.IOException;
import java.util.Collections;

/**
 * An exception caused by an underlying I/O exception.
 */

public final class ProfileDatabaseIOException extends ProfileDatabaseException {

  /**
   * Construct an exception.
   *
   * @param message The exception message
   */

  public ProfileDatabaseIOException(
      final String message,
      final IOException exception) {
    super(message, Collections.singletonList(NullCheck.notNull(exception, "exception")));
  }
}
