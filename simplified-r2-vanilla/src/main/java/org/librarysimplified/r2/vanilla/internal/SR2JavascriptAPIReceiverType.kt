package org.librarysimplified.r2.vanilla.internal

/**
 * Methods called from javascript running inside a WebView.
 */

internal interface SR2JavascriptAPIReceiverType {

  /**
   * The reading position has changed.
   *
   * @param currentPage The page position within the chapter.
   * @param pageCount Total pages within the chapter with the current styling.
   */

  @android.webkit.JavascriptInterface
  fun onReadingPositionChanged(currentPage: Int, pageCount: Int)

  /** The center of the screen was tapped. */

  @android.webkit.JavascriptInterface
  fun onCenterTapped()

  /** The screen was clicked somewhere. */

  @android.webkit.JavascriptInterface
  fun onClicked()

  /** The left edge of the screen was tapped. */

  @android.webkit.JavascriptInterface
  fun onLeftTapped()

  /** The right edge of the screen was tapped. */

  @android.webkit.JavascriptInterface
  fun onRightTapped()
}
