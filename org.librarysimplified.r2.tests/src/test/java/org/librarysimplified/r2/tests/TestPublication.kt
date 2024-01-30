package org.librarysimplified.r2.tests

import android.content.Context
import kotlinx.coroutines.runBlocking
import org.librarysimplified.r2.vanilla.BuildConfig
import org.librarysimplified.r2.vanilla.internal.SR2NoPDFFactory
import org.mockito.Mockito
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.ErrorException
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
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

    val httpClient =
      DefaultHttpClient(userAgent = "${BuildConfig.LIBRARY_PACKAGE_NAME}/${BuildConfig.R2_VERSION_NAME}")
    val assetRetriever =
      AssetRetriever(context.contentResolver, httpClient)

    val publicationParser =
      DefaultPublicationParser(
        context = context,
        httpClient = httpClient,
        assetRetriever = assetRetriever,
        pdfFactory = SR2NoPDFFactory,
      )
    val publicationOpener =
      PublicationOpener(
        publicationParser = publicationParser,
        contentProtections = listOf(),
        onCreatePublication = {
        },
      )

    return runBlocking {
      when (val result = assetRetriever.retrieve(output)) {
        is Try.Failure -> throw IOException("Failed to open EPUB", ErrorException(result.value))
        is Try.Success ->
          when (val pub = publicationOpener.open(
            asset = result.value,
            credentials = null,
            allowUserInteraction = false,
          )) {
            is Try.Failure -> throw IOException("Failed to open EPUB", ErrorException(pub.value))
            is Try.Success -> pub.value
          }
      }
    }
  }
}
