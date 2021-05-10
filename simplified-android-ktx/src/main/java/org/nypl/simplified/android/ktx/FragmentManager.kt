package org.nypl.simplified.android.ktx

import androidx.fragment.app.FragmentManager

fun FragmentManager.tryPopToRoot(): Boolean {
  if (backStackEntryCount == 0) {
    return false
  }
  this.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
  return true
}

fun FragmentManager.tryPopBackStack(): Boolean {
  if (backStackEntryCount == 0) {
    return false
  }
  this.popBackStack()
  return true
}
