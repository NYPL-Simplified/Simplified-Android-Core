package org.nypl.simplified.bugsnag;


import com.bugsnag.android.Severity;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.AppenderBase;

/**
 * Custom Bugsnag Logback Appender
 */
public class BugsnagLogbackAppender extends AppenderBase<ILoggingEvent>
{
  /**
   * Construct the appender
   */
  public BugsnagLogbackAppender()
  {
    super();
  }

  /**
   * @param event The logging event
   */
  @Override protected void append(final ILoggingEvent event)
  {
    final Level level = event.getLevel();
    final IThrowableProxy throwable_proxy = event.getThrowableProxy();

    final BugsnagType bugsnag = IfBugsnag.get();

    if ((level == Level.ERROR || level == Level.WARN)
      && (throwable_proxy != null && throwable_proxy instanceof ThrowableProxy)) {
      final ThrowableProxy throwable_proxy_impl = (ThrowableProxy) throwable_proxy;

      bugsnag.setContext(event.getLoggerName());
      bugsnag.notify(throwable_proxy_impl.getThrowable(),
        level == Level.ERROR ? Severity.ERROR : Severity.WARNING);

    } else if (level == Level.DEBUG || level == Level.INFO) {
      bugsnag.leaveBreadcrumb(event.getLoggerName() + ": " + event.getFormattedMessage());
    }
  }
}
