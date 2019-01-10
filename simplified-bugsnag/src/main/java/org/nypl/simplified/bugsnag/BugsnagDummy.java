package org.nypl.simplified.bugsnag;


import com.bugsnag.android.Severity;

import org.nypl.simplified.books.core.LogUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bugsnag no-op interface
 */
public class BugsnagDummy implements BugsnagType
{
  private static final Logger LOG = LoggerFactory.getLogger(BugsnagDummy.class);

  /**
   *
   */
  public BugsnagDummy()
  {
    LOG.debug("created.");
  }

  /**
   * @param exception Exception
   */
  @Override public void notify(final Throwable exception)
  {
    LOG.trace("notify: ", exception);
  }


  /**
   * @param exception Exception
   * @param severity  Severity
   */
  @Override public void notify(final Throwable exception, final Severity severity)
  {
    LOG.trace("notify: ", exception, severity);
  }

  /**
   * @param tab   Tab
   * @param key   Key
   * @param value Value
   */
  @Override public void addToTab(final String tab, final String key, final Object value)
  {
    LOG.trace("addToTab: ", tab, key, value);
  }

  /**
   * @param message Message
   */
  @Override public void leaveBreadcrumb(final String message)
  {
    LOG.trace("leaveBreadcrumb: ", message);
  }

  /**
   * @param context Context
   */
  @Override public void setContext(final String context)
  {
    LOG.trace("setContext: ", context);
  }
}
