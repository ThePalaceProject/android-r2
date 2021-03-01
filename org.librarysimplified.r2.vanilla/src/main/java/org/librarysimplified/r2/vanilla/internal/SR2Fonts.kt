package org.librarysimplified.r2.vanilla.internal

import org.librarysimplified.r2.api.SR2Font

internal object SR2Fonts {
  fun fontFamilyStringOf(font: SR2Font): String {
    return when (font) {
      SR2Font.FONT_SANS ->
        "sans-serif"
      SR2Font.FONT_SERIF ->
        "serif"
      SR2Font.FONT_OPEN_DYSLEXIC ->
        "OpenDyslexic"
    }
  }
}
