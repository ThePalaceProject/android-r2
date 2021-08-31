package org.librarysimplified.r2.tests

import android.content.Context
import kotlinx.coroutines.runBlocking
import org.mockito.Mockito
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.asset.FileAsset
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.streamer.Streamer
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

object TestPublication {

  fun loadPublication(name: String, directory: File): Publication {
    val path = "/org/librarysimplified/r2/tests/$name"
    val url = SR2NavigationGraphsTest::class.java.getResource(path)
      ?: throw FileNotFoundException(path)

    val output = File(directory, "output.epub")
    url.openStream().use { inputStream ->
      output.outputStream().use { outputStream ->
        inputStream.copyTo(outputStream)
      }
    }

    val context =
      Mockito.mock(Context::class.java)
    Mockito.`when`(context.applicationContext)
      .thenReturn(context)

    val streamer = Streamer(context)
    return runBlocking {
      streamer.open(FileAsset(output), allowUserInteraction = false)
    }.getOrElse {
      throw IOException("Failed to open EPUB", it)
    }
  }
}
