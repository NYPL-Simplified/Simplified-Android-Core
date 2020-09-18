package org.nypl.simplified.ui.images

import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.squareup.picasso.RequestCreator
import org.nypl.simplified.accounts.api.AccountProviderDescription
import org.slf4j.LoggerFactory
import java.net.URI

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
    account: AccountProviderDescription,
    @DrawableRes defaultIcon: Int,
    iconView: ImageView
  ) {
    val request: RequestCreator
    val logoURI: URI? = account.logoURI?.hrefURI
    if (logoURI != null) {
      LOG.debug("configuring account logo: {}", logoURI)
      request = loader.load(logoURI.toString())
    } else {
      request = loader.load(defaultIcon)
    }

    request.into(
      iconView,
      object : Callback {
        override fun onSuccess() {
          iconView.visibility = View.VISIBLE
        }

        override fun onError(e: Exception) {
          LOG.error("failed to load account icon: ", e)
          iconView.setImageResource(defaultIcon)
          iconView.visibility = View.VISIBLE
        }
      }
    )
  }
}
