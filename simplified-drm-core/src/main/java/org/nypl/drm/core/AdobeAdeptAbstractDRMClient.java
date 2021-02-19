package org.nypl.drm.core;

import java.util.Objects;
import com.io7m.junreachable.UnreachableCodeException;
import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;

/**
 * An empty abstract implementation of the {@link AdobeAdeptDRMClientType}
 * interface.
 */

public abstract class AdobeAdeptAbstractDRMClient
  implements AdobeAdeptDRMClientType
{
  private final Logger log;

  protected AdobeAdeptAbstractDRMClient(final Logger in_log)
  {
    this.log = Objects.requireNonNull(in_log);
  }

  @Override public void onCompletelyFinished()
  {
    this.log.debug("onCompletelyFinished");
  }

  @Override public void onWorkflowsDone(
    final int w,
    final byte[] remaining)
  {
    try {
      this.log.debug(
        "onWorkflowsDone: {} {}",
        Integer.valueOf(w),
        new String(remaining, "UTF-8"));
    } catch (final UnsupportedEncodingException e) {
      throw new UnreachableCodeException(e);
    }
  }

  @Override public void onWorkflowProgress(
    final int w,
    final String title,
    final double progress)
  {
    this.log.debug(
      "onWorkflowProgress: {} {} {}",
      Integer.valueOf(w),
      title,
      Double.valueOf(progress));
  }

  @Override public void onWorkflowError(
    final int w,
    final String code)
  {
    this.log.debug("onWorkflowError: {} {}", Integer.valueOf(w), code);
  }

  @Override public void onFollowupURL(
    final int w,
    final String url)
  {
    this.log.debug("onFollowupURL: {} {}", Integer.valueOf(w), url);
  }

  @Override public void onDownloadCompleted(
    final String url,
    final byte[] rights,
    final boolean returnable,
    final String loan_id)
  {
    this.log.debug(
      "onDownloadCompleted: {} (rights: {} bytes) (returnable: {}) (loan_id: "
      + "{})",
      url,
      Integer.valueOf(rights.length),
      Boolean.valueOf(returnable),
      loan_id);
  }
}
