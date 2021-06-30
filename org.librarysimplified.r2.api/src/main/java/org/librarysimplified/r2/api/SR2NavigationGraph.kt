package org.librarysimplified.r2.api

import org.librarysimplified.r2.api.SR2NavigationNode.SR2NavigationReadingOrderNode
import org.librarysimplified.r2.api.SR2NavigationNode.SR2NavigationResourceNode
import org.librarysimplified.r2.api.SR2NavigationNode.SR2NavigationTOCNode

/**
 * A navigation graph.
 */

data class SR2NavigationGraph(

  /**
   * The book reading order.
   */

  val readingOrder: List<SR2NavigationReadingOrderNode>,

  /**
   * The book's table of contents.
   */

  val tableOfContents: List<SR2NavigationTOCNode>,

  /**
   * The book's list of extra resources.
   */

  val resources: List<SR2NavigationResourceNode>
) {

  /*
   * A flattened table of contents.
   */

  val tableOfContentsFlat: List<SR2TOCEntry> =
    flattenTOC(this.tableOfContents)

  companion object {
    private fun flattenTOC(
      tableOfContents: List<SR2NavigationTOCNode>
    ): List<SR2TOCEntry> {
      val results = mutableListOf<SR2TOCEntry>()
      tableOfContents.forEach { node -> this.flattenTOCNode(node, 0, results) }
      return results.toList()
    }

    private fun flattenTOCNode(
      node: SR2NavigationTOCNode,
      depth: Int,
      results: MutableList<SR2TOCEntry>
    ) {
      results.add(SR2TOCEntry(node, depth))
      for (child in node.children) {
        this.flattenTOCNode(child, depth + 1, results)
      }
    }
  }
}
