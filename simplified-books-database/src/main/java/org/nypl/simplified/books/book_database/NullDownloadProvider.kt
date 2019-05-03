package org.nypl.simplified.books.book_database

import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import org.nypl.audiobook.android.api.PlayerDownloadProviderType
import org.nypl.audiobook.android.api.PlayerDownloadRequest

/**
 * A download provider that does nothing.
 */

internal class NullDownloadProvider : PlayerDownloadProviderType {
  override fun download(request: PlayerDownloadRequest): ListenableFuture<Unit> {
    return Futures.immediateFailedFuture(UnsupportedOperationException())
  }
}