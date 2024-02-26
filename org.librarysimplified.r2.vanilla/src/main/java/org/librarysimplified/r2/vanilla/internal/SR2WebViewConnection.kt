package org.librarysimplified.r2.vanilla.internal

import android.view.MotionEvent
import android.webkit.WebView
import org.librarysimplified.r2.api.SR2Executors
import org.librarysimplified.r2.api.SR2ScrollingMode
import org.librarysimplified.r2.api.SR2ScrollingMode.SCROLLING_MODE_CONTINUOUS
import org.librarysimplified.r2.api.SR2ScrollingMode.SCROLLING_MODE_PAGINATED
import org.readium.r2.shared.publication.epub.EpubLayout
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * A connection to a web view.
 */

internal class SR2WebViewConnection(
  private val jsAPI: SR2JavascriptAPI,
  private val webView: WebView,
  private val uiExecutor: (f: () -> Unit) -> Unit,
  private val commandQueue: SR2Controller,
) : SR2WebViewConnectionType {

  private val logger =
    LoggerFactory.getLogger(SR2WebViewConnection::class.java)

  companion object {

    fun create(
      webView: WebView,
      jsReceiver: SR2JavascriptAPIReceiverType,
      uiExecutor: (f: () -> Unit) -> Unit,
      commandQueue: SR2Controller,
      scrollingMode: SR2ScrollingMode,
      layout: EpubLayout,
    ): SR2WebViewConnectionType {
      val webChromeClient = SR2WebChromeClient()
      webView.webChromeClient = webChromeClient
      webView.settings.javaScriptEnabled = true
      webView.addJavascriptInterface(jsReceiver, "Android")

      if (layout == EpubLayout.FIXED) {
        with(webView.settings) {
          /*
           * Allow pinch-and-zoom.
           */

          setSupportZoom(true)
          builtInZoomControls = true
          displayZoomControls = false

          /*
           * Allow the web view to obey <meta> viewport elements in the <head> section of
           * documents. This is required for rendering fixed-layout EPUB files.
           *
           * @see "https://www.w3.org/publishing/epub/epub-contentdocs.html#sec-fixed-layouts"
           */

          loadWithOverviewMode = true
          useWideViewPort = true
        }
      } else {
        when (scrollingMode) {
          SCROLLING_MODE_PAGINATED -> {
            /*
             * Disable manual scrolling on the web view. Scrolling is controlled via the javascript API.
             */

            webView.setOnTouchListener { v, event -> event.action == MotionEvent.ACTION_MOVE }
            webView.isVerticalScrollBarEnabled = false
            webView.isHorizontalScrollBarEnabled = false
          }
          SCROLLING_MODE_CONTINUOUS -> {
            webView.isVerticalScrollBarEnabled = true
            webView.isHorizontalScrollBarEnabled = false
          }
        }
      }

      return SR2WebViewConnection(
        jsAPI = SR2JavascriptAPI(webView, commandQueue),
        webView = webView,
        uiExecutor = uiExecutor,
        commandQueue = commandQueue,
      )
    }
  }

  override fun openURL(
    location: String,
  ): CompletableFuture<*> {
    val id =
      UUID.randomUUID()
    val future =
      CompletableFuture<Any>()

    /*
     * Execute a request to the web view on the UI thread, and wait for that request to
     * complete on the background thread we're using for web view connections. Because
     * the background thread executor is a single thread, this has the effect of serializing
     * and queueing requests made to the web view. We can effectively wait on the [Future]
     * that will be set by the web view on completion without having to block the UI thread
     * waiting for the request to complete.
     */

    SR2Executors.ioExecutor.execute {
      this.logger.debug("[{}]: openURL {}", id, location)
      this.uiExecutor.invoke {
        this.webView.webViewClient = SR2WebViewClient(location, future, this.commandQueue)
        this.webView.loadUrl(location)
      }
      this.waitOrFail(id, future)
    }
    return future
  }

  /**
   * Wait up to a minute. If nothing is happening after a minute, something is seriously
   * broken and we should propagate an error via the [Future].
   */

  private fun <T> waitOrFail(
    id: UUID,
    future: CompletableFuture<T>,
  ) {
    try {
      this.logger.debug("[{}]: Waiting for request to complete", id)
      future.get(1L, TimeUnit.MINUTES)
      this.logger.debug("[{}]: Request completed", id)
    } catch (e: TimeoutException) {
      this.logger.error("[{}]: Timed out waiting for the web view to complete: ", id, e)
      future.completeExceptionally(e)
    } catch (e: Throwable) {
      this.logger.error("[{}]: Future failed: ", id, e)
      future.completeExceptionally(e)
    }
  }

  override fun executeJS(
    function: (SR2JavascriptAPIType) -> CompletableFuture<*>,
  ): CompletableFuture<*> {
    val id =
      UUID.randomUUID()
    val future =
      CompletableFuture<Any>()

    /*
     * Execute a request to the web view on the UI thread, and wait for that request to
     * complete on the background thread we're using for web view connections. Because
     * the background thread executor is a single thread, this has the effect of serializing
     * and queueing requests made to the web view. We can effectively wait on the [Future]
     * that will be set by the web view on completion without having to block the UI thread
     * waiting for the request to complete.
     */

    SR2Executors.ioExecutor.execute {
      this.logger.debug("[{}] executeJS", id)

      this.uiExecutor.invoke {
        val jsFuture = function.invoke(this.jsAPI)
        jsFuture.whenComplete { value, exception ->
          if (exception == null) {
            future.complete(value)
          } else {
            future.completeExceptionally(exception)
          }
        }
      }

      this.waitOrFail(id, future)
    }
    return future
  }
}
