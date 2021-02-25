package org.librarysimplified.r2.api

import android.graphics.Color

/**
 * A color scheme specification.
 */

enum class SR2ColorScheme {

  /**
   * Dark text on a light background.
   */

  DARK_TEXT_LIGHT_BACKGROUND,

  /**
   * Light text on a dark background.
   */

  LIGHT_TEXT_DARK_BACKGROUND,

  /**
   * Dark text on a sepia background.
   */

  DARK_TEXT_ON_SEPIA;

  /**
   * @return The foreground color as an integer color
   */

  fun foreground(): Int {
    return when (this) {
      DARK_TEXT_LIGHT_BACKGROUND ->
        Color.argb(0xff, 0x00, 0x00, 0x00)
      LIGHT_TEXT_DARK_BACKGROUND ->
        Color.argb(0xff, 0xff, 0xff, 0xff)
      DARK_TEXT_ON_SEPIA ->
        Color.argb(0xff, 0x00, 0x00, 0x00)
    }
  }

  /**
   * @return The background color as an integer color
   */

  fun background(): Int {
    return when (this) {
      DARK_TEXT_LIGHT_BACKGROUND ->
        Color.argb(0xff, 0xff, 0xff, 0xff)
      LIGHT_TEXT_DARK_BACKGROUND ->
        Color.argb(0xff, 0x00, 0x00, 0x00)
      DARK_TEXT_ON_SEPIA ->
        Color.argb(0xff, 0xf2, 0xe4, 0xcb)
    }
  }
}
