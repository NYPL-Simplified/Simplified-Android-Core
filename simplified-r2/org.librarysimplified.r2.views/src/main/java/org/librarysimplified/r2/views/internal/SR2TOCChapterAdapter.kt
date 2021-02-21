package org.librarysimplified.r2.views.internal

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.librarysimplified.r2.api.SR2BookChapter
import org.librarysimplified.r2.views.R
import org.librarysimplified.r2.views.internal.SR2Adapters.chapterDiffCallback
import org.librarysimplified.r2.views.internal.SR2TOCChapterAdapter.SR2TOCChapterViewHolder

internal class SR2TOCChapterAdapter(
  private val resources: Resources,
  private val onChapterSelected: (SR2BookChapter) -> Unit
) : ListAdapter<SR2BookChapter, SR2TOCChapterViewHolder>(chapterDiffCallback) {

  class SR2TOCChapterViewHolder(
    val rootView: View
  ) : RecyclerView.ViewHolder(rootView) {
    val chapterIcon: ImageView =
      rootView.findViewById(R.id.chapterIcon)
    val chapterTitleText: TextView =
      rootView.findViewById(R.id.chapterTitle)
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): SR2TOCChapterViewHolder {
    val inflater =
      LayoutInflater.from(parent.context)
    val chapterView =
      inflater.inflate(R.layout.sr2_toc_chapter_item, parent, false)
    return SR2TOCChapterViewHolder(chapterView)
  }

  override fun onBindViewHolder(
    holder: SR2TOCChapterViewHolder,
    position: Int
  ) {
    val chapter = this.getItem(position)
    holder.rootView.setOnClickListener {
      holder.rootView.setOnClickListener(null)
      this.onChapterSelected.invoke(chapter)
    }
    holder.chapterTitleText.text =
      this.resources.getString(
        R.string.tocChapterIndexed,
        chapter.chapterIndex + 1,
        chapter.title)
  }

  fun setChapters(chaptersNow: List<SR2BookChapter>) {
    this.submitList(chaptersNow)
  }
}
