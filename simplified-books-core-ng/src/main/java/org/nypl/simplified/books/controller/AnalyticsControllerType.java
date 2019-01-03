package org.nypl.simplified.books.controller;

/**
 * Interface into the logger for analytics information.
 */
public interface AnalyticsControllerType {

  /**
   * Writes the message out to the analytics log file.
   * This method is fail safe and suppresses all exceptions
   *
   * @param message The message to log.  Date and time is prepended.
   */
  void logToAnalytics(String message);

  /**
   * Pushes analytics to the analytics server if the log is full enough.
   */
  void attemptToPushAnalytics(String deviceId);
}
