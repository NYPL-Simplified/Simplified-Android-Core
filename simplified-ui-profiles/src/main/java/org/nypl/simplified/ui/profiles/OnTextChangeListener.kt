package org.nypl.simplified.ui.profiles

import android.text.Editable
import android.text.TextWatcher

/**
 * A text watcher that calls a given function when text changes.
 */

internal class OnTextChangeListener(
  private val onChanged: (sequence: CharSequence,
                          start: Int,
                          before: Int,
                          count: Int) -> Unit
) : TextWatcher {
  override fun afterTextChanged(sequence: Editable) {

  }

  override fun beforeTextChanged(
    sequence: CharSequence,
    start: Int,
    count: Int,
    after: Int
  ) {

  }

  override fun onTextChanged(
    sequence: CharSequence,
    start: Int,
    before: Int,
    count: Int
  ) {
    this.onChanged.invoke(sequence, start, before, count)
  }
}