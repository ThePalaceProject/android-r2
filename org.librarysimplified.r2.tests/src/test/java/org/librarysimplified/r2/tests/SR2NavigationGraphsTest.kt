package org.librarysimplified.r2.tests

import android.content.Context
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.librarysimplified.r2.api.SR2Locator.*
import org.librarysimplified.r2.api.SR2Locator.SR2LocatorPercent.*
import org.librarysimplified.r2.vanilla.internal.SR2NavigationGraphs
import org.librarysimplified.r2.vanilla.internal.SR2NavigationNode
import org.librarysimplified.r2.vanilla.internal.SR2NavigationNode.SR2NavigationReadingOrderNode
import org.librarysimplified.r2.vanilla.internal.SR2NavigationTarget
import org.mockito.Mockito
import org.readium.r2.shared.publication.Href
import org.slf4j.LoggerFactory
import java.io.File

@Disabled("Roboelectric is not yet compatible with JUnit5")
class SR2NavigationGraphsTest {

  private val logger =
    LoggerFactory.getLogger(SR2NavigationGraphsTest::class.java)

  private lateinit var context: Context
  private lateinit var directory: File

  @BeforeEach
  fun testSetup() {
    this.directory =
      TestDirectories.temporaryDirectory()
    this.context =
      Mockito.mock(Context::class.java)

    Mockito.`when`(this.context.applicationContext)
      .thenReturn(this.context)
  }

  @Test
  fun testBigTOC() {
    val publication =
      TestPublication.loadPublication("epubs/bigtoc.epub", this.directory)
    val graph =
      SR2NavigationGraphs.create(publication)

    assertEquals(1000, graph.readingOrder.size)
    assertEquals(2, graph.resources.size)

    val start = graph.start()
    assertTrue(start.node.navigationPoint.title.startsWith("Page 1."))

    var node: SR2NavigationNode? = start.node
    for (i in 0 until 1000) {
      node = graph.findNextNode(node!!) as SR2NavigationReadingOrderNode?
    }
    assertEquals(null, node)
  }

  @Test
  fun testLinks() {
    val publication =
      TestPublication.loadPublication("epubs/links.epub", this.directory)
    val graph =
      SR2NavigationGraphs.create(publication)

    assertEquals(1, graph.readingOrder.size)
    assertEquals(4, graph.resources.size)

    val start = graph.start()
    assertEquals("Page 0.", start.node.navigationPoint.title)
    val node = graph.findNextNode(start.node)
    assertEquals(null, node)
  }

  @Test
  fun testNestedTOC() {
    val publication = TestPublication.loadPublication("epubs/nestedtoc.epub", this.directory)
    val graph = SR2NavigationGraphs.create(publication)

    fun List<SR2NavigationNode>.firstWithHref(href: String): SR2NavigationNode =
      this.first { it.navigationPoint.locator.chapterHref == Href(href)!! }

    assertEquals(1, graph.readingOrder.size)
    assertEquals(4, graph.resources.size)
    assertEquals("Something", graph.readingOrder[0].navigationPoint.title)

    var target: SR2NavigationTarget?
    target = graph.findNavigationNode(SR2LocatorPercent.start(Href("/epub/text/p1.xhtml")!!))
    assertEquals("Something Nested", target!!.node.navigationPoint.title)
    assertEquals(null, target.extraFragment)

    target = graph.findNavigationNode(SR2LocatorPercent.start(Href("/epub/text/p2.xhtml")!!))
    assertEquals("Something Else Nested", target!!.node.navigationPoint.title)
    assertEquals(null, target.extraFragment)

    val start = graph.start()
    assertEquals("Something", start.node.navigationPoint.title)

    val node = graph.findNextNode(start.node)
    assertEquals(null, node)
  }

  @Test
  fun testGreatExpectations() {
    val publication =
      TestPublication.loadPublication(
        "epubs/charles-dickens_great-expectations.epub",
        this.directory,
      )
    val graph =
      SR2NavigationGraphs.create(publication)

    assertEquals(63, graph.readingOrder.size)
    assertEquals(7, graph.resources.size)

    val start = graph.start()
    assertEquals("Titlepage", start.node.navigationPoint.title)

    var node: SR2NavigationNode? = start.node
    for (i in 0 until 63) {
      node = graph.findNextNode(node!!) as SR2NavigationReadingOrderNode?
    }
    assertEquals(null, node)
  }

  @Test
  fun testFragments() {
    val publication = TestPublication.loadPublication("epubs/fragments.epub", this.directory)
    val graph = SR2NavigationGraphs.create(publication)

    assertEquals(3, graph.readingOrder.size)

    val start = graph.start()
    assertEquals("Chapter 0", start.node.navigationPoint.title)

    var node: SR2NavigationNode? = start.node
    node = graph.findNextNode(node!!)
    assertEquals("Chapter 1", node!!.navigationPoint.title)
    node = graph.findNextNode(node)
    assertEquals("Chapter 2", node!!.navigationPoint.title)
    node = graph.findNextNode(node)
    assertEquals(null, node)

    var target: SR2NavigationTarget?
    target = graph.findNavigationNode(SR2LocatorPercent.start(Href("/epub/text/p0.xhtml")!!))
    assertEquals("Chapter 0", target!!.node.navigationPoint.title)
    assertEquals(null, target.extraFragment)

    target = graph.findNavigationNode(SR2LocatorPercent.start(Href("/epub/text/p0.xhtml#p0_02")!!))
    assertEquals("Chapter 0", target!!.node.navigationPoint.title)
    assertEquals("p0_02", target.extraFragment)

    target = graph.findNavigationNode(SR2LocatorPercent.start(Href("/epub/text/p1.xhtml#p1_01")!!))
    assertEquals("Chapter 1", target!!.node.navigationPoint.title)
    assertEquals("p1_01", target.extraFragment)

    target = graph.findNavigationNode(SR2LocatorPercent.start(Href("/epub/text/p1.xhtml#p1_02")!!))
    assertEquals("Chapter 1", target!!.node.navigationPoint.title)
    assertEquals("p1_02", target.extraFragment)

    target = graph.findNavigationNode(SR2LocatorPercent.start(Href("/epub/text/p2.xhtml#p2_01")!!))
    assertEquals("Chapter 2", target!!.node.navigationPoint.title)
    assertEquals("p2_01", target.extraFragment)

    target = graph.findNavigationNode(SR2LocatorPercent.start(Href("/epub/text/p2.xhtml#p2_02")!!))
    assertEquals("Chapter 2", target!!.node.navigationPoint.title)
    assertEquals("p2_02", target.extraFragment)
  }
}
