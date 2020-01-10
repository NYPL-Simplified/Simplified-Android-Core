package org.nypl.simplified.ui.errorpage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TableLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import org.nypl.simplified.presentableerror.api.PresentableErrorType
import org.nypl.simplified.ui.toolbar.ToolbarHostType
import org.slf4j.LoggerFactory

/**
 * A full-screen fragment for displaying presentable errors, and reporting those errors
 * to technical support.
 */

class ErrorPageFragment : Fragment() {

  private val logger = LoggerFactory.getLogger(ErrorPageFragment::class.java)

  companion object {

    private const val PARAMETERS_ID =
      "org.nypl.simplified.ui.errorpage.ErrorPageFragment.parameters"

    /**
     * Create a new error page fragment.
     */

    fun <E : PresentableErrorType> create(
      parameters: ErrorPageParameters<E>
    ): ErrorPageFragment {
      val args = Bundle()
      args.putSerializable(this.PARAMETERS_ID, parameters)

      val fragment = ErrorPageFragment()
      fragment.arguments = args
      return fragment
    }
  }

  private lateinit var errorAttributesTable: TableLayout
  private lateinit var errorAttributesTitle: TextView
  private lateinit var errorStepsList: RecyclerView
  private lateinit var errorTitle: TextView
  private lateinit var listener: ErrorPageListenerType
  private lateinit var parameters: ErrorPageParameters<PresentableErrorType>
  private lateinit var sendButton: Button

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.logger.debug("onCreate")

    run {
      val className = ErrorPageListenerType::class.java.canonicalName
      val activity = this.requireActivity()
      if (activity is ErrorPageListenerType) {
        this.listener = activity
      } else {
        throw IllegalStateException(
          "The activity hosting this fragment must implement $className")
      }
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {

    val viewRoot =
      inflater.inflate(R.layout.error_page, container, false)

    this.errorTitle =
      viewRoot.findViewById(R.id.errorTitle)
    this.errorAttributesTitle =
      viewRoot.findViewById(R.id.errorDetailsTitle)
    this.errorAttributesTable =
      viewRoot.findViewById(R.id.errorDetailsTable)
    this.errorStepsList =
      viewRoot.findViewById(R.id.errorSteps)
    this.sendButton =
      viewRoot.findViewById(R.id.errorSendButton)

    this.parameters =
      this.arguments!!.getSerializable(PARAMETERS_ID)
        as ErrorPageParameters<PresentableErrorType>

    if (parameters.attributes.isEmpty()) {
      this.errorAttributesTable.visibility = View.GONE
      this.errorAttributesTitle.visibility = View.GONE
    } else {
      this.errorAttributesTable.removeAllViews()

      for (key in parameters.attributes.keys) {
        val value =
          parameters.attributes[key]
        val tableRow =
          inflater.inflate(R.layout.error_attribute_row, this.errorAttributesTable, false)
        val tableCell0 =
          tableRow.findViewById<TextView>(R.id.errorAttributeKey)
        val tableCell1 =
          tableRow.findViewById<TextView>(R.id.errorAttributeValue)

        tableCell0.text = key
        tableCell1.text = value
        this.errorAttributesTable.addView(tableRow)
      }
    }

    this.errorStepsList.setHasFixedSize(false)
    this.errorStepsList.layoutManager =
      LinearLayoutManager(context)
    this.errorStepsList.adapter =
      ErrorPageStepsListAdapter(parameters.taskSteps)
    (this.errorStepsList.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

    return viewRoot
  }

  override fun onStart() {
    super.onStart()

    run {
      val className = ToolbarHostType::class.java.canonicalName
      val activity = this.requireActivity()
      if (activity is ToolbarHostType) {
        val toolbar = activity.findToolbar()
        toolbar.menu.clear()
        toolbar.setTitle(R.string.errorDetailsTitle)
        toolbar.subtitle = ""
      } else {
        throw IllegalStateException(
          "The activity hosting this fragment must implement $className")
      }
    }

    this.sendButton.isEnabled = true
    this.sendButton.setOnClickListener {
      this.sendButton.isEnabled = false
      this.listener.onErrorPageSendReport(this.parameters)
    }
  }

  override fun onStop() {
    super.onStop()

    this.sendButton.setOnClickListener(null)
  }
}
