package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.junreachable.UnreachableCodeException;

import org.slf4j.Logger;

/**
 * Utilities to format log messages.
 */

public final class LogUtilities
{
  private LogUtilities()
  {
    throw new UnreachableCodeException();
  }

  /**
   * Format a log message with the given exception (if any).
   *
   * @param log     The log handle
   * @param message The message
   * @param error   The optional exception
   */

  public static void errorWithOptionalException(
    final Logger log,
    final String message,
    final OptionType<Throwable> error)
  {
    if (error.isSome()) {
      final Some<Throwable> some = (Some<Throwable>) error;
      log.error(" ", some.get());
    } else {
      log.error("{}", message);
    }
  }
}
