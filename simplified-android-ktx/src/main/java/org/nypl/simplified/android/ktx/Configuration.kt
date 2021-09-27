package org.nypl.simplified.android.ktx

import android.content.res.Configuration

val Configuration.isNightModeYes: Boolean
  get() = this.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
