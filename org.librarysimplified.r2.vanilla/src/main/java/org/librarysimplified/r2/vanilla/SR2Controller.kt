package org.librarysimplified.r2.vanilla

import android.webkit.WebView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.librarysimplified.r2.api.SR2Command
import org.librarysimplified.r2.api.SR2ControllerConfiguration
import org.librarysimplified.r2.api.SR2ControllerType
import org.librarysimplified.r2.api.SR2Event
import org.readium.r2.shared.Publication
import org.readium.r2.streamer.container.Container
import org.readium.r2.streamer.parser.EpubParser
import org.readium.r2.streamer.server.Server
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.concurrent.GuardedBy

/**
 * The default R2 controller implementation.
 */

class SR2Controller private constructor(
  private val port: Int,
  private val server: Server,
  private val publication: Publication,
  private val container: Container,
  private val epubFileName: String
) : SR2ControllerType {

  companion object {

    private val logger =
      LoggerFactory.getLogger(SR2Controller::class.java)

    /**
     * Find a high-numbered port upon which to run the internal server. Tries up to ten
     * times to find a port and then gives up with an exception if it can't.
     */

    private fun fetchUnusedHTTPPort(): Int {
      for (i in 0 until 10) {
        try {
          val socket = ServerSocket(0)
          val port = socket.localPort
          socket.close()
          return port
        } catch (e: IOException) {
          this.logger.error("failed to open port: ", e)
        }

        try {
          Thread.sleep(1_000L)
        } catch (e: InterruptedException) {
          Thread.currentThread().interrupt()
        }
      }

      throw IOException("Unable to find an unused port for the server")
    }

    /**
     * Create a new controller based on the given configuration.
     */

    fun create(
      configuration: SR2ControllerConfiguration
    ): SR2ControllerType {
      val bookFile = configuration.bookFile
      this.logger.debug("creating controller for {}", bookFile)

      val box =
        EpubParser().parse(bookFile.absolutePath)
          ?: throw IOException("Failed to parse EPUB")

      this.logger.debug("publication uri: {}", box.publication.baseUrl())
      this.logger.debug("publication title: {}", box.publication.metadata.title)
      val port = this.fetchUnusedHTTPPort()
      this.logger.debug("server port: {}", port)

      val server = Server(port)
      this.logger.debug("starting server")
      server.start(5_000)

      this.logger.debug("loading readium resources")
      server.loadReadiumCSSResources(configuration.context.assets)
      server.loadR2ScriptResources(configuration.context.assets)
      server.loadR2FontResources(configuration.context.assets, configuration.context)

      this.logger.debug("loading epub into server")
      val epubName = "/${bookFile.name}"
      server.addEpub(
        publication = box.publication,
        container = box.container,
        fileName = epubName,
        userPropertiesPath = null
      )

      this.logger.debug("server ready")
      return SR2Controller(
        port = port,
        server = server,
        epubFileName = epubName,
        publication = box.publication,
        container = box.container
      )
    }
  }

  private val logger = LoggerFactory.getLogger(SR2Controller::class.java)
  private val closed = AtomicBoolean(false)

  private val webViewLock = Any()
  @GuardedBy("webViewLock")
  private var webView: WebViewReference? = null

  private data class WebViewReference(
    val webView: WebView
  )

  private val eventSubject: PublishSubject<SR2Event> =
    PublishSubject.create()

  override val events: Observable<SR2Event> =
    this.eventSubject

  override fun execute(command: SR2Command) {
    TODO()
  }

  override fun viewConnect(webView: WebView) {
    synchronized(this.webViewLock) {
      this.webView = WebViewReference(webView)
    }
  }

  override fun viewDisconnect() {
    synchronized(this.webViewLock) {
      this.webView = null
    }
  }

  override fun close() {
    if (this.closed.compareAndSet(false, true)) {
      try {
        this.viewDisconnect()
      } catch (e: Exception) {
        this.logger.error("could not disconnect view: ", e)
      }

      try {
        this.server.closeAllConnections()
      } catch (e: Exception) {
        this.logger.error("could not close connections: ", e)
      }

      try {
        this.server.stop()
      } catch (e: Exception) {
        this.logger.error("could not stop server: ", e)
      }

      try {
        this.eventSubject.onComplete()
      } catch (e: Exception) {
        this.logger.error("could not complete event stream: ", e)
      }
    }
  }
}
