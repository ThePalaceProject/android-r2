package org.librarysimplified.r2.vanilla.internal

/**
 * Methods called from javascript running inside a WebView.
 */

internal interface SR2JavascriptAPIReceiverType {
  @android.webkit.JavascriptInterface
  fun onReadingPositionChanged(
    chapterProgress: Double,
    currentPage: Int,
    pageCount: Int,
  )

  @android.webkit.JavascriptInterface
  fun onWantChapterNext()

  @android.webkit.JavascriptInterface
  fun onWantChapterPrevious()

  @android.webkit.JavascriptInterface
  fun onPageSetInitial()

  @android.webkit.JavascriptInterface
  fun onPageSetCalculating(progress: Double)

  @android.webkit.JavascriptInterface
  fun onPageSetReady(count: Double)

  @android.webkit.JavascriptInterface
  fun onGetViewportWidth(): Double

  @android.webkit.JavascriptInterface
  fun onLogError(
    message: String?,
    file: String?,
    line: String?,
  )
}
