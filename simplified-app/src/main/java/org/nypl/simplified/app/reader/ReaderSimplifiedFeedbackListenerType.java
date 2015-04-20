package org.nypl.simplified.app.reader;

/**
 * Functions called via the <tt>host_app_feedback.js</tt> file using the
 * <tt>simplified</tt> URI scheme.
 */

public interface ReaderSimplifiedFeedbackListenerType
{
  void onSimplifiedUnknownFunction(
    String text);
}
