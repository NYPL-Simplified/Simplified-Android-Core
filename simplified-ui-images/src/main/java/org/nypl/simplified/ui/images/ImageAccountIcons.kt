package org.nypl.simplified.ui.images

import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.squareup.picasso.Picasso
import org.nypl.simplified.accounts.api.AccountProviderDescription
import org.slf4j.LoggerFactory

/**
 * Functions to efficiently load account logos.
 */

object ImageAccountIcons {

  private val logger =
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
    iconView: ImageView,
  ) {
    val uri = account.logoURI?.hrefURI
    this.logger.debug("configuring account logo: {}", uri)

    if (uri == null) {
      iconView.setImageResource(defaultIcon)
      return
    }

    loader
      .load(uri.toString())
      .placeholder(defaultIcon)
      .error(defaultIcon)
      .into(iconView)
  }
}
