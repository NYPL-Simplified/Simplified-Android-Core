package org.nypl.simplified.cardcreator

import android.content.Context
import androidx.fragment.app.Fragment

interface CardCreatorServiceType {
  fun openCardCreatorActivity(
    fragment: Fragment,
    context: Context?,
    resultCode: Int,
    isLoggedIn: Boolean,
    userIdentifier: String
  )
}
