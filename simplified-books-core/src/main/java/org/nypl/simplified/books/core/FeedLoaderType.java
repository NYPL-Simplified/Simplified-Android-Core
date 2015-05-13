package org.nypl.simplified.books.core;

import java.net.URI;
import java.util.concurrent.Future;

import com.io7m.jfunctional.Unit;

public interface FeedLoaderType
{
  Future<Unit> fromURI(
    URI uri,
    FeedLoaderListenerType listener);

  Future<Unit> fromURIRefreshing(
    URI uri,
    FeedLoaderListenerType listener);
}
