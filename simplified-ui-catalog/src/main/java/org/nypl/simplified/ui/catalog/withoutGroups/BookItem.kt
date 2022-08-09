package org.nypl.simplified.ui.catalog.withoutGroups

import android.content.Context
import org.joda.time.DateTime
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.ui.catalog.withoutGroups.BookItem.Type.CORRUPT
import org.nypl.simplified.ui.catalog.withoutGroups.BookItem.Type.ERROR
import org.nypl.simplified.ui.catalog.withoutGroups.BookItem.Type.IDLE
import org.nypl.simplified.ui.catalog.withoutGroups.BookItem.Type.LOADING

sealed class BookItem {
  abstract val type: Type

  enum class Type { IDLE, CORRUPT, ERROR, LOADING }

  data class Idle(
    val entry: FeedEntry.FeedEntryOPDS,
    val actions: IdleActions,
    val loanExpiry: DateTime? = null,
    val downloadState: DownloadState? = null
  ) : BookItem() {
    override val type = IDLE
    val title: String? = entry.feedEntry.title
    val author: String? = entry.feedEntry.authorsCommaSeparated

    interface IdleActions {
      fun openBookDetail()
      fun primaryButton(): IdleButtonConfig?
      fun secondaryButton(): IdleButtonConfig?
    }

    data class IdleButtonConfig(
      val getText: (Context) -> String,
      val getDescription: (Context) -> String,
      val onClick: () -> Unit,
      val locked: Boolean = false
    )
  }

  data class Corrupt(
    val entry: FeedEntry.FeedEntryCorrupt,
  ) : BookItem() {
    override val type = CORRUPT
  }

  data class Error(
    val entry: FeedEntry.FeedEntryOPDS,
    val failure: TaskResult.Failure<Unit>,
    val actions: ErrorActions
  ) : BookItem() {
    override val type = ERROR
    val title: String? = entry.feedEntry.title

    interface ErrorActions {
      fun dismiss()
      fun details()
      fun retry()
    }
  }

  data class InProgress(
    val entry: FeedEntry.FeedEntryOPDS,
    val progress: Int? = null
  ) : BookItem() {
    override val type = LOADING
    val title: String? = entry.feedEntry.title
    val author: String? = entry.feedEntry.authorsCommaSeparated
    val isIndeterminate = progress == null
  }
}

sealed class DownloadState {
  abstract val progress: Int?
  fun isIndeterminate() = progress == null
  fun isComplete() = progress == 100

  object Complete: DownloadState() { override val progress = 100 }
  class InProgress(override val progress: Int? = null): DownloadState()
}
