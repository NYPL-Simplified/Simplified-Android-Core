package org.nypl.simplified.cardcreator.utils

import android.widget.EditText
import androidx.core.widget.doOnTextChanged
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

@ExperimentalCoroutinesApi
fun EditText.textChanges() = callbackFlow {
  val listener = doOnTextChanged { text, _, _, _ -> trySend(text) }
  awaitClose { removeTextChangedListener(listener) }
}
