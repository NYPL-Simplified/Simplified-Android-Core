package org.nypl.simplified.bugsnag;

import com.bugsnag.android.Severity;

/**
 * BugsnagType
 */
public interface BugsnagType
{

  /**
   * Notify Bugsnag of a handled exception
   *
   * @param  exception  the exception to send to Bugsnag
   */
  void notify(final Throwable exception);

  /**
   * Notify Bugsnag of a handled exception
   *
   * @param exception the exception to send to Bugsnag
   * @param severity  the severity of the error, one of Severity.ERROR,
   *                  Severity.WARNING or Severity.INFO
   */
  void notify(final Throwable exception, final Severity severity);

  /**
   * Add diagnostic information to every error report.
   * Diagnostic information is collected in "tabs" on your dashboard.
   *
   * For example:
   *
   *     Bugsnag.addToTab("account", "name", "Acme Co.");
   *     Bugsnag.addToTab("account", "payingCustomer", true);
   *
   * @param  tab    the dashboard tab to add diagnostic data to
   * @param  key    the name of the diagnostic information
   * @param  value  the contents of the diagnostic information
   */
  void addToTab(final String tab, final String key, final Object value);

  /**
   * Leave a "breadcrumb" log message, representing an action that occurred
   * in your app, to aid with debugging.
   *
   * @param  message  the log message to leave (max 140 chars)
   */
  void leaveBreadcrumb(final String message);

  /**
   * Set the context sent to Bugsnag. By default we'll attempt to detect the
   * name of the top-most activity at the time of a notification, and use this
   * as the context, but sometime this is not possible.
   *
   * @param  context  set what was happening at the time of a crash
   */
  void setContext(final String context);

}
