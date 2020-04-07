package org.nypl.simplified.cardcreator

import android.app.Activity
import android.content.Context

interface CardCreatorServiceType {
  fun openCardCreatorActivity(activity: Activity?, context: Context?, resultCode: Int)
}
