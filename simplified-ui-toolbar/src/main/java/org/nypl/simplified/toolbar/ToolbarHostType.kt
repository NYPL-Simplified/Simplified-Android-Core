package org.nypl.simplified.toolbar

import android.graphics.drawable.Drawable
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

  fun toolbarBackArrow(): Drawable

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
   * icon used is that returned by [toolbarBackArrow].
   */

  fun toolbarSetBackArrowConditionally(
    shouldArrowBePresent: () -> Boolean,
    onArrowClicked: () -> Unit
  ) {
    if (shouldArrowBePresent()) {
      this.toolbar.navigationIcon = this.toolbarBackArrow()
      this.toolbar.setNavigationOnClickListener { onArrowClicked() }
    } else {
      this.toolbar.navigationIcon = null
      this.toolbar.setNavigationOnClickListener(null)
    }
  }
}
