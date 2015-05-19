package org.nypl.simplified.app.reader;

import java.net.URI;
import java.net.URLDecoder;

import org.json.JSONObject;
import org.nypl.simplified.app.utilities.LogUtilities;
import org.slf4j.Logger;

import com.io7m.jnull.NullCheck;

/**
 * The default implementation of the
 * {@link ReaderReadiumFeedbackDispatcherType} interface.
 */

public final class ReaderReadiumFeedbackDispatcher implements
  ReaderReadiumFeedbackDispatcherType
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(ReaderReadiumFeedbackDispatcher.class);
  }

  public static ReaderReadiumFeedbackDispatcherType newDispatcher()
  {
    return new ReaderReadiumFeedbackDispatcher();
  }

  private static void onInitialize(
    final ReaderReadiumFeedbackListenerType l)
  {
    try {
      l.onReadiumFunctionInitialize();
    } catch (final Throwable e) {
      try {
        l.onReadiumFunctionInitializeError(e);
      } catch (final Throwable x1) {
        ReaderReadiumFeedbackDispatcher.LOG.error("{}", x1.getMessage(), x1);
      }
    }
  }

  private static void onMediaOverlayStatusChanged(
    final ReaderReadiumFeedbackListenerType l,
    final String[] parts)
  {
    try {
      if (parts.length < 2) {
        throw new IllegalArgumentException(
          "Expected media overlay data, but got nothing");
      }

      final String encoded = NullCheck.notNull(parts[1]);
      final String decoded =
        NullCheck.notNull(URLDecoder.decode(encoded, "UTF-8"));
      final JSONObject json = new JSONObject(decoded);

      ReaderReadiumFeedbackDispatcher.LOG.debug("media-overlay: {}", json);

      if (json.has("isPlaying")) {
        l.onReadiumMediaOverlayStatusChangedIsPlaying(json
          .getBoolean("isPlaying"));
      }

    } catch (final Throwable e) {
      try {
        l.onReadiumMediaOverlayStatusError(e);
      } catch (final Throwable x1) {
        ReaderReadiumFeedbackDispatcher.LOG.error("{}", x1.getMessage(), x1);
      }
    }
  }

  private static void onPaginationChanged(
    final ReaderReadiumFeedbackListenerType l,
    final String[] parts)
  {
    try {
      if (parts.length < 2) {
        throw new IllegalArgumentException(
          "Expected pagination data, but got nothing");
      }

      final String encoded = NullCheck.notNull(parts[1]);
      final String decoded =
        NullCheck.notNull(URLDecoder.decode(encoded, "UTF-8"));
      final JSONObject json = new JSONObject(decoded);
      final ReaderPaginationChangedEvent e =
        ReaderPaginationChangedEvent.fromJSON(json);

      l.onReadiumFunctionPaginationChanged(e);
    } catch (final Throwable e) {
      try {
        l.onReadiumFunctionPaginationChangedError(e);
      } catch (final Throwable x1) {
        ReaderReadiumFeedbackDispatcher.LOG.error("{}", x1.getMessage(), x1);
      }
    }
  }

  private static void onSettingsApplied(
    final ReaderReadiumFeedbackListenerType l)
  {
    try {
      l.onReadiumFunctionSettingsApplied();
    } catch (final Throwable e) {
      try {
        l.onReadiumFunctionSettingsAppliedError(e);
      } catch (final Throwable x1) {
        ReaderReadiumFeedbackDispatcher.LOG.error("{}", x1.getMessage(), x1);
      }
    }
  }

  private ReaderReadiumFeedbackDispatcher()
  {

  }

  @Override public void dispatch(
    final URI uri,
    final ReaderReadiumFeedbackListenerType l)
  {
    NullCheck.notNull(uri);
    NullCheck.notNull(l);

    ReaderReadiumFeedbackDispatcher.LOG.debug("dispatching: {}", uri);

    /**
     * Note that all exceptions are caught here, as any exceptions raised
     * inside a callback called from a WebView tend to segfault the WebView.
     */

    try {
      final String data = NullCheck.notNull(uri.getSchemeSpecificPart());
      final String[] parts = NullCheck.notNull(data.split("/"));

      if (parts.length >= 1) {
        final String function = NullCheck.notNull(parts[0]);
        if ("initialize".equals(function)) {
          ReaderReadiumFeedbackDispatcher.onInitialize(l);
          return;
        }

        if ("media-overlay-status-changed".equals(function)) {
          ReaderReadiumFeedbackDispatcher.onMediaOverlayStatusChanged(
            l,
            parts);
          return;
        }

        if ("media-overlay-is-available".equals(function)) {
          ReaderReadiumFeedbackDispatcher.onMediaOverlayIsAvailable(l, parts);
          return;
        }

        if ("pagination-changed".equals(function)) {
          ReaderReadiumFeedbackDispatcher.onPaginationChanged(l, parts);
          return;
        }

        if ("settings-applied".equals(function)) {
          ReaderReadiumFeedbackDispatcher.onSettingsApplied(l);
          return;
        }
      }

      l.onReadiumFunctionUnknown(NullCheck.notNull(uri.toString()));
    } catch (final Throwable x) {
      try {
        l.onReadiumFunctionDispatchError(x);
      } catch (final Throwable x1) {
        ReaderReadiumFeedbackDispatcher.LOG.error("{}", x1.getMessage(), x1);
      }
    }
  }

  private static void onMediaOverlayIsAvailable(
    final ReaderReadiumFeedbackListenerType l,
    final String[] parts)
  {
    try {
      if (parts.length < 2) {
        throw new IllegalArgumentException(
          "Expected media overlay data, but got nothing");
      }

      final String encoded = NullCheck.notNull(parts[1]);
      final Boolean decoded = NullCheck.notNull(Boolean.valueOf(encoded));
      l.onReadiumMediaOverlayStatusChangedIsAvailable(decoded.booleanValue());
    } catch (final Throwable e) {
      try {
        l.onReadiumMediaOverlayStatusError(e);
      } catch (final Throwable x1) {
        ReaderReadiumFeedbackDispatcher.LOG.error("{}", x1.getMessage(), x1);
      }
    }
  }
}
