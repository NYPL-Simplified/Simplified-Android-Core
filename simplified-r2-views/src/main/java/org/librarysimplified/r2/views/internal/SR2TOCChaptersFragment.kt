package org.librarysimplified.r2.views.internal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import org.librarysimplified.r2.api.SR2BookChapter
import org.librarysimplified.r2.api.SR2Command
import org.librarysimplified.r2.api.SR2ControllerType
import org.librarysimplified.r2.api.SR2Locator.SR2LocatorPercent
import org.librarysimplified.r2.views.R
import org.librarysimplified.r2.views.SR2ControllerHostType
import org.nypl.simplified.ui.thread.api.UIThread

internal class SR2TOCChaptersFragment : Fragment() {

  private lateinit var controller: SR2ControllerType
  private lateinit var readerModel: SR2ReaderViewModel
  private lateinit var controllerHost: SR2ControllerHostType
  private lateinit var chapterAdapter: SR2TOCChapterAdapter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.chapterAdapter =
      SR2TOCChapterAdapter(
        resources = this.resources,
        onChapterSelected = { this.onChapterSelected(it) })
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val layout =
      inflater.inflate(R.layout.sr2_toc_chapters, container, false)
    val recyclerView =
      layout.findViewById<RecyclerView>(R.id.tocChaptersList)

    recyclerView.adapter = this.chapterAdapter
    recyclerView.setHasFixedSize(true)
    recyclerView.setItemViewCacheSize(32)
    recyclerView.layoutManager = LinearLayoutManager(this.context)
    (recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
    return layout
  }

  override fun onStart() {
    super.onStart()

    val activity = this.requireActivity()
    this.controllerHost = activity as SR2ControllerHostType
    this.readerModel =
      ViewModelProviders.of(activity)
        .get(SR2ReaderViewModel::class.java)

    this.controller = this.readerModel.get()!!
    this.chapterAdapter.setChapters(controller.bookMetadata.readingOrder)
  }

  private fun onChapterSelected(chapter: SR2BookChapter) {
    this.controller.submitCommand(SR2Command.OpenChapter(SR2LocatorPercent(
      chapterIndex = chapter.chapterIndex,
      chapterProgress = 0.0
    )))

    UIThread.runOnUIThreadDelayed(Runnable {
      this.controllerHost.onNavigationClose()
    }, 1_000L)
  }
}
