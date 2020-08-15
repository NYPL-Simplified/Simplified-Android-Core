package org.nypl.simplified.android.ktx

import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

/** Returns the support action bar from the parent [AppCompatActivity]. */

val Fragment.supportActionBar: ActionBar?
  get() = (activity as? AppCompatActivity)?.supportActionBar
