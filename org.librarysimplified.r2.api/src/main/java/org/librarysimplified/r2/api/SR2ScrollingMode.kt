package org.librarysimplified.r2.api

/**
 * A specification of the scrolling mode.
 */

enum class SR2ScrollingMode {

  /**
   * Paginated scrolling mode; book chapters are presented as a series of discrete pages.
   */

  SCROLLING_MODE_PAGINATED,

  /**
   * Continuous scrolling mode; book chapters are presented as a single scrollable region of text.
   */

  SCROLLING_MODE_CONTINUOUS
}
