package org.nypl.drm.core;

import java.io.File;

/**
 * The type of listeners that receive the results of fulfillment operations.
 *
 * @see AdobeAdeptConnectorType#fulfillACSM(AdobeAdeptFulfillmentListenerType, byte[], AdobeUserID)
 */

public interface AdobeAdeptFulfillmentListenerType
{
  /**
   * Downloading the book failed.
   *
   * @param error The error message
   */

  void onFulfillmentFailure(String error);

  /**
   * Downloading the book succeeded. A temporary copy of the book is currently
   * at {@code file} and must be copied to a new location before this method
   * returns - the temporary copy will be automatically deleted. Rights
   * information to the book are given in {@code rights} and should be saved for
   * later use.
   *
   * @param file   The temporary book
   * @param rights The book rights
   */

  void onFulfillmentSuccess(
    File file,
    AdobeAdeptLoan rights);

  /**
   * The current download progress is {@code progress}, where {@code 0.0 <=
   * progress <= 1.0}.
   *
   * @param progress The current download progress
   */

  void onFulfillmentProgress(
    double progress);

  /**
   * The user cancelled the download in progress.
   */

  void onFulfillmentCancelled();
}
