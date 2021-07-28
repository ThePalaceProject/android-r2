package org.librarysimplified.r2.api

import org.librarysimplified.r2.api.SR2ColorScheme.DARK_TEXT_LIGHT_BACKGROUND
import org.librarysimplified.r2.api.SR2Font.FONT_SANS
import org.librarysimplified.r2.api.SR2PublisherCSS.SR2_PUBLISHER_DEFAULT_CSS_DISABLED

/**
 * A specification of the current reader theme.
 */

data class SR2Theme(
  val colorScheme: SR2ColorScheme = DARK_TEXT_LIGHT_BACKGROUND,
  val font: SR2Font = FONT_SANS,
  val textSize: Double = 1.0,
  val publisherCSS: SR2PublisherCSS = SR2_PUBLISHER_DEFAULT_CSS_DISABLED
) {
  init {
    check(this.textSize >= TEXT_SIZE_MINIMUM_INCLUSIVE) {
      "Text size $textSize must be >= $TEXT_SIZE_MINIMUM_INCLUSIVE"
    }
    check(this.textSize < TEXT_SIZE_MAXIMUM_EXCLUSIVE) {
      "Text size $textSize must be < $TEXT_SIZE_MAXIMUM_EXCLUSIVE"
    }
  }

  companion object {
    const val TEXT_SIZE_MAXIMUM_EXCLUSIVE = 8.0
    const val TEXT_SIZE_MINIMUM_INCLUSIVE = 0.7

    /**
     * Constrain the given size parameter to the allowed range [TEXT_SIZE_MINIMUM_INCLUSIVE, TEXT_SIZE_MAXIMUM_EXCLUSIVE).
     */

    fun sizeConstrain(size: Double): Double {
      return Math.max(TEXT_SIZE_MINIMUM_INCLUSIVE, Math.min(size, TEXT_SIZE_MAXIMUM_EXCLUSIVE - 0.01))
    }
  }
}
