package org.nypl.simplified.cardcreator.utils

import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment

/**
 * Utility to hide keyboard
 */

fun Fragment.hideKeyboard() {
  val view = this.activity?.currentFocus
  if (view != null) {
    val imm = this.context?.getSystemService<InputMethodManager>()
    imm?.hideSoftInputFromWindow(view.windowToken, 0)
  }
}
