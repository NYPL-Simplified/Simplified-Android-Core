package org.nypl.simplified.app.images

import android.view.View
import android.widget.ImageView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.squareup.picasso.RequestCreator
import org.nypl.simplified.accounts.api.AccountProviderDescriptionType
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.app.R
import org.slf4j.LoggerFactory

/**
 * Functions to efficiently load account logos.
 */

object ImageAccountIcons {

  private val LOG =
    LoggerFactory.getLogger(ImageAccountIcons::class.java)

  /**
   * Load the logo of the given account into the given image view,
   * or load a default image if the account does not have a logo.
   */

  @JvmStatic
  fun loadAccountLogoIntoView(
    loader: Picasso,
    account: AccountProviderDescriptionType,
    iconView: ImageView) {

    val request: RequestCreator
    val logoURI = account.logoURI
    if (logoURI != null) {
      LOG.debug("configuring account logo: {}", logoURI)
      request = loader.load(logoURI.toString())
    } else {
      request = loader.load(R.drawable.librarylogomagic)
    }

    request.into(iconView, object : Callback {
      override fun onSuccess() {
        iconView.visibility = View.VISIBLE
      }

      override fun onError() {
        iconView.visibility = View.INVISIBLE
      }
    })
  }
}
