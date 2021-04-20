package org.nypl.simplified.ui.catalog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import org.slf4j.LoggerFactory
import java.util.Calendar

/**
 * The AgeGateDialog is the solution to Google's policy violation of the SimplyE age gate not being
 * neutral for apps that target both children and older audiences.
 *
 * This DialogFragment should always be launched with `setCancelable(false)`
 *
 * See: https://jira.nypl.org/browse/SIMPLY-3493
 */
class AgeGateDialog : DialogFragment(), AdapterView.OnItemSelectedListener {

  private val logger = LoggerFactory.getLogger(AgeGateDialog::class.java)
  private lateinit var birthYearSpinner: Spinner
  private val currentYear = Calendar.getInstance().get(Calendar.YEAR)
  private lateinit var birthYearSelectedListener: BirthYearSelectedListener
  private val century = 100
  private val positiveButton: Button?
    get() = (dialog as? AlertDialog)?.getButton(AlertDialog.BUTTON_POSITIVE)

  interface BirthYearSelectedListener {
    fun onBirthYearSelected(isOver13: Boolean)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    try {
      birthYearSelectedListener = requireParentFragment() as BirthYearSelectedListener
    } catch (exception: ClassCastException) {
      throw ClassCastException("${context?.javaClass?.simpleName} must implement BirthYearSelectedListener")
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    isCancelable = false
    return super.onCreateView(inflater, container, savedInstanceState)
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val dialog = AlertDialog.Builder(requireContext())
      .setTitle(getString(R.string.ageVerification))
      .setPositiveButton(R.string.done) { _, _ ->
        val selection = birthYearSpinner.selectedItem.toString()
        if (selection != getString(R.string.ageVerification)) {
          val birthYear = selection.toInt()
          birthYearSelectedListener.onBirthYearSelected(currentYear - birthYear > 13)
        }
      }
    val view = layoutInflater.inflate(R.layout.age_gate_dialog, null)
    val years = ArrayList<String>()
    val maxYear = currentYear - century
    years.add(getString(R.string.selectYear))
    for (i in maxYear..currentYear) {
      years.add(i.toString())
    }
    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, years)
    birthYearSpinner = view.findViewById(R.id.birthYearSpinner)
    birthYearSpinner.adapter = adapter
    birthYearSpinner.onItemSelectedListener = this
    dialog.setView(view)
    return dialog.create()
  }

  override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
    logger.debug("Year selected: ${birthYearSpinner.getItemAtPosition(position)}")
    positiveButton?.isEnabled = position > 0
  }

  override fun onNothingSelected(parent: AdapterView<*>?) {
    positiveButton?.isEnabled = false
  }

  companion object {
    fun create(): AgeGateDialog {
      return AgeGateDialog()
    }
  }
}
