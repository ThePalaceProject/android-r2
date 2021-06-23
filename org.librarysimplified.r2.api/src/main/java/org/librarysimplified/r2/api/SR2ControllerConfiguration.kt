package org.librarysimplified.r2.api

import android.content.Context
import com.google.common.util.concurrent.ListeningExecutorService
import org.readium.r2.shared.publication.asset.PublicationAsset
import org.readium.r2.streamer.Streamer

/**
 * Configuration values for an R2 controller.
 */

data class SR2ControllerConfiguration(

  /**
   * A publication asset containing a book.
   */

  val bookFile: PublicationAsset,

  /**
   * An identifier used to uniquely identify a publication. Unfortunately, identifier are optional
   * in EPUB files. For the sake of consistency, we require an identifier to always be provided.
   */

  val bookId: String,

  /**
   * The initial theme used for the reader.
   */

  val theme: SR2Theme,

  /**
   * The current Android application context.
   */

  val context: Context,

  /**
   * A Readium Streamer to open the book.
   */

  val streamer: Streamer,

  /**
   * An executor service used to execute I/O code on one or more background threads.
   */

  val ioExecutor: ListeningExecutorService,

  /**
   * A function that executes `f` on the Android UI thread.
   */

  val uiExecutor: (f: () -> Unit) -> Unit,

  /**
   * The book scrolling mode.
   */

  val scrollingMode: SR2ScrollingMode
)
