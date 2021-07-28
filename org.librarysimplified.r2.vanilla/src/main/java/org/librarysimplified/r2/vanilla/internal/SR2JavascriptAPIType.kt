package org.librarysimplified.r2.vanilla.internal

import androidx.annotation.UiThread
import com.google.common.util.concurrent.ListenableFuture
import org.librarysimplified.r2.api.SR2PublisherCSS
import org.librarysimplified.r2.api.SR2ScrollingMode

/**
 * The Javascript API exported by web views.
 */

internal interface SR2JavascriptAPIType {

  /**
   * Open the next page in the current chapter.
   */

  @UiThread
  fun openPageNext(): ListenableFuture<String>

  /**
   * Open the previous page in the current chapter.
   */

  @UiThread
  fun openPagePrevious(): ListenableFuture<String>

  /**
   * Open the final page in the current chapter.
   */

  @UiThread
  fun openPageLast(): ListenableFuture<String>

  /**
   * Set the font family for the reader.
   */

  @UiThread
  fun setFontFamily(value: String): ListenableFuture<*>

  /**
   * Set the text scale (in the range [0, n], where `n = 1.0` means "100%".
   */

  @UiThread
  fun setFontSize(value: Double): ListenableFuture<*>

  /**
   * Set the reader color scheme.
   */

  @UiThread
  fun setTheme(value: SR2ReadiumInternalTheme): ListenableFuture<String>

  /**
   * Set the current chapter position. This must be in the range [0, 1].
   */

  @UiThread
  fun setProgression(progress: Double): ListenableFuture<String>

  /**
   * Broadcast the current reading position.
   */

  @UiThread
  fun broadcastReadingPosition(): ListenableFuture<*>

  /**
   * Set the scrolling mode.
   */

  @UiThread
  fun setScrollMode(mode: SR2ScrollingMode): ListenableFuture<*>

  /**
   * Scroll to the element with the given ID.
   */

  @UiThread
  fun scrollToId(id: String): ListenableFuture<*>

  /**
   * Enable/disable publisher CSS.
   */

  @UiThread
  fun setPublisherCSS(css: SR2PublisherCSS): ListenableFuture<*>
}
