package org.librarysimplified.r2.vanilla.internal

import org.librarysimplified.r2.api.SR2Locator
import org.librarysimplified.r2.vanilla.internal.SR2NavigationNode.SR2NavigationReadingOrderNode
import org.librarysimplified.r2.vanilla.internal.SR2NavigationNode.SR2NavigationResourceNode

/**
 * A navigation graph.
 */

data class SR2NavigationGraph(

  /**
   * The book reading order.
   */

  val readingOrder: List<SR2NavigationReadingOrderNode>,

  /**
   * The book's list of extra resources.
   */

  val resources: List<SR2NavigationResourceNode>,
) {
  init {
    require(this.readingOrder.sortedBy { it.index } == this.readingOrder) {
      "The reading order must be sorted"
    }
    require(this.readingOrder.associateBy { it.index }.size == this.readingOrder.size) {
      "The reading order indices must be unique"
    }
  }

  /**
   * The node pointing at the start of the book.
   */

  fun start(): SR2NavigationTarget {
    return SR2NavigationTarget(
      node = this.readingOrder.first(),
      extraFragment = null,
    )
  }

  /**
   * Try to find a relevant navigation node for the given locator, but do not check outside
   * of the reading order to do so.
   */

  fun findNavigationReadingOrderNodeExact(locator: SR2Locator): SR2NavigationReadingOrderNode? {
    for (node in this.readingOrder) {
      if (node.matches(locator)) {
        return node
      }
    }
    return null
  }

  /**
   * Try to find a relevant navigation node for the given locator, but do not check outside
   * of the resources to do so.
   */

  fun findResourcesNodeExact(locator: SR2Locator): SR2NavigationResourceNode? {
    for (entry in this.resources) {
      if (entry.matches(locator)) {
        return entry
      }
    }
    return null
  }

  /**
   * Find a relevant navigation node for the given locator.
   */

  fun findNavigationNode(locator: SR2Locator): SR2NavigationTarget? {
    val exact =
      this.findNavigationReadingOrderNodeExact(locator)
        ?: this.findResourcesNodeExact(locator)

    if (exact != null) {
      return SR2NavigationTarget(
        node = exact,
        extraFragment = null,
      )
    }

    /*
     * If there wasn't a resource that matched the locator exactly, then try the search
     * again but without the fragment (if there is one). Record the fragment as an extra.
     */

    val fragment = locator.chapterHref.substringAfter('#', "")
    return if (fragment.isNotBlank()) {
      val withoutFragment =
        when (locator) {
          is SR2Locator.SR2LocatorChapterEnd ->
            locator.copy(chapterHref = locator.chapterHref.substringBefore('#'))
          is SR2Locator.SR2LocatorPercent ->
            locator.copy(chapterHref = locator.chapterHref.substringBefore('#'))
        }

      (
        this.findNavigationReadingOrderNodeExact(withoutFragment)
          ?: findResourcesNodeExact(withoutFragment)
        )
        ?.let { node -> SR2NavigationTarget(node, fragment) }
    } else {
      null
    }
  }

  /**
   * Find the "previous" node for the given node. There may not be any such thing as
   * a "previous" node for items that are not in the reading order.
   */

  fun findPreviousNode(currentNode: SR2NavigationNode): SR2NavigationNode? {
    return when (currentNode) {
      is SR2NavigationReadingOrderNode -> {
        if (currentNode.index == 0) {
          null
        } else {
          this.readingOrder[currentNode.index - 1]
        }
      }

      is SR2NavigationResourceNode ->
        null
    }
  }

  /**
   * Find the "next" node for the given node. There may not be any such thing as
   * a "next" node for items that are not in the reading order.
   */

  fun findNextNode(currentNode: SR2NavigationNode): SR2NavigationNode? {
    return when (currentNode) {
      is SR2NavigationReadingOrderNode -> {
        if (currentNode.index == this.readingOrder.size - 1) {
          null
        } else {
          this.readingOrder[currentNode.index + 1]
        }
      }

      is SR2NavigationResourceNode ->
        null
    }
  }
}
