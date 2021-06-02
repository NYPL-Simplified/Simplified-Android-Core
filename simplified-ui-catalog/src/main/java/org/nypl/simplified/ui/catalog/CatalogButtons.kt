package org.nypl.simplified.ui.catalog

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import org.nypl.simplified.ui.screen.ScreenSizeInformationType

/**
 * Functions to create buttons for catalog views.
 */

class CatalogButtons(
  private val context: Context,
  private val screenSizeInformation: ScreenSizeInformationType
) {

  @UiThread
  fun createButtonSpace(): Space {
    val space = Space(this.context)
    space.layoutParams = this.buttonSpaceLayoutParameters()
    return space
  }

  @UiThread
  fun createCenteredTextForButtons(
    @StringRes res: Int
  ): TextView {
    val text = AppCompatTextView(this.context)
    text.gravity = Gravity.CENTER
    text.text = this.context.getString(res)
    return text
  }

  @UiThread
  fun createButton(
    context: Context,
    text: Int,
    description: Int,
    onClick: (Button) -> Unit
  ): Button {
    val button = AppCompatButton(this.context)
    button.text = context.getString(text)
    button.contentDescription = context.getString(description)
    button.layoutParams = this.buttonLayoutParameters()
    button.setOnClickListener {
      button.isEnabled = false
      onClick.invoke(button)
      button.isEnabled = true
    }
    return button
  }

  @UiThread
  fun createReadButton(
    onClick: (Button) -> Unit
  ): Button {
    return this.createButton(
      context = this.context,
      text = R.string.catalogRead,
      description = R.string.catalogAccessibilityBookRead,
      onClick = onClick
    )
  }

  @UiThread
  fun createListenButton(
    onClick: (Button) -> Unit
  ): Button {
    return this.createButton(
      context = this.context,
      text = R.string.catalogListen,
      description = R.string.catalogAccessibilityBookListen,
      onClick = onClick
    )
  }

  @UiThread
  fun createDownloadButton(
    onClick: (Button) -> Unit
  ): Button {
    return this.createButton(
      context = this.context,
      text = R.string.catalogDownload,
      description = R.string.catalogAccessibilityBookDownload,
      onClick = onClick
    )
  }

  @UiThread
  fun createRevokeHoldButton(
    onClick: (Button) -> Unit
  ): Button {
    return this.createButton(
      context = this.context,
      text = R.string.catalogCancelHold,
      description = R.string.catalogAccessibilityBookRevokeHold,
      onClick = onClick
    )
  }

  @UiThread
  fun createRevokeLoanButton(
    onClick: (Button) -> Unit
  ): Button {
    return this.createButton(
      context = this.context,
      text = R.string.catalogReturn,
      description = R.string.catalogAccessibilityBookRevokeLoan,
      onClick = onClick
    )
  }

  @UiThread
  fun createCancelDownloadButton(
    onClick: (Button) -> Unit
  ): Button {
    return this.createButton(
      context = this.context,
      text = R.string.catalogCancel,
      description = R.string.catalogAccessibilityBookDownloadCancel,
      onClick = onClick
    )
  }

  @UiThread
  fun createReserveButton(
    onClick: (Button) -> Unit
  ): Button {
    return this.createButton(
      context = this.context,
      text = R.string.catalogReserve,
      description = R.string.catalogAccessibilityBookReserve,
      onClick = onClick
    )
  }

  @UiThread
  fun createGetButton(
    onClick: (Button) -> Unit
  ): Button {
    return this.createButton(
      context = this.context,
      text = R.string.catalogGet,
      description = R.string.catalogAccessibilityBookBorrow,
      onClick = onClick
    )
  }

  @UiThread
  fun createDeleteButton(
    onClick: (Button) -> Unit
  ): Button {
    return this.createButton(
      context = this.context,
      text = R.string.catalogDelete,
      description = R.string.catalogAccessibilityBookDelete,
      onClick = onClick
    )
  }

  @UiThread
  fun createRetryButton(
    onClick: (Button) -> Unit
  ): Button {
    return this.createButton(
      context = this.context,
      text = R.string.catalogRetry,
      description = R.string.catalogAccessibilityBookErrorRetry,
      onClick = onClick
    )
  }

  @UiThread
  fun createDetailsButton(
    onClick: (Button) -> Unit
  ): Button {
    return this.createButton(
      context = this.context,
      text = R.string.catalogDetails,
      description = R.string.catalogAccessibilityBookErrorDetails,
      onClick = onClick
    )
  }

  @UiThread
  fun createDismissButton(
    onClick: (Button) -> Unit
  ): Button {
    return this.createButton(
      context = this.context,
      text = R.string.catalogDismiss,
      description = R.string.catalogAccessibilityBookErrorDismiss,
      onClick = onClick
    )
  }

  @UiThread
  fun createButtonSizedSpace(): View? {
    val space = Space(this.context)
    space.layoutParams = this.buttonLayoutParameters()
    space.visibility = View.INVISIBLE
    space.isEnabled = false
    return space
  }

  @UiThread
  fun buttonSpaceLayoutParameters(): LinearLayout.LayoutParams {
    val spaceLayoutParams = LinearLayout.LayoutParams(0, 0)
    spaceLayoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
    spaceLayoutParams.width = this.screenSizeInformation.dpToPixels(16).toInt()
    return spaceLayoutParams
  }

  @UiThread
  fun buttonLayoutParameters(): LinearLayout.LayoutParams {
    val buttonLayoutParams = LinearLayout.LayoutParams(0, 0)
    buttonLayoutParams.weight = 1.0f
    buttonLayoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
    buttonLayoutParams.width = this.screenSizeInformation.dpToPixels(64).toInt()
    return buttonLayoutParams
  }
}
