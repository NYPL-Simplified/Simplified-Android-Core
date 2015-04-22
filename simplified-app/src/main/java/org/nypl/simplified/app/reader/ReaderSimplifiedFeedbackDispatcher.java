package org.nypl.simplified.app.reader;

import java.net.URI;

import org.nypl.simplified.app.utilities.LogUtilities;
import org.slf4j.Logger;

import com.io7m.jnull.NullCheck;

/**
 * The default implementation of the
 * {@link ReaderSimplifiedFeedbackDispatcherType} interface.
 */

public final class ReaderSimplifiedFeedbackDispatcher implements
  ReaderSimplifiedFeedbackDispatcherType
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(ReaderSimplifiedFeedbackDispatcher.class);
  }

  public static ReaderSimplifiedFeedbackDispatcherType newDispatcher()
  {
    return new ReaderSimplifiedFeedbackDispatcher();
  }

  private static void onGestureLeft(
    final ReaderSimplifiedFeedbackListenerType l)
  {
    try {
      l.onSimplifiedGestureLeft();
    } catch (final Throwable e) {
      try {
        l.onSimplifiedGestureLeftError(e);
      } catch (final Throwable x1) {
        ReaderSimplifiedFeedbackDispatcher.LOG.error(
          "{}",
          x1.getMessage(),
          x1);
      }
    }
  }

  private static void onGestureRight(
    final ReaderSimplifiedFeedbackListenerType l)
  {
    try {
      l.onSimplifiedGestureRight();
    } catch (final Throwable e) {
      try {
        l.onSimplifiedGestureRightError(e);
      } catch (final Throwable x1) {
        ReaderSimplifiedFeedbackDispatcher.LOG.error(
          "{}",
          x1.getMessage(),
          x1);
      }
    }
  }

  private ReaderSimplifiedFeedbackDispatcher()
  {

  }

  @Override public void dispatch(
    final URI uri,
    final ReaderSimplifiedFeedbackListenerType l)
  {
    NullCheck.notNull(uri);
    NullCheck.notNull(l);

    ReaderSimplifiedFeedbackDispatcher.LOG.debug("dispatching: {}", uri);

    /**
     * Note that all exceptions are caught here, as any exceptions raised
     * inside a callback called from a WebView tend to segfault the WebView.
     */

    try {
      final String data = NullCheck.notNull(uri.getSchemeSpecificPart());
      final String[] parts = NullCheck.notNull(data.split("/"));

      if (parts.length >= 1) {
        final String function = NullCheck.notNull(parts[0]);
        if ("gesture-left".equals(function)) {
          ReaderSimplifiedFeedbackDispatcher.onGestureLeft(l);
          return;
        }
        if ("gesture-right".equals(function)) {
          ReaderSimplifiedFeedbackDispatcher.onGestureRight(l);
          return;
        }
      }

      l.onSimplifiedFunctionUnknown(NullCheck.notNull(uri.toString()));
    } catch (final Throwable x) {
      try {
        l.onSimplifiedFunctionDispatchError(x);
      } catch (final Throwable x1) {
        ReaderSimplifiedFeedbackDispatcher.LOG.error(
          "{}",
          x1.getMessage(),
          x1);
      }
    }
  }
}
