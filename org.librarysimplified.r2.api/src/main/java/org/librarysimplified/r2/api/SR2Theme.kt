package org.librarysimplified.r2.api

/**
 * A specification of the current reader theme.
 */

data class SR2Theme(
  val colorScheme: SR2ColorScheme,
  val font: SR2Font,
  val textSize: Double = 1.125
)
