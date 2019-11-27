package org.nypl.simplified.ui.toolbar

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.Toolbar

/**
 * The interface exposed by toolbar hosts.
 */

interface ToolbarHostType {

  /**
   * Retrieve access to the toolbar.
   */

  val toolbar: Toolbar

  /**
   * The icon used to represent a back arrow in the toolbar.
   */

  fun toolbarIconBackArrow(context: Context): Drawable {
    return context.getDrawable(this.toolbarIconResourceBackArrow())!!
  }

  /**
   * The icon used to represent a back arrow in the toolbar.
   */

  @DrawableRes
  fun toolbarIconResourceBackArrow(): Int {
    return R.drawable.toolbar_back_arrow
  }

  /**
   * The icon used to represent the overflow menu in the toolbar.
   */

  fun toolbarIconOverflow(context: Context): Drawable {
    return context.getDrawable(this.toolbarIconResourceOverflow())!!
  }

  /**
   * The icon used to represent the overflow menu in the toolbar.
   */

  @DrawableRes
  fun toolbarIconResourceOverflow(): Int {
    return R.drawable.toolbar_overflow
  }

  /**
   * A convenience function to clear the toolbar.
   */

  fun toolbarClearMenu() {
    this.toolbar.menu.clear()
  }

  /**
   * A convenience function to clear the toolbar.
   */

  fun toolbarSetTitleSubtitle(
    title: String,
    subtitle: String
  ) {
    this.toolbar.title = title
    this.toolbar.subtitle = subtitle
  }

  /**
   * A convenience function to set the up arrow for the toolbar conditionally. If the
   * [shouldArrowBePresent] returns `true`, the toolbar is considered with a back arrow that
   * executes [onArrowClicked] when clicked. Otherwise, the back arrow is made invisible. The
   * icon used is that returned by [toolbarIconBackArrow].
   */

  fun toolbarSetBackArrowConditionally(
    context: Context,
    shouldArrowBePresent: () -> Boolean,
    onArrowClicked: () -> Unit
  ) {
    if (shouldArrowBePresent()) {
      this.toolbar.navigationIcon = this.toolbarIconBackArrow(context)
      this.toolbar.setNavigationOnClickListener { onArrowClicked() }
    } else {
      this.toolbar.navigationIcon = null
      this.toolbar.setNavigationOnClickListener(null)
    }
  }
}
