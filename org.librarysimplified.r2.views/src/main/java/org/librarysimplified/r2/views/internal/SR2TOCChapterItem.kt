package org.librarysimplified.r2.views.internal

import org.librarysimplified.r2.api.SR2NavigationNode
import org.librarysimplified.r2.api.SR2TOCEntry

internal data class SR2TOCChapterItem(
  val title: String,
  val href: String,
  val depth: Int
)

internal fun SR2TOCEntry.toChapterItem() = SR2TOCChapterItem(
  this.node.title,
  this.node.navigationPoint.locator.chapterHref,
  this.depth
)

internal fun SR2NavigationNode.SR2NavigationReadingOrderNode.toChapterItem() = SR2TOCChapterItem(
  this.title,
  this.navigationPoint.locator.chapterHref,
  0
)
