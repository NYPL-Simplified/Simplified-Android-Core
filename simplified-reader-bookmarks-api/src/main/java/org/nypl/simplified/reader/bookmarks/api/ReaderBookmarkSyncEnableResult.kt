package org.nypl.simplified.reader.bookmarks.api

/**
 * The result of attempting to enable/disable syncing (assuming that the attempt didn't
 * outright fail with an exception).
 */

enum class ReaderBookmarkSyncEnableResult {
  SYNC_ENABLE_NOT_SUPPORTED,
  SYNC_ENABLED,
  SYNC_DISABLED
}
