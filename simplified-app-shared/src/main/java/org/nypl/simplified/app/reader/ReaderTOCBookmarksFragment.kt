package org.nypl.simplified.app.reader

import android.content.Context
import android.database.DataSetObserver
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*

import org.nypl.simplified.app.R
import org.nypl.simplified.app.Simplified
import org.nypl.simplified.books.core.BookmarkAnnotation
import org.nypl.simplified.rfc3339.core.RFC3339Formatter
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

/**
 * A reusable fragment for a ListView of bookmarks
 */

class ReaderTOCBookmarksFragment : Fragment(), ListAdapter {

  private var inflater: LayoutInflater? = null
  private var adapter: ArrayAdapter<BookmarkAnnotation>? = null
  private var listener: ReaderTOCFragmentSelectionListenerType ? = null

  private var bookmarksTOCLayout: View? = null
  private var bookmarksTOCListView: ListView? = null

  private companion object {
    val LOG = LoggerFactory.getLogger(ReaderTOCBookmarksFragment::class.java)!!
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {

    this.inflater = inflater
    bookmarksTOCLayout = inflater.inflate(R.layout.reader_toc_bookmarks, null)
    bookmarksTOCListView = bookmarksTOCLayout?.findViewById(R.id.bookmarks_list) as? ListView

    val activity = activity as? ReaderTOCActivity
    val bookmarks = activity?.bookmarks ?: ArrayList<BookmarkAnnotation>(0)

    val sortedMarks = bookmarks.sortedWith(compareBy({ it.body.bookProgress }))

    adapter = ArrayAdapter(context, 0, sortedMarks)
    bookmarksTOCListView?.adapter = this

    return bookmarksTOCLayout
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    if (context is ReaderTOCFragmentSelectionListenerType ) {
      listener = context
    } else {
      throw RuntimeException(context.toString() +
          " must implement ReaderTOCFragmentSelectionListenerType ")
    }
  }

  /**
   * List View Adapter
   */

  override fun areAllItemsEnabled(): Boolean {
    return adapter!!.areAllItemsEnabled()
  }

  override fun getCount(): Int {
    return adapter!!.count
  }

  override fun getItem(position: Int): Any {
    return adapter!!.getItem(position)
  }

  override fun getItemId(position: Int): Long {
    return adapter!!.getItemId(position)
  }

  override fun getItemViewType(position: Int): Int {
    return adapter!!.getItemViewType(position)
  }

  override fun getView(position: Int, reuse: View?, parent: ViewGroup?): View {

    val itemView = if (reuse != null) {
      reuse as ViewGroup
    } else {
      inflater?.inflate(R.layout.reader_toc_element, parent, false) as ViewGroup
    }

    val layoutView = itemView.findViewById<ViewGroup>(R.id.toc_bookmark_element)
    val textView = layoutView.findViewById<TextView>(R.id.toc_bookmark_element_title)
    val detailTextView = layoutView.findViewById<TextView>(R.id.toc_bookmark_element_subtitle)
    val bookmark = adapter?.getItem(position)

    textView.text = bookmark?.body?.chapterTitle ?: "Bookmark"
    detailTextView.text = detailTextFrom(bookmark)

    val rs = Simplified.getReaderAppServices()
    val settings = rs.settings

    textView.setTextColor(settings.colorScheme.foregroundColor)

    layoutView.setOnClickListener { _ ->
      this.listener?.onBookmarkSelected(bookmark)
    }

    return layoutView
  }

  override fun getViewTypeCount(): Int {
    return adapter!!.viewTypeCount
  }

  override fun hasStableIds(): Boolean {
    return adapter!!.hasStableIds()
  }

  override fun isEmpty(): Boolean {
    return adapter!!.isEmpty
  }

  override fun isEnabled(position: Int): Boolean {
    return adapter!!.isEnabled(position)
  }

  override fun registerDataSetObserver(observer: DataSetObserver?) {
    adapter!!.registerDataSetObserver(observer)
  }

  override fun unregisterDataSetObserver(observer: DataSetObserver?) {
    adapter!!.unregisterDataSetObserver(observer)
  }

  /**
   * Private Instance Methods
   */

  private fun detailTextFrom(bookmark: BookmarkAnnotation?): String {
    val shortDate = if (bookmark?.body?.timestamp != null) {
      try {
        val date = RFC3339Formatter.parseRFC3339Date(bookmark.body.timestamp)
        val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.US)
        val dateFormatString = dateFormat.format(date.time)
        "$dateFormatString - "
      } catch (e: Exception) {
        LOG.error("Error parsing date/time in bookmark. Falling back to raw timestamp.")
        bookmark.body.timestamp + " - "
      }
    } else {
      ""
    }

    val chapterProgress = if (bookmark?.body?.chapterProgress != null) {
      val progressFloat = bookmark.body.chapterProgress!!
      val percent = (progressFloat * 100).toInt()
      "$percent% through chapter"
    } else {
      "Chapter Location Marked"
    }

    return shortDate + chapterProgress
  }
}

