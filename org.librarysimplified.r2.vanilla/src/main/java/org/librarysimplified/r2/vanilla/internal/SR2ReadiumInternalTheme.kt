package org.librarysimplified.r2.vanilla.internal

import org.librarysimplified.r2.api.SR2ColorScheme

internal enum class SR2ReadiumInternalTheme {
  LIGHT,
  DARK,
  DAY,
  NIGHT,
  SEPIA,
  ;

  companion object {
    fun from(
      colorScheme: SR2ColorScheme,
    ): SR2ReadiumInternalTheme {
      return when (colorScheme) {
        SR2ColorScheme.DARK_TEXT_LIGHT_BACKGROUND ->
          LIGHT
        SR2ColorScheme.LIGHT_TEXT_DARK_BACKGROUND ->
          DARK
        SR2ColorScheme.DARK_TEXT_ON_SEPIA ->
          SEPIA
      }
    }
  }
}
