package org.nypl.drm.core;

import android.net.Uri;

/**
 * The type of listeners that receive the results of completing the Join
 * Accounts workflow.
 */

public interface AdobeAdeptJoinAccountDispatcherListenerType
{
  /**
   * An exception was raised during the join accounts form submission.
   *
   * @param e The exception
   */

  void onJoinAccountsException(final Throwable e);

  /**
   * Called just prior to executing the POST query (to complete the form
   * submission).
   *
   * This method is provided to assist with unit testing.
   *
   * @param builder The builder used to produce the query.
   *
   * @return <tt>true</tt> If the query should proceed.
   */

  boolean onPreparedQuery(final Uri.Builder builder);

  /**
   * An HTML page was received.
   *
   * @param text The text of the page
   */

  void onReceivedHTMLPage(final String text);

  /**
   * An ACSM file was received.
   *
   * @param text The text of the ACSM
   */

  void onReceivedACSM(String text);
}
