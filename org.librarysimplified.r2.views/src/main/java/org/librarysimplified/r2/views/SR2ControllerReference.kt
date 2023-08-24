package org.librarysimplified.r2.views

import org.librarysimplified.r2.api.SR2ControllerType

/**
 * A reference to an SR2 controller.
 */

data class SR2ControllerReference(

  /**
   * The controller.
   */

  val controller: SR2ControllerType,

  /**
   * `true` if this is the first time the controller has been requested
   */

  val isFirstStartup: Boolean,
)
