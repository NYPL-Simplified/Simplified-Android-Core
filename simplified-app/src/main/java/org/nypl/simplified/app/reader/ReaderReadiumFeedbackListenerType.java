package org.nypl.simplified.app.reader;

/**
 * Functions called via the <tt>host_app_feedback.js</tt> file using the
 * <tt>readium</tt> URI scheme.
 */

public interface ReaderReadiumFeedbackListenerType
{
  void onReadiumFunctionDispatchError(
    Throwable x);

  void onReadiumFunctionInitialize();

  void onReadiumFunctionInitializeError(
    Throwable e);

  void onReadiumFunctionUnknown(
    String text);
}
