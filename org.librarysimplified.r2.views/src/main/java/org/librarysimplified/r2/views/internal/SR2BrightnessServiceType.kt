package org.librarysimplified.r2.views.internal

/**
 * A brightness service.
 */

interface SR2BrightnessServiceType {

  /**
   * @return The current device brightness in the range [0, 1]
   */

  fun brightness(): Double

  /**
   * Set the device brightness in the range [0, 1]
   */

  fun setBrightness(brightness: Double)
}
