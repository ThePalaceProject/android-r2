package org.librarysimplified.r2.vanilla.internal

import androidx.annotation.UiThread
import org.librarysimplified.r2.api.SR2PublisherCSS
import org.librarysimplified.r2.api.SR2ScrollingMode
import java.util.concurrent.CompletableFuture

/**
 * The Javascript API exported by web views.
 */

internal interface SR2JavascriptAPIType {

  /**
   * Highlight or clear the highlight for the given searching terms.
   */
  @UiThread
  fun highlightSearchingTerms(
    searchingTerms: String,
    clearHighlight: Boolean,
  ): CompletableFuture<String>

  /**
   * Open the next page in the current chapter.
   */

  @UiThread
  fun openPageNext(): CompletableFuture<String>

  /**
   * Open the previous page in the current chapter.
   */

  @UiThread
  fun openPagePrevious(): CompletableFuture<String>

  /**
   * Open the final page in the current chapter.
   */

  @UiThread
  fun openPageLast(): CompletableFuture<String>

  /**
   * Set the font family for the reader.
   */

  @UiThread
  fun setFontFamily(value: String): CompletableFuture<*>

  /**
   * Set the text scale (in the range [0, n], where `n = 1.0` means "100%".
   */

  @UiThread
  fun setFontSize(value: Double): CompletableFuture<*>

  /**
   * Set the reader color scheme.
   */

  @UiThread
  fun setTheme(value: SR2ReadiumInternalTheme): CompletableFuture<String>

  /**
   * Set the current chapter position. This must be in the range [0, 1].
   */

  @UiThread
  fun setProgression(progress: Double): CompletableFuture<String>

  /**
   * Broadcast the current reading position.
   */

  @UiThread
  fun broadcastReadingPosition(): CompletableFuture<*>

  /**
   * Set the scrolling mode.
   */

  @UiThread
  fun setScrollMode(mode: SR2ScrollingMode): CompletableFuture<*>

  /**
   * Scroll to the element with the given ID.
   */

  @UiThread
  fun scrollToId(id: String): CompletableFuture<*>

  /**
   * Enable/disable publisher CSS.
   */

  @UiThread
  fun setPublisherCSS(css: SR2PublisherCSS): CompletableFuture<*>
}
