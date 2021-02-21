package org.librarysimplified.r2.views.internal

import androidx.recyclerview.widget.DiffUtil
import org.librarysimplified.r2.api.SR2BookChapter
import org.librarysimplified.r2.api.SR2Bookmark

internal object SR2Adapters {

  val bookmarkDiffCallback: DiffUtil.ItemCallback<SR2Bookmark> =
    object : DiffUtil.ItemCallback<SR2Bookmark>() {
      override fun areItemsTheSame(
        oldItem: SR2Bookmark,
        newItem: SR2Bookmark
      ): Boolean =
        oldItem == newItem

      override fun areContentsTheSame(
        oldItem: SR2Bookmark,
        newItem: SR2Bookmark
      ): Boolean =
        oldItem == newItem
    }

  val chapterDiffCallback: DiffUtil.ItemCallback<SR2BookChapter> =
    object : DiffUtil.ItemCallback<SR2BookChapter>() {
      override fun areItemsTheSame(
        oldItem: SR2BookChapter,
        newItem: SR2BookChapter
      ): Boolean =
        oldItem == newItem

      override fun areContentsTheSame(
        oldItem: SR2BookChapter,
        newItem: SR2BookChapter
      ): Boolean =
        oldItem == newItem
    }
}
