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
import org.nypl.simplified.app.utilities.UIThread

/**
 * A reusable fragment for a table of contents view
 */
class ReaderTOCContentsFragment : Fragment(), ListAdapter, ReaderSettingsListenerType {

  private var readerTOCLayout: View? = null
  private var readerTOCListView: ListView? = null

  private var inflater: LayoutInflater? = null

  private var adapter: ArrayAdapter<ReaderTOC.TOCElement>? = null
  private var listener: ReaderTOCContentsFragmentSelectionListenerType? = null

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {

    this.inflater = inflater
    readerTOCLayout = inflater.inflate(R.layout.reader_toc, null)
    readerTOCListView = readerTOCLayout?.findViewById(R.id.reader_toc_list)

    val act = activity as? ReaderTOCActivity
    val elements = act?.in_toc?.elements as? List<ReaderTOC.TOCElement>
    adapter = ArrayAdapter(context, 0, elements)
    readerTOCListView?.adapter = this

    val rs = Simplified.getReaderAppServices()
    val settings = rs.settings
    settings.addListener(this)

    applyColorScheme(settings.colorScheme)

    return readerTOCLayout
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    if (context is ReaderTOCContentsFragmentSelectionListenerType) {
      listener = context
    } else {
      throw RuntimeException(context.toString() +
          " must implement ReaderTOCContentsFragmentSelectionListenerType")
    }
  }

  override fun onDetach() {
    super.onDetach()
    listener = null

    val rs = Simplified.getReaderAppServices()
    val settings = rs.settings
    settings?.removeListener(this)
  }

  private fun applyColorScheme(cs: ReaderColorScheme) {
    UIThread.checkIsUIThread()
    readerTOCListView?.rootView?.setBackgroundColor(cs.backgroundColor)
  }

  /**
   * ReaderSettingsListenerType
   */

  override fun onReaderSettingsChanged(s: ReaderSettingsType?) {
    UIThread.runOnUIThread {
      if (s != null) {
        applyColorScheme(s.colorScheme)
      }
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
    var itemView: ViewGroup
    if (reuse != null) {
      itemView = reuse as ViewGroup
    } else {
      itemView = inflater?.inflate(R.layout.reader_toc_element, parent, false) as ViewGroup
    }

    /**
     * Populate the text view and set the left margin based on the desired
     * indentation level.
     */

    val textView = itemView.findViewById<TextView>(R.id.reader_toc_element_text)
    val element = adapter?.getItem(position)
    textView.text = element?.title ?: "TOC Marker"

    val rs = Simplified.getReaderAppServices()
    val settings = rs.settings

    val p = RelativeLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
    )

    val leftIndent = if (element != null) { rs.screenDPToPixels(element.indent * 16) } else { 0.0 }
    p.setMargins(leftIndent.toInt(), 0, 0, 0)
    textView.layoutParams = p
    textView.setTextColor(settings.colorScheme.foregroundColor)

    itemView.setOnClickListener { _ ->
      this.listener?.onTOCItemSelected(element)
    }

    return itemView
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
}
