package org.nypl.simplified.viewer.epub.readium1;

import com.io7m.jnull.NullCheck;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLDecoder;

/**
 * The default implementation of the {@link ReaderReadiumFeedbackDispatcherType}
 * interface.
 */

public final class ReaderReadiumFeedbackDispatcher
  implements ReaderReadiumFeedbackDispatcherType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(ReaderReadiumFeedbackDispatcher.class);

  private ReaderReadiumFeedbackDispatcher()
  {

  }

  /**
   * @return A new dispatcher
   */

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

  private static void onContentDocumentLoaded(
    final ReaderReadiumFeedbackListenerType l)
  {
    try {
      l.onReadiumContentDocumentLoaded();
    } catch (final Throwable e) {
      try {
        l.onReadiumContentDocumentLoadedError(e);
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
        l.onReadiumMediaOverlayStatusChangedIsPlaying(
          json.getBoolean("isPlaying"));
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

        if ("content-document-loaded".equals(function)) {
          ReaderReadiumFeedbackDispatcher.onContentDocumentLoaded(l);
          return;
        }

        if ("media-overlay-status-changed".equals(function)) {
          ReaderReadiumFeedbackDispatcher.onMediaOverlayStatusChanged(
            l, parts);
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
}
