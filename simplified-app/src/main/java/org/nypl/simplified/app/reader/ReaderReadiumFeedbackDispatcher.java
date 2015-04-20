package org.nypl.simplified.app.reader;

import java.net.URI;

import android.util.Log;

import com.io7m.jnull.NullCheck;

/**
 * The default implementation of the
 * {@link ReaderReadiumFeedbackDispatcherType} interface.
 */

public final class ReaderReadiumFeedbackDispatcher implements
  ReaderReadiumFeedbackDispatcherType
{
  private static final String TAG = "RRFD";

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
        Log.e(ReaderReadiumFeedbackDispatcher.TAG, x1.getMessage(), x1);
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

    Log.d(ReaderReadiumFeedbackDispatcher.TAG, "dispatching: " + uri);

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
      }

      l.onReadiumFunctionUnknown(NullCheck.notNull(uri.toString()));
    } catch (final Throwable x) {
      try {
        l.onReadiumFunctionDispatchError(x);
      } catch (final Throwable x1) {
        Log.e(ReaderReadiumFeedbackDispatcher.TAG, x1.getMessage(), x1);
      }
    }
  }
}
