package org.nypl.simplified.app.reader

import android.content.Context
import android.database.DataSetObserver
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.TextView
import org.nypl.simplified.app.R
import org.nypl.simplified.app.Simplified
import org.nypl.simplified.app.utilities.UIThread
import org.nypl.simplified.books.profiles.ProfileEvent
import org.nypl.simplified.books.profiles.ProfilePreferencesChanged
import org.nypl.simplified.books.reader.ReaderColorScheme
import org.nypl.simplified.observable.ObservableSubscriptionType

/**
 * A reusable fragment for a table of contents view
 */
class ReaderTOCContentsFragment : Fragment(), ListAdapter {

  private var readerTOCLayout: View? = null
  private var readerTOCListView: ListView? = null

  private var inflater: LayoutInflater? = null

  private var adapter: ArrayAdapter<ReaderTOC.TOCElement>? = null
  private var listener: ReaderTOCFragmentSelectionListenerType? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?): View? {

    this.inflater = inflater
    this.readerTOCLayout = inflater.inflate(R.layout.reader_toc, null)
    this.readerTOCListView = this.readerTOCLayout?.findViewById(R.id.reader_toc_list)

    val reader_activity = this.activity as? ReaderTOCActivity
    val elements = reader_activity?.in_toc?.elements as? List<ReaderTOC.TOCElement>
    this.adapter = ArrayAdapter(this.context, 0, elements)
    this.readerTOCListView?.adapter = this

    this.applyColorScheme(
      Simplified.getProfilesController()
        .profileCurrent()
        .preferences()
        .readerPreferences()
        .colorScheme())

    return this.readerTOCLayout
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    if (context is ReaderTOCFragmentSelectionListenerType) {
      this.listener = context

      this.profileSubscription =
        Simplified.getProfilesController()
          .profileEvents()
          .subscribe { event -> this.onProfileEvent(event) }
    } else {
      throw RuntimeException(context.toString() +
        " must implement ReaderTOCFragmentSelectionListenerType ")
    }
  }

  private var profileSubscription: ObservableSubscriptionType<ProfileEvent>? = null

  override fun onDetach() {
    super.onDetach()
    this.listener = null

    this.profileSubscription?.unsubscribe()
    this.profileSubscription = null
  }

  private fun onProfileEvent(event: ProfileEvent) {
    if (event is ProfilePreferencesChanged) {
      if (event.changedReaderPreferences()) {
        val colorScheme =
          Simplified.getProfilesController()
            .profileCurrent()
            .preferences()
            .readerPreferences()
            .colorScheme()
        UIThread.runOnUIThread { this.applyColorScheme(colorScheme) }
      }
    }
  }

  private fun applyColorScheme(cs: ReaderColorScheme) {
    UIThread.checkIsUIThread()
    this.readerTOCListView?.rootView?.setBackgroundColor(ReaderColorSchemes.background(cs))
  }

  /**
   * List View Adapter
   */

  override fun areAllItemsEnabled(): Boolean {
    return this.adapter!!.areAllItemsEnabled()
  }

  override fun getCount(): Int {
    return this.adapter!!.count
  }

  override fun getItem(position: Int): Any {
    return this.adapter!!.getItem(position)
  }

  override fun getItemId(position: Int): Long {
    return this.adapter!!.getItemId(position)
  }

  override fun getItemViewType(position: Int): Int {
    return this.adapter!!.getItemViewType(position)
  }

  override fun getView(position: Int, reuse: View?, parent: ViewGroup?): View {

    val itemView = if (reuse != null) {
      reuse as ViewGroup
    } else {
      this.inflater?.inflate(R.layout.reader_toc_element, parent, false) as ViewGroup
    }

    val textView = itemView.findViewById<TextView>(R.id.reader_toc_element_text)
    val bookmarkLayout = itemView.findViewById<ViewGroup>(R.id.toc_bookmark_element)
    bookmarkLayout.visibility = View.GONE
    val element = this.adapter?.getItem(position)
    textView.text = element?.title ?: "TOC Marker"

    val p = RelativeLayout.LayoutParams(
      android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
      android.view.ViewGroup.LayoutParams.WRAP_CONTENT
    )

    // Set the left margin based on the desired indentation level.
    val leftIndent = if (element != null) {
      Simplified.getScreenSizeInformation().screenDPToPixels(element.indent * 16)
    } else {
      0.0
    }

    p.setMargins(leftIndent.toInt(), 0, 0, 0)
    textView.layoutParams = p
    textView.setTextColor(
      ReaderColorSchemes.foreground(Simplified.getProfilesController()
        .profileCurrent()
        .preferences()
        .readerPreferences()
        .colorScheme()))

    itemView.setOnClickListener { this.listener?.onTOCItemSelected(element) }
    return itemView
  }

  override fun getViewTypeCount(): Int {
    return this.adapter!!.viewTypeCount
  }

  override fun hasStableIds(): Boolean {
    return this.adapter!!.hasStableIds()
  }

  override fun isEmpty(): Boolean {
    return this.adapter!!.isEmpty
  }

  override fun isEnabled(position: Int): Boolean {
    return this.adapter!!.isEnabled(position)
  }

  override fun registerDataSetObserver(observer: DataSetObserver?) {
    this.adapter!!.registerDataSetObserver(observer)
  }

  override fun unregisterDataSetObserver(observer: DataSetObserver?) {
    this.adapter!!.unregisterDataSetObserver(observer)
  }
}
