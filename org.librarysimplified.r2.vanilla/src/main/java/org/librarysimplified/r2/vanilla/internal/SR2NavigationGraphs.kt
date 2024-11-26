package org.librarysimplified.r2.vanilla.internal

import org.librarysimplified.r2.api.SR2Locator
import org.librarysimplified.r2.vanilla.internal.SR2NavigationNode.SR2NavigationReadingOrderNode
import org.librarysimplified.r2.vanilla.internal.SR2NavigationNode.SR2NavigationResourceNode
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.slf4j.LoggerFactory
import java.net.URI

object SR2NavigationGraphs {

  private val logger =
    LoggerFactory.getLogger(SR2NavigationGraphs::class.java)

  private data class FlatTOCLink(
    val depth: Int,
    val link: Link,
  )

  fun create(
    publication: Publication,
  ): SR2NavigationGraph {
    val flatToc = mutableListOf<FlatTOCLink>()
    flattenTOC(flatToc, publication.tableOfContents, depth = 0)

    val readingOrder =
      publication.readingOrder.mapIndexed { index, link ->
        this.makeReadingOrderNode(flatToc, index, link)
      }
    val resources =
      publication.resources.map { link ->
        this.makeResourceNode(flatToc, link)
      }

    return SR2NavigationGraph(
      readingOrder = readingOrder,
      resources = resources,
    )
  }

  private fun flattenTOC(
    flatToc: MutableList<FlatTOCLink>,
    tableOfContents: List<Link>,
    depth: Int,
  ) {
    for (entry in tableOfContents) {
      flatToc.add(FlatTOCLink(depth, entry))
      flattenTOC(
        flatToc = flatToc,
        tableOfContents = entry.children,
        depth = depth + 1,
      )
    }
  }

  private fun makeResourceNode(
    flatTOC: List<FlatTOCLink>,
    link: Link,
  ): SR2NavigationResourceNode {
    return SR2NavigationResourceNode(
      navigationPoint = this.makeNavigationPoint(flatTOC, link),
    )
  }

  private fun makeReadingOrderNode(
    flatTOC: List<FlatTOCLink>,
    index: Int,
    link: Link,
  ): SR2NavigationReadingOrderNode {
    return SR2NavigationReadingOrderNode(
      navigationPoint = this.makeNavigationPoint(flatTOC, link),
      index = index,
    )
  }

  private fun makeNavigationPoint(
    flatTOC: List<FlatTOCLink>,
    link: Link,
  ): SR2NavigationPoint {
    val title = this.findTitleFor(flatTOC, link)
    this.logger.debug("Title: {} -> {}", link.href, title)

    return SR2NavigationPoint(
      title,
      SR2Locator.SR2LocatorPercent.create(link.href, 0.0),
    )
  }

  /**
   * Perform a depth-first search for a title in the toc tree.
   * In case of multiples matches, deeper items have precedence because they're likely to be
   * more specific.
   */

  private fun findTitleFor(
    flatTOC: List<FlatTOCLink>,
    link: Link,
  ): String {
    val linkTitle = link.title?.trim()
    if (!linkTitle.isNullOrBlank()) {
      return linkTitle
    }

    val bestLink =
      flatTOC.filter { flatLink -> hasSuitableTitle(candidateEntry = flatLink.link, link) }
        .sortedBy(FlatTOCLink::depth)
        .lastOrNull()

    return bestLink?.link?.title ?: ""
  }

  private fun hasSuitableTitle(
    candidateEntry: Link,
    link: Link,
  ): Boolean {
    /*
     * If there isn't a non-blank title, then we know this entry isn't suitable.
     */

    val title = candidateEntry.title ?: return false
    if (title.isBlank()) {
      return false
    }

    /*
     * If the URLs match exactly, then we know this entry is suitable.
     */

    if (candidateEntry.href == link.href) {
      return true
    }

    /*
     * Otherwise, if we _don't_ have a fragment, and the target link body matches ours but
     * _does_ have a fragment, we treat it as suitable.
     */

    try {
      val linkAsURI = URI.create(link.href.toString())
      if (!linkAsURI.fragment.isNullOrBlank()) {
        return false
      }

      val linkWithoutFragment =
        URI.create(linkAsURI.toString().substringBefore('#'))
      val candidateEntryAsURI =
        URI.create(candidateEntry.href.toString())
      val candidateEntryWithoutFragment =
        URI.create(candidateEntryAsURI.toString().substringBefore('#'))

      return candidateEntryWithoutFragment == linkWithoutFragment
    } catch (e: Exception) {
      return false
    }
  }
}
