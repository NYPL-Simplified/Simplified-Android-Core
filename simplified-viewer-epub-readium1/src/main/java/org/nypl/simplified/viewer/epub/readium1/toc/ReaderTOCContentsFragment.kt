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
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import io.reactivex.disposables.Disposable
import org.librarysimplified.services.api.Services
import org.nypl.simplified.app.reader.ReaderColorSchemes
import org.nypl.simplified.viewer.epub.readium1.toc.ReaderTOCSelection.ReaderSelectedTOCElement
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileUpdated
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.reader.api.ReaderColorScheme
import org.nypl.simplified.ui.screen.ScreenSizeInformationType
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.nypl.simplified.viewer.epub.readium1.R
import org.slf4j.LoggerFactory

/**
 * A reusable fragment for a table of contents view
 */

class ReaderTOCContentsFragment : Fragment(), ListAdapter {

  private val logger = LoggerFactory.getLogger(ReaderTOCBookmarksFragment::class.java)

  private lateinit var adapter: ArrayAdapter<ReaderTOCElement>
  private lateinit var listener: ReaderTOCSelectionListenerType
  private lateinit var parameters: ReaderTOCParameters
  private lateinit var profilesController: ProfilesControllerType
  private lateinit var readerTOCLayout: View
  private lateinit var readerTOCListView: ListView
  private lateinit var uiThread: UIThreadServiceType
  private var profileSubscription: Disposable? = null

  companion object {

    private const val parametersKey = "org.nypl.simplified.app.reader.toc.parameters"

    fun newInstance(parameters: ReaderTOCParameters): ReaderTOCContentsFragment {
      val args = Bundle()
      args.putSerializable(parametersKey, parameters)
      val fragment = ReaderTOCContentsFragment()
      fragment.arguments = args
      return fragment
    }
  }

  override fun onCreate(state: Bundle?) {
    this.logger.debug("onCreate")
    super.onCreate(state)
    this.retainInstance = true
    this.parameters = this.arguments!!.getSerializable(parametersKey) as ReaderTOCParameters

    val services = Services.serviceDirectory()

    this.profilesController =
      services.requireService(ProfilesControllerType::class.java)
    this.uiThread =
      services.requireService(UIThreadServiceType::class.java)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    this.readerTOCLayout =
      inflater.inflate(R.layout.reader_toc, null)
    this.readerTOCListView =
      this.readerTOCLayout.findViewById(R.id.reader_toc_list)

    this.adapter = ArrayAdapter(requireContext(), 0, this.parameters.tocElements)
    this.readerTOCListView.adapter = this

    this.applyColorScheme(
      Services.serviceDirectory()
        .requireService(ProfilesControllerType::class.java)
        .profileCurrent()
        .preferences()
        .readerPreferences
        .colorScheme()
    )

    return this.readerTOCLayout
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    if (context is ReaderTOCSelectionListenerType) {
      this.listener = context

      this.profileSubscription =
        Services.serviceDirectory()
          .requireService(ProfilesControllerType::class.java)
          .profileEvents()
          .subscribe { event -> this.onProfileEvent(event) }
    } else {
      throw RuntimeException(
        context.toString() +
          " must implement ReaderTOCSelectionListenerType "
      )
    }
  }

  override fun onDetach() {
    super.onDetach()
    this.profileSubscription?.dispose()
    this.profileSubscription = null
  }

  private fun onProfileEvent(event: ProfileEvent) {
    if (event is ProfileUpdated.Succeeded) {
      if (event.oldDescription.preferences != event.newDescription.preferences) {
        this.uiThread.runOnUIThread {
          this.applyColorScheme(event.newDescription.preferences.readerPreferences.colorScheme())
        }
      }
    }
  }

  private fun applyColorScheme(cs: ReaderColorScheme) {
    this.uiThread.checkIsUIThread()
    this.readerTOCListView.rootView?.setBackgroundColor(ReaderColorSchemes.backgroundAsAndroidColor(cs))
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
      this.layoutInflater.inflate(
        R.layout.reader_toc_element,
        parent,
        false
      ) as ViewGroup
    }

    val textView =
      itemView.findViewById<TextView>(R.id.reader_toc_element_text)
    val bookmarkLayout =
      itemView.findViewById<ViewGroup>(R.id.toc_bookmark_element)

    bookmarkLayout.visibility = View.GONE
    val element = this.adapter.getItem(position)!!
    textView.text = element.title

    val layoutParams =
      RelativeLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )

    // Set the left margin based on the desired indentation level.
    val leftIndent = Services.serviceDirectory()
      .requireService(ScreenSizeInformationType::class.java)
      .dpToPixels(element.indent * 16)

    layoutParams.setMargins(leftIndent.toInt(), 0, 0, 0)
    textView.layoutParams = layoutParams
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

    itemView.setOnClickListener {
      this.listener.onTOCItemSelected(ReaderSelectedTOCElement(element))
    }
    return itemView
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
}
