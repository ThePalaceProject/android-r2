package org.librarysimplified.r2.vanilla.internal

import org.librarysimplified.r2.api.SR2Locator
import org.librarysimplified.r2.vanilla.internal.SR2NavigationNode.SR2NavigationReadingOrderNode
import org.librarysimplified.r2.vanilla.internal.SR2NavigationNode.SR2NavigationResourceNode
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication

object SR2NavigationGraphs {

  fun create(
    publication: Publication,
  ): SR2NavigationGraph {
    val readingOrder =
      publication.readingOrder.mapIndexed { index, link ->
        this.makeReadingOrderNode(publication, index, link)
      }
    val resources =
      publication.resources.map { link ->
        this.makeResourceNode(publication, link)
      }

    return SR2NavigationGraph(
      readingOrder = readingOrder,
      resources = resources,
    )
  }

  private fun makeResourceNode(
    publication: Publication,
    link: Link,
  ): SR2NavigationResourceNode {
    return SR2NavigationResourceNode(
      navigationPoint = this.makeNavigationPoint(publication, link),
    )
  }

  private fun makeReadingOrderNode(
    publication: Publication,
    index: Int,
    link: Link,
  ): SR2NavigationReadingOrderNode {
    return SR2NavigationReadingOrderNode(
      navigationPoint = this.makeNavigationPoint(publication, link),
      index = index,
    )
  }

  private fun makeNavigationPoint(publication: Publication, link: Link): SR2NavigationPoint {
    val title =
      link.title?.takeIf(String::isNotBlank)
        ?: titleFromTOC(publication.tableOfContents, link)
        ?: ""
    return SR2NavigationPoint(
      title,
      SR2Locator.SR2LocatorPercent(link.href, 0.0),
    )
  }

  /**
   * Perform a depth-first search for a title in the toc tree.
   * In case of multiples matches, deeper items have precedence because they're likely to be more specific.
   */

  private fun titleFromTOC(toc: List<Link>, link: Link): String? {
    for (entry in toc) {
      this.titleFromTOC(entry.children, link)?.let {
        return it
      }
    }

    for (entry in toc) {
      if (entry.href == link.href && entry.title != null) {
        return entry.title
      }
    }

    return null
  }
}
