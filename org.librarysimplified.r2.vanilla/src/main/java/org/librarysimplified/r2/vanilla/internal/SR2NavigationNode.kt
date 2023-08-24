package org.librarysimplified.r2.vanilla.internal

import org.librarysimplified.r2.api.SR2Locator

/**
 * A navigation node.
 */

sealed class SR2NavigationNode {

  /**
   * @return true If the locator matches this navigation node
   */

  fun matches(locator: SR2Locator): Boolean {
    val nodeURI = this.navigationPoint.locator.chapterHref
    val wantURI = locator.chapterHref
    return nodeURI == wantURI
  }

  abstract val navigationPoint: SR2NavigationPoint

  /**
   * The node title, equivalent to that of the navigation point within.
   */

  val title: String
    get() = this.navigationPoint.title

  /**
   * A node in the reading order.
   */

  data class SR2NavigationReadingOrderNode(
    override val navigationPoint: SR2NavigationPoint,
    val index: Int,
  ) : SR2NavigationNode()

  /**
   * A node in the resource list.
   */

  data class SR2NavigationResourceNode(
    override val navigationPoint: SR2NavigationPoint,
  ) : SR2NavigationNode()
}
