package org.librarysimplified.r2.vanilla

import androidx.annotation.UiThread

/**
 * The Javascript API exported by web views.
 */

interface SR2JavascriptAPIType {

  /**
   * Open the next page in the current chapter.
   */

  @UiThread
  fun openPageNext()

  /**
   * Open the previous page in the current chapter.
   */

  @UiThread
  fun openPagePrevious()

  /**
   * Open the final page in the current chapter.
   */

  @UiThread
  fun openPageLast()

}
