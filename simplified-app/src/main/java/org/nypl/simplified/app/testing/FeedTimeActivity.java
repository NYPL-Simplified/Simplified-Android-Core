package org.nypl.simplified.app.testing;

import java.net.URI;

import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedCatalogAppServicesType;
import org.nypl.simplified.books.core.FeedLoaderListenerType;
import org.nypl.simplified.books.core.FeedLoaderType;
import org.nypl.simplified.books.core.FeedType;

import android.app.Activity;
import android.os.Bundle;

import com.io7m.jnull.Nullable;

public final class FeedTimeActivity extends Activity implements
  FeedLoaderListenerType
{
  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final FeedLoaderType loader = app.getFeedLoader();
    loader.fromURI(
      URI.create("http://circulation.alpha.librarysimplified.org/popular/"),
      this);
  }

  @Override public void onFeedLoadFailure(
    final URI u,
    final Throwable x)
  {

  }

  @Override public void onFeedLoadSuccess(
    final URI u,
    final FeedType f)
  {

  }
}
