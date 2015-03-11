package org.nypl.simplified.opds.core;

/**
 * The type of feed loading listeners.
 */

public interface OPDSFeedLoadListenerType
{
  /**
   * The feed could not be parsed (or possibly could not even be fetched).
   *
   * @param e
   *          The error, if any
   */

  void onFeedLoadingFailure(
    final Throwable e);

  /**
   * The feed was successfully parsed.
   *
   * @param f
   *          The resulting feed
   */

  void onFeedLoadingSuccess(
    final OPDSFeedType f);
}
