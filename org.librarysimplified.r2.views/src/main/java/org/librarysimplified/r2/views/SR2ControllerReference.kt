package org.librarysimplified.r2.views

import org.librarysimplified.r2.api.SR2ControllerType

data class SR2ControllerReference(
  val controller: SR2ControllerType,
  val isFirstStartup: Boolean
)
