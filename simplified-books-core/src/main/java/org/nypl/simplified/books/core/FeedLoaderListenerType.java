package org.nypl.simplified.books.core;

import java.net.URI;

public interface FeedLoaderListenerType
{
  void onFeedLoadSuccess(
    URI u,
    FeedType f);

  void onFeedLoadFailure(
    URI u,
    Throwable x);
}
