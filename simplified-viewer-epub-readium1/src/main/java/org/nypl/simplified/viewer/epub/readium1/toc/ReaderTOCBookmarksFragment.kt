package org.nypl.simplified.viewer.epub.readium1.toc

import android.content.Context
import android.database.DataSetObserver
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import org.joda.time.format.DateTimeFormat
import org.librarysimplified.services.api.Services
import org.nypl.simplified.app.reader.ReaderColorSchemes
import org.nypl.simplified.viewer.epub.readium1.toc.ReaderTOCSelection.ReaderSelectedBookmark
import org.nypl.simplified.books.api.Bookmark
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.viewer.epub.readium1.R
import org.slf4j.LoggerFactory

/**
 * A reusable fragment for a ListView of bookmarks
 */

class ReaderTOCBookmarksFragment : Fragment(), ListAdapter {

  private val logger = LoggerFactory.getLogger(ReaderTOCBookmarksFragment::class.java)

  private lateinit var inflater: LayoutInflater
  private lateinit var adapter: ArrayAdapter<Bookmark>
  private lateinit var listener: ReaderTOCSelectionListenerType
  private lateinit var bookmarksTOCLayout: View
  private lateinit var bookmarksTOCListView: ListView
  private lateinit var parameters: ReaderTOCParameters

  companion object {

    private const val parametersKey = "org.nypl.simplified.app.reader.toc.parameters"

    fun newInstance(parameters: ReaderTOCParameters): ReaderTOCBookmarksFragment {
      val args = Bundle()
      args.putSerializable(parametersKey, parameters)
      val fragment = ReaderTOCBookmarksFragment()
      fragment.arguments = args
      return fragment
    }
  }

  override fun onCreate(state: Bundle?) {
    this.logger.debug("onCreate")
    super.onCreate(state)
    this.retainInstance = true
    this.parameters = this.arguments!!.getSerializable(parametersKey) as ReaderTOCParameters
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    this.inflater = inflater
    this.bookmarksTOCLayout =
      inflater.inflate(R.layout.reader_toc_bookmarks, null)
    this.bookmarksTOCListView =
      this.bookmarksTOCLayout.findViewById(R.id.bookmarks_list) as ListView

    this.adapter =
      ArrayAdapter(
        requireContext(),
        0,
        this.parameters.bookmarks.bookmarks.sortedBy { bookmark ->
          bookmark.bookProgress
        }.reversed()
      )

    this.bookmarksTOCListView.adapter = this
    return this.bookmarksTOCLayout
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    if (context is ReaderTOCSelectionListenerType) {
      this.listener = context
    } else {
      throw IllegalStateException(
        context.toString() +
          " must implement ReaderTOCSelectionListenerType "
      )
    }
  }

  /**
   * List View Adapter
   */

  override fun areAllItemsEnabled(): Boolean {
    return this.adapter.areAllItemsEnabled()
  }

  override fun getCount(): Int {
    return this.adapter.count
  }

  override fun getItem(position: Int): Any {
    return this.adapter.getItem(position)
  }

  override fun getItemId(position: Int): Long {
    return this.adapter.getItemId(position)
  }

  override fun getItemViewType(position: Int): Int {
    return this.adapter.getItemViewType(position)
  }

  override fun getView(position: Int, reuse: View?, parent: ViewGroup?): View {
    val itemView = if (reuse != null) {
      reuse as ViewGroup
    } else {
      this.inflater.inflate(R.layout.reader_toc_element, parent, false) as ViewGroup
    }

    val layoutView =
      itemView.findViewById<ViewGroup>(R.id.toc_bookmark_element)
    val textView =
      layoutView.findViewById<TextView>(R.id.toc_bookmark_element_title)
    val detailTextView =
      layoutView.findViewById<TextView>(R.id.toc_bookmark_element_subtitle)

    val bookmark = this.adapter.getItem(position)!!
    textView.text = bookmark.chapterTitle
    detailTextView.text = this.detailTextFrom(bookmark)

    textView.setTextColor(
      ReaderColorSchemes.foregroundAsAndroidColor(
        Services.serviceDirectory()
          .requireService(ProfilesControllerType::class.java)
          .profileCurrent()
          .preferences()
          .readerPreferences
          .colorScheme()
      )
    )

    layoutView.setOnClickListener {
      this.listener.onTOCItemSelected(ReaderSelectedBookmark(bookmark))
    }
    return layoutView
  }

  override fun getViewTypeCount(): Int {
    return this.adapter.viewTypeCount
  }

  override fun hasStableIds(): Boolean {
    return this.adapter.hasStableIds()
  }

  override fun isEmpty(): Boolean {
    return this.adapter.isEmpty
  }

  override fun isEnabled(position: Int): Boolean {
    return this.adapter.isEnabled(position)
  }

  override fun registerDataSetObserver(observer: DataSetObserver?) {
    this.adapter.registerDataSetObserver(observer)
  }

  override fun unregisterDataSetObserver(observer: DataSetObserver?) {
    this.adapter.unregisterDataSetObserver(observer)
  }

  private fun detailTextFrom(bookmark: Bookmark): String {
    val shortDateText =
      run {
        val formatter = DateTimeFormat.forPattern("dd/MM/yy")
        formatter.print(bookmark.time) + " - "
      }

    val chapterProgressText =
      run {
        val percent = (bookmark.chapterProgress * 100).toInt()
        "$percent% through chapter"
      }

    return shortDateText + chapterProgressText
  }
}
