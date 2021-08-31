package org.librarysimplified.r2.tests

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.librarysimplified.r2.vanilla.internal.SR2Books
import org.slf4j.LoggerFactory
import java.io.File

class SR2BooksTest {
  private val logger =
    LoggerFactory.getLogger(SR2BooksTest::class.java)

  private lateinit var directory: File

  @BeforeEach
  fun testSetup() {
    this.directory =
      TestDirectories.temporaryDirectory()
  }

  @Test
  fun testNestedTOC() {
    val publication = TestPublication.loadPublication("epubs/nestedtoc.epub", this.directory)
    val metadata = SR2Books.makeMetadata(publication, "id")
    val toc = metadata.tableOfContents

    assertEquals(3, toc.size)
    assertEquals("Something", toc[0].title)
    assertEquals("Something Nested", toc[1].title)
    assertEquals("Something Else Nested", toc[2].title)
  }
}
