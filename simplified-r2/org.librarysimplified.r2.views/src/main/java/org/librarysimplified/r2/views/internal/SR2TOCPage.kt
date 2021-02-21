package org.librarysimplified.r2.views.internal

import androidx.fragment.app.Fragment

internal data class SR2TOCPage(
  val title: String,
  val fragmentConstructor: () -> Fragment
)
