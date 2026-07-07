package org.librarysimplified.r2.api

import androidx.annotation.Dimension

/**
 * Settings for the UI.
 */

data class SR2UISettings(
  /**
   * Whether page buttons are to be enabled and, if so, their width in dp.
   */

  @Dimension(unit = Dimension.DP)
  val pageButtonWidth: Double?,
  /**
   * Whether swipe navigation is enabled.
   */

  val pageSwipeEnabled: Boolean
) {
  companion object {
    @Dimension(unit = Dimension.DP)
    val pageButtonWidthMinimum = 1.0

    @Dimension(unit = Dimension.DP)
    val pageButtonWidthMaximum = 80.0

    /**
     * The default UI settings.
     */

    val defaultSettings =
      SR2UISettings(
        pageButtonWidth = 64.0,
        pageSwipeEnabled = true
      )
  }
}
