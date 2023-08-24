package org.librarysimplified.r2.vanilla.internal

/**
 * Methods called from javascript running inside a WebView.
 */

internal interface SR2JavascriptAPIReceiverType {

  /**
   * The reading position has changed.
   *
   * @param chapterProgress The chapter progress in the range `[0, 1]`.
   * @param currentPage The page position within the chapter.
   * @param pageCount Total pages within the chapter with the current styling.
   */

  @android.webkit.JavascriptInterface
  fun onReadingPositionChanged(
    chapterProgress: Double,
    currentPage: Int,
    pageCount: Int,
  )

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

  /** The user swiped left. */

  @android.webkit.JavascriptInterface
  fun onLeftSwiped()

  /** The user swiped right. */

  @android.webkit.JavascriptInterface
  fun onRightSwiped()

  @android.webkit.JavascriptInterface
  fun getViewportWidth(): Double

  /**
   * An error was encountered in the JS.
   */

  @android.webkit.JavascriptInterface
  fun logError(
    message: String?,
    file: String?,
    line: String?,
  )
}
