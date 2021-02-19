package org.nypl.drm.core;

import java.util.Objects;
import org.slf4j.Logger;

/**
 * An adapter to convert the results of loan returns as delivered by the Adobe
 * library into something developers would actually want.
 */

public final class AdobeAdeptLoanReturnAdapter
  extends AdobeAdeptAbstractDRMClient implements AdobeAdeptDRMClientType
{
  private final AdobeAdeptLoanReturnListenerType listener;
  private final Logger                           log;
  private       boolean                          error;

  /**
   * Construct a loan return adapter.
   *
   * @param in_log      A logger
   * @param in_listener The original loan return listener
   */

  public AdobeAdeptLoanReturnAdapter(
    final Logger in_log,
    final AdobeAdeptLoanReturnListenerType in_listener)
  {
    super(in_log);
    this.log = Objects.requireNonNull(in_log);
    this.listener = Objects.requireNonNull(in_listener);
  }

  @Override public void onCompletelyFinished()
  {
    super.onCompletelyFinished();
    if (this.error == false) {
      this.listener.onLoanReturnSuccess();
    }
  }

  @Override public void onWorkflowError(
    final int w,
    final String code)
  {
    super.onWorkflowError(w, code);
    this.listener.onLoanReturnFailure(code);
  }
}
