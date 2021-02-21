package org.librarysimplified.r2.views.internal

import android.content.res.Resources
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import org.joda.time.format.DateTimeFormatterBuilder
import org.librarysimplified.r2.api.SR2Bookmark
import org.librarysimplified.r2.api.SR2Bookmark.Type.EXPLICIT
import org.librarysimplified.r2.api.SR2Bookmark.Type.LAST_READ
import org.librarysimplified.r2.api.SR2Locator
import org.librarysimplified.r2.views.R

internal class SR2TOCBookmarkViewHolder(
  val rootView: View
) : RecyclerView.ViewHolder(rootView) {

  private val bookmarkIcon: ImageView =
    this.rootView.findViewById(R.id.bookmarkIcon)
  private val bookmarkDelete: ImageView =
    this.rootView.findViewById(R.id.bookmarkDelete)
  private val bookmarkDate: TextView =
    this.rootView.findViewById(R.id.bookmarkDate)
  private val bookmarkProgressText: TextView =
    this.rootView.findViewById(R.id.bookmarkProgressText)
  private val bookmarkTitleText: TextView =
    this.rootView.findViewById(R.id.bookmarkTitle)

  companion object {
    private val FORMATTER =
      DateTimeFormatterBuilder()
        .appendYear(4, 5)
        .appendLiteral('-')
        .appendMonthOfYear(2)
        .appendLiteral('-')
        .appendDayOfMonth(2)
        .appendLiteral(' ')
        .appendHourOfDay(2)
        .appendLiteral(':')
        .appendMinuteOfHour(2)
        .appendLiteral(':')
        .appendSecondOfMinute(2)
        .toFormatter()
  }

  fun bindTo(
    resources: Resources,
    onBookmarkSelected: (SR2Bookmark) -> Unit,
    onBookmarkDeleteRequested: (SR2Bookmark) -> Unit,
    bookmark: SR2Bookmark
  ) {
    when (bookmark.type) {
      EXPLICIT -> {
        this.bookmarkIcon.setImageResource(R.drawable.sr2_bookmark)
        this.bookmarkDelete.visibility = View.VISIBLE
        this.bookmarkDelete.setOnClickListener {
          this.openDeleteDialog(bookmark, onBookmarkDeleteRequested)
        }
      }
      LAST_READ -> {
        this.bookmarkIcon.setImageResource(R.drawable.sr2_last_read)
        this.bookmarkDelete.visibility = View.INVISIBLE
      }
    }

    this.rootView.setOnClickListener {
      this.rootView.setOnClickListener(null)
      onBookmarkSelected.invoke(bookmark)
    }
    this.bookmarkTitleText.text = bookmark.title
    this.bookmarkDate.text = FORMATTER.print(bookmark.date)
    this.bookmarkProgressText.text =
      when (val locator = bookmark.locator) {
        is SR2Locator.SR2LocatorPercent -> {
          val percent = (locator.chapterProgress * 100.0).toInt()
          resources.getString(R.string.bookmarkProgressPercent, percent)
        }
        is SR2Locator.SR2LocatorChapterEnd ->
          resources.getString(R.string.bookmarkEnd)
      }
  }

  private fun openDeleteDialog(
    bookmark: SR2Bookmark,
    onBookmarkDeleteRequested: (SR2Bookmark) -> Unit
  ) {
    AlertDialog.Builder(this.rootView.context)
      .setMessage(R.string.tocBookmarkDeleteMessage)
      .setPositiveButton(R.string.tocBookmarkDelete) { dialog, which ->
        onBookmarkDeleteRequested.invoke(bookmark)
        dialog.dismiss()
      }.show()
  }
}
