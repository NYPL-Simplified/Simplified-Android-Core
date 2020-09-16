package org.nypl.simplified.opds.core

import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some

/** Returns `T` if [OptionType] is [Some]; else `null`. */

fun <T> OptionType<T>.getOrNull(): T? = when (this) {
  is Some<T> -> this.get()
  else -> null
}
