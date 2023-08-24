package org.librarysimplified.r2.views.internal

import android.app.Activity

/**
 * The default implementation of the [SR2BrightnessServiceType] interface.
 */

class SR2BrightnessService(
  private val activity: Activity,
) : SR2BrightnessServiceType {

  override fun brightness(): Double {
    val layoutParams = this.activity.window.attributes
    val currentBrightness = layoutParams.screenBrightness.toDouble()
    return if (currentBrightness < 0 || currentBrightness > 1.0) {
      1.0
    } else {
      currentBrightness
    }
  }

  override fun setBrightness(
    brightness: Double,
  ) {
    val layoutParams = this.activity.window.attributes
    layoutParams.screenBrightness = brightness.toFloat()
    this.activity.window.attributes = layoutParams
  }
}
