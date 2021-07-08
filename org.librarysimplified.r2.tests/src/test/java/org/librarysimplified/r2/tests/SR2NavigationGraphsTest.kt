package org.librarysimplified.r2.tests

import android.content.Context
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.librarysimplified.r2.api.SR2Locator.SR2LocatorPercent
import org.librarysimplified.r2.api.SR2NavigationNode
import org.librarysimplified.r2.api.SR2NavigationNode.SR2NavigationReadingOrderNode
import org.librarysimplified.r2.api.SR2NavigationTarget
import org.librarysimplified.r2.vanilla.internal.SR2NavigationGraphs
import org.mockito.Mockito
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.asset.FileAsset
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.streamer.Streamer
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

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

  private fun loadPublication(name: String): Publication {
    val path = "/org/librarysimplified/r2/tests/$name"
    val url = SR2NavigationGraphsTest::class.java.getResource(path)
      ?: throw FileNotFoundException(path)

    val output = File(this.directory, "output.epub")
    url.openStream().use { inputStream ->
      output.outputStream().use { outputStream ->
        inputStream.copyTo(outputStream)
      }
    }

    val streamer = Streamer(this.context)
    return runBlocking {
      streamer.open(FileAsset(output), allowUserInteraction = false)
    }.getOrElse {
      throw IOException("Failed to open EPUB", it)
    }
  }

  @Test
  fun testBigTOC() {
    val publication = this.loadPublication("epubs/bigtoc.epub")
    val graph = SR2NavigationGraphs.create(publication)

    assertEquals(1000, graph.tableOfContentsFlat.size)
    assertEquals(1000, graph.tableOfContents.size)
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
    val publication = this.loadPublication("epubs/links.epub")
    val graph = SR2NavigationGraphs.create(publication)

    assertEquals(1, graph.tableOfContentsFlat.size)
    assertEquals(1, graph.tableOfContents.size)
    assertEquals(1, graph.readingOrder.size)
    assertEquals(4, graph.resources.size)

    val start = graph.start()
    assertEquals("Page 0.", start.node.navigationPoint.title)
    val node = graph.findNextNode(start.node)
    assertEquals(null, node)
  }

  @Test
  fun testNestedTOC() {
    val publication = this.loadPublication("epubs/nestedtoc.epub")
    val graph = SR2NavigationGraphs.create(publication)

    assertEquals(3, graph.tableOfContentsFlat.size)
    assertEquals(1, graph.tableOfContents.size)
    assertEquals(1, graph.readingOrder.size)
    assertEquals("Something", graph.readingOrder[0].navigationPoint.title)
    assertEquals("Something", graph.tableOfContentsFlat[0].node.navigationPoint.title)
    assertEquals("Something Nested", graph.tableOfContentsFlat[1].node.navigationPoint.title)
    assertEquals("Something Else Nested", graph.tableOfContentsFlat[2].node.navigationPoint.title)

    val start = graph.start()
    assertEquals("Something", start.node.navigationPoint.title)

    val node = graph.findNextNode(start.node)
    assertEquals(null, node)
  }

  @Test
  fun testGreatExpectations() {
    val publication = this.loadPublication("epubs/charles-dickens_great-expectations.epub")
    val graph = SR2NavigationGraphs.create(publication)

    assertEquals(63, graph.tableOfContentsFlat.size)
    assertEquals(63, graph.tableOfContents.size)
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
    val publication = this.loadPublication("epubs/fragments.epub")
    val graph = SR2NavigationGraphs.create(publication)

    assertEquals(7, graph.tableOfContentsFlat.size)
    assertEquals(3, graph.tableOfContents.size)
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

    var target: SR2NavigationTarget? = null
    target = graph.findNavigationNode(SR2LocatorPercent.start("/epub/text/p0.xhtml#p0_01"))
    assertEquals("Chapter 0.1", target!!.node.navigationPoint.title)
    assertEquals(null, target.extraFragment)

    target = graph.findNavigationNode(SR2LocatorPercent.start("/epub/text/p0.xhtml#p0_02"))
    assertEquals("Chapter 0.2", target!!.node.navigationPoint.title)
    assertEquals(null, target.extraFragment)

    target = graph.findNavigationNode(SR2LocatorPercent.start("/epub/text/p1.xhtml#p1_01"))
    assertEquals("Chapter 1.1", target!!.node.navigationPoint.title)
    assertEquals(null, target.extraFragment)

    target = graph.findNavigationNode(SR2LocatorPercent.start("/epub/text/p1.xhtml#p1_02"))
    assertEquals("Chapter 1.2", target!!.node.navigationPoint.title)
    assertEquals(null, target.extraFragment)

    /*
     * Because the fragments in chapter 2 are not advertised in the TOC, the chapter itself
     * will be returned with the fragments as extras.
     */

    target = graph.findNavigationNode(SR2LocatorPercent.start("/epub/text/p2.xhtml#p2_01"))
    assertEquals("Chapter 2", target!!.node.navigationPoint.title)
    assertEquals("p2_01", target.extraFragment)

    target = graph.findNavigationNode(SR2LocatorPercent.start("/epub/text/p2.xhtml#p2_02"))
    assertEquals("Chapter 2", target!!.node.navigationPoint.title)
    assertEquals("p2_02", target.extraFragment)
  }
}
