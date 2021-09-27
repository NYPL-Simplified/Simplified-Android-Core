package org.nypl.simplified.ui.errorpage

import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import org.nypl.simplified.android.ktx.supportActionBar
import org.nypl.simplified.reports.Reports
import org.slf4j.LoggerFactory

/**
 * A full-screen fragment for displaying presentable errors, and reporting those errors
 * to technical support.
 */

class ErrorPageFragment : Fragment(R.layout.error_page) {

  private val logger = LoggerFactory.getLogger(ErrorPageFragment::class.java)

  companion object {

    private const val PARAMETERS_ID =
      "org.nypl.simplified.ui.errorpage.ErrorPageFragment.parameters"

    /**
     * Create a new error page fragment.
     */

    fun create(
      parameters: ErrorPageParameters
    ): ErrorPageFragment {
      val args = Bundle()
      args.putSerializable(this.PARAMETERS_ID, parameters)

      val fragment = ErrorPageFragment()
      fragment.arguments = args
      return fragment
    }
  }

  private lateinit var errorDetails: TextView
  private lateinit var errorStepsList: RecyclerView
  private lateinit var parameters: ErrorPageParameters
  private lateinit var sendButton: Button

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    this.errorDetails =
      view.findViewById(R.id.errorDetails)
    this.errorStepsList =
      view.findViewById(R.id.errorSteps)
    this.sendButton =
      view.findViewById(R.id.errorSendButton)

    this.parameters =
      this.arguments!!.getSerializable(PARAMETERS_ID)
      as ErrorPageParameters

    if (parameters.attributes.isEmpty()) {
      this.errorDetails.visibility = View.GONE
    } else {
      this.errorDetails.text = ""

      val ssb = SpannableStringBuilder()
      parameters.attributes.entries.forEachIndexed { index, (key, value) ->
        if (index > 0) { ssb.append("\n\n") }
        ssb.append(key)

        val styleSpan = StyleSpan(Typeface.BOLD)
        val spanStart = ssb.length - key.length
        val spanEnd = ssb.length
        ssb.setSpan(styleSpan, spanStart, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        ssb.append("\n")
        ssb.append(value)
      }
      this.errorDetails.text = ssb
    }

    this.errorStepsList.setHasFixedSize(false)
    this.errorStepsList.layoutManager =
      LinearLayoutManager(context)
    this.errorStepsList.adapter =
      ErrorPageStepsListAdapter(parameters.taskSteps)
    (this.errorStepsList.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
  }

  override fun onStart() {
    super.onStart()
    this.configureToolbar()

    this.sendButton.isEnabled = true
    this.sendButton.setOnClickListener {
      this.sendButton.isEnabled = false

      Reports.sendReportsDefault(
        context = requireContext(),
        address = parameters.emailAddress,
        subject = parameters.subject,
        body = parameters.report
      )
    }
  }

  private fun configureToolbar() {
    this.supportActionBar?.apply {
      title = getString(R.string.errorDetailsTitle)
      subtitle = null
    }
  }
}
