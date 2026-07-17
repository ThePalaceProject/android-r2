package org.librarysimplified.r2.vanilla.internal

import androidx.annotation.UiThread
import org.librarysimplified.r2.api.SR2PublisherCSS
import org.librarysimplified.r2.api.SR2Theme
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
  fun openPageNext(): CompletableFuture<*>

  /**
   * Open the previous page in the current chapter.
   */

  @UiThread
  fun openPagePrevious(): CompletableFuture<*>

  /**
   * Open the final page in the current chapter.
   */

  @UiThread
  fun openPageLast(): CompletableFuture<*>

  @UiThread
  fun setSettings(value: SR2Theme): CompletableFuture<*>

  /**
   * Set the current chapter position. This must be in the range [0, 1].
   */

  @UiThread
  fun setProgression(progress: Double): CompletableFuture<String>

  /**
   * Scroll to the element with the given ID.
   */

  @UiThread
  fun scrollToId(id: String): CompletableFuture<*>
}
