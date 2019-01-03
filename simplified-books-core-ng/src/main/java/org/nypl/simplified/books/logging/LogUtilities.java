package org.nypl.simplified.books.logging;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities to obtain log handles, and format log messages.
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
    final OptionType<? extends Throwable> error)
  {
    if (error.isSome()) {
      final Some<Throwable> some = (Some<Throwable>) error;
      log.error(" ", some.get());
    } else {
      log.error("{}", message);
    }
  }

  /**
   * @param c A class
   *
   * @return A logger for the given class
   */

  public static Logger getLog(
    final Class<?> c)
  {
    return NullCheck.notNull(LoggerFactory.getLogger(c));
  }
}
