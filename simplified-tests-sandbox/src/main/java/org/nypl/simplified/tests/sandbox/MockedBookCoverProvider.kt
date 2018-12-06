package org.nypl.simplified.tests.sandbox

import android.widget.ImageView
import com.google.common.util.concurrent.FluentFuture
import com.google.common.util.concurrent.Futures
import org.nypl.simplified.books.core.FeedEntryOPDS
import org.nypl.simplified.books.covers.BookCoverProviderType

class MockedBookCoverProvider : BookCoverProviderType {

  override fun loadingThumbailsPause() {

  }

  override fun loadingThumbnailsContinue() {

  }

  override fun loadThumbnailInto(entry: FeedEntryOPDS, imageView: ImageView, width: Int, height: Int): FluentFuture<Unit> {
    return FluentFuture.from(Futures.immediateFuture(Unit))
  }

  override fun loadCoverInto(entry: FeedEntryOPDS, imageView: ImageView, width: Int, height: Int): FluentFuture<Unit> {
    return FluentFuture.from(Futures.immediateFuture(Unit))
  }

}