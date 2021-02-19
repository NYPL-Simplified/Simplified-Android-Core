package org.nypl.drm.core;

import java.util.Objects;
import com.io7m.junreachable.UnreachableCodeException;
import org.slf4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * An adapter to convert the results of account joining as delivered by the Adobe
 * library into something developers would actually want.
 */

public final class AdobeAdeptJoinAccountAdapter
  extends AdobeAdeptAbstractDRMClient implements AdobeAdeptDRMClientType
{
  private final AdobeAdeptJoinAccountListenerType listener;
  private final Logger                            log;
  private       boolean                           completed;

  /**
   * Construct a join account adapter.
   *
   * @param in_log      A logger
   * @param in_listener The original fulfillment listener
   */

  public AdobeAdeptJoinAccountAdapter(
    final Logger in_log,
    final AdobeAdeptJoinAccountListenerType in_listener)
  {
    super(in_log);
    this.log = Objects.requireNonNull(in_log);
    this.listener = Objects.requireNonNull(in_listener);
  }

  @Override public void onFollowupURL(
    final int w,
    final String url)
  {
    super.onFollowupURL(w, url);
    try {
      this.listener.onJoinAccountStartingURL(new URL(url));
    } catch (final MalformedURLException e) {
      throw new UnreachableCodeException(e);
    }
  }

  @Override public void onWorkflowError(
    final int w,
    final String code)
  {
    super.onWorkflowError(w, code);
    this.listener.onJoinAccountFailure(code);
  }
}
