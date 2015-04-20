package org.nypl.simplified.app.reader;

import java.net.URI;

import android.util.Log;

import com.io7m.jnull.NullCheck;

/**
 * The default implementation of the
 * {@link ReaderSimplifiedFeedbackDispatcherType} interface.
 */

public final class ReaderSimplifiedFeedbackDispatcher implements
  ReaderSimplifiedFeedbackDispatcherType
{
  private static final String TAG = "RSFD";

  public static ReaderSimplifiedFeedbackDispatcherType newDispatcher()
  {
    return new ReaderSimplifiedFeedbackDispatcher();
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

    Log.d(ReaderSimplifiedFeedbackDispatcher.TAG, "dispatching: " + uri);

    l.onSimplifiedUnknownFunction(uri.toString());
  }
}
