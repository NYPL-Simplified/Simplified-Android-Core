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

  void onFailure(
    final Exception e);

  /**
   * The feed was successfully parsed.
   *
   * @param f
   *          The resulting feed
   */

  void onSuccess(
    final OPDSFeedType f);
}
