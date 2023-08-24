package org.librarysimplified.r2.vanilla.internal

import org.librarysimplified.r2.api.SR2Locator

/**
 * A navigation point.
 */

data class SR2NavigationPoint(
  val title: String,
  val locator: SR2Locator,
)
