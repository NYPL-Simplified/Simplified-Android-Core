package org.nypl.simplified.cardcreator.utils

import androidx.fragment.app.Fragment
import java.lang.Double

/**
 * Gets Cache object
 */

fun Fragment.getCache(): Cache {
  return Cache(this.requireActivity())
}

/**
 * Determines whether or not the user identifier is a barcode or username
 */
fun Fragment.isBarcode(identifier: String): Boolean {
  return identifier.toDoubleOrNull() != null
}
