package org.nypl.simplified.bugsnag;


import com.bugsnag.android.Client;
import com.bugsnag.android.Severity;

/**
 * Bugsnag interface
 */
public class BugsnagWrapper implements BugsnagType
{
  private final Client client;

  /**
   * @param bugsnag_client Bugsnag client instance
   */
  public BugsnagWrapper(final Client bugsnag_client)
  {
    this.client = bugsnag_client;
  }

  /**
   * Notify Bugsnag of a handled exception
   *
   * @param exception the exception to send to Bugsnag
   */
  @Override public void notify(final Throwable exception)
  {
    this.client.notify(exception);
  }


  /**
   * Notify Bugsnag of a handled exception
   *
   * @param exception the exception to send to Bugsnag
   * @param severity  the severity of the error, one of Severity.ERROR,
   *                  Severity.WARNING or Severity.INFO
   */
  @Override public void notify(final Throwable exception, final Severity severity)
  {
    this.client.notify(exception, severity);
  }

  /**
   * Add diagnostic information to every error report.
   * Diagnostic information is collected in "tabs" on your dashboard.
   * <p/>
   * For example:
   * <p/>
   * Bugsnag.addToTab("account", "name", "Acme Co.");
   * Bugsnag.addToTab("account", "payingCustomer", true);
   *
   * @param tab   the dashboard tab to add diagnostic data to
   * @param key   the name of the diagnostic information
   * @param value the contents of the diagnostic information
   */
  @Override public void addToTab(final String tab, final String key, final Object value)
  {
    this.client.addToTab(tab, key, value);
  }


  /**
   * Leave a "breadcrumb" log message, representing an action that occurred
   * in your app, to aid with debugging.
   *
   * @param message the log message to leave (max 140 chars)
   */
  @Override public void leaveBreadcrumb(final String message)
  {
    this.client.leaveBreadcrumb(message);
  }
}
