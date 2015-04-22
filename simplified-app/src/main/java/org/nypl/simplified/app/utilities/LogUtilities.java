package org.nypl.simplified.app.utilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;

public final class LogUtilities
{
  public static Logger getLog(
    final Class<?> c)
  {
    return NullCheck.notNull(LoggerFactory.getLogger(c));
  }

  public static void errorWithOptionalException(
    final Logger log,
    final String message,
    final OptionType<Throwable> error)
  {
    if (error.isSome()) {
      final Some<Throwable> some = (Some<Throwable>) error;
      log.error("{}", message, some.get());
    } else {
      log.error("{}", message);
    }
  }
}
