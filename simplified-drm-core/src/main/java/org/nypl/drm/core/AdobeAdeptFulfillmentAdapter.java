package org.nypl.drm.core;

import java.util.Objects;
import org.slf4j.Logger;

import java.io.File;
import java.nio.ByteBuffer;

/**
 * An adapter to convert the results of fulfillment as delivered by the Adobe
 * library into something developers would actually want.
 */

public final class AdobeAdeptFulfillmentAdapter
  extends AdobeAdeptAbstractDRMClient implements AdobeAdeptDRMClientType
{
  private final AdobeAdeptFulfillmentListenerType listener;
  private final Logger                            log;
  private       boolean                           completed;

  /**
   * Construct a fulfillment adapter.
   *
   * @param in_log      A logger
   * @param in_listener The original fulfillment listener
   */

  public AdobeAdeptFulfillmentAdapter(
    final Logger in_log,
    final AdobeAdeptFulfillmentListenerType in_listener)
  {
    super(in_log);
    this.log = Objects.requireNonNull(in_log);
    this.listener = Objects.requireNonNull(in_listener);
  }

  @Override public void onDownloadCompleted(
    final String file,
    final byte[] rights,
    final boolean returnable,
    final String loan_id)
  {
    super.onDownloadCompleted(file, rights, returnable, loan_id);
    this.completed = true;

    try {
      final AdobeAdeptLoan r = new AdobeAdeptLoan(
        new AdobeLoanID(loan_id), ByteBuffer.wrap(rights), returnable);
      this.listener.onFulfillmentSuccess(new File(file), r);
    } catch (final Throwable e) {
      this.log.error("listener raised error: ", e);
    }
  }

  @Override public void onWorkflowError(
    final int w,
    final String code)
  {
    super.onWorkflowError(w, code);

    /**
     * The NYPL's net provider allows download cancellation. The cancellation
     * is signalled by notifying the downloading stream client of an error
     * {@code E_NYPL_CANCELLED}. The Adobe library propagates this error code
     * to this callback function, and can therefore be detected here.
     */

    if ("E_NYPL_CANCELLED".equals(code)) {
      try {
        this.listener.onFulfillmentCancelled();
      } catch (final Throwable e) {
        this.log.error("listener raised error: ", e);
      }
      return;
    }

    /**
     * Due to a bug in the connector, an error code {@code
     * E_ADEPT_DOCUMENT_CREATE_ERROR} will always be delivered after
     * #onDownloadCompleted has been called, regardless of what actually
     * happened. The only workaround is to ignore errors after the download
     * has completed.
     */

    if (this.completed == false) {
      try {
        this.listener.onFulfillmentFailure(code);
      } catch (final Throwable e) {
        this.log.error("listener raised error: ", e);
      }
    }
  }

  @Override public void onWorkflowProgress(
    final int w,
    final String title,
    final double progress)
  {
    super.onWorkflowProgress(w, title, progress);

    try {
      this.listener.onFulfillmentProgress(progress);
    } catch (final Throwable e) {
      this.log.error("listener raised error: ", e);
    }
  }
}
