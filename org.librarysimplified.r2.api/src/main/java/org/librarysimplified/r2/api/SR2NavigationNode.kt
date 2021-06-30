package org.librarysimplified.r2.api

/**
 * A navigation node.
 */

sealed class SR2NavigationNode {

  abstract val navigationPoint: SR2NavigationPoint

  /**
   * A node in the reading order.
   */

  data class SR2NavigationReadingOrderNode(
    override val navigationPoint: SR2NavigationPoint,
    val index: Int
  ) : SR2NavigationNode()

  /**
   * A node in the table of contents.
   */

  data class SR2NavigationTOCNode(
    override val navigationPoint: SR2NavigationPoint,
    val children: List<SR2NavigationTOCNode>
  ) : SR2NavigationNode()

  /**
   * A node in the resource list.
   */

  data class SR2NavigationResourceNode(
    override val navigationPoint: SR2NavigationPoint
  ) : SR2NavigationNode()
}
