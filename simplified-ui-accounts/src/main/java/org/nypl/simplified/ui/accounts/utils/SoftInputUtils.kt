package org.nypl.simplified.ui.accounts.utils

import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment

/**
 * Hides keyboard in Fragment
 */
fun Fragment.hideSoftInput() {
  val view = this.activity?.currentFocus
  if (view != null) {
    val imm = this.context?.getSystemService<InputMethodManager>()
    imm?.hideSoftInputFromWindow(view.windowToken, 0)
  }
}
