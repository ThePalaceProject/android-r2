package org.librarysimplified.r2.api

import org.librarysimplified.r2.api.SR2ColorScheme.DARK_TEXT_LIGHT_BACKGROUND
import org.librarysimplified.r2.api.SR2Font.FONT_SANS

/**
 * A specification of the current reader theme.
 */

data class SR2Theme(
  val colorScheme: SR2ColorScheme = DARK_TEXT_LIGHT_BACKGROUND,
  val font: SR2Font = FONT_SANS,
  val textSize: Double = 1.125
)
