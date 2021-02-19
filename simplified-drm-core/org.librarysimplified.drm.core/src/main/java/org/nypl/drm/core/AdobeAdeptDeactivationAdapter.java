package org.nypl.drm.core;

import java.util.Objects;
import org.slf4j.Logger;

/**
 * An adapter to convert the results of deactivation as delivered by the Adobe
 * library into something developers would actually want.
 */

public final class AdobeAdeptDeactivationAdapter
  extends AdobeAdeptAbstractDRMClient implements AdobeAdeptDRMClientType
{
  private final AdobeAdeptDeactivationReceiverType listener;
  private final Logger                             log;
  private       boolean                            error;

  /**
   * Construct a fulfillment adapter.
   *
   * @param in_log      A logger
   * @param in_listener The original fulfillment listener
   */

  public AdobeAdeptDeactivationAdapter(
    final Logger in_log,
    final AdobeAdeptDeactivationReceiverType in_listener)
  {
    super(in_log);
    this.log = Objects.requireNonNull(in_log);
    this.listener = Objects.requireNonNull(in_listener);
  }

  @Override public void onCompletelyFinished()
  {
    super.onCompletelyFinished();
    if (this.error == false) {
      this.listener.onDeactivationSucceeded();
    }
  }

  @Override public void onWorkflowError(
    final int w,
    final String code)
  {
    super.onWorkflowError(w, code);
    this.listener.onDeactivationError(code);
  }
}
