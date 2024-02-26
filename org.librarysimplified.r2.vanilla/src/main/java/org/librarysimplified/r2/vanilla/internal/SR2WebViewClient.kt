package org.librarysimplified.r2.vanilla.internal

import android.os.Build
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import org.librarysimplified.r2.api.SR2Command
import org.librarysimplified.r2.vanilla.internal.SR2Controller.Companion.PREFIX_ASSETS
import org.librarysimplified.r2.vanilla.internal.SR2Controller.Companion.PREFIX_PUBLICATION
import org.slf4j.LoggerFactory
import java.net.URLDecoder
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * A web view client that provides logging of errors and also allows for the execution of
 * functions when pages have loaded. A client handles a single request; there's a 1:1 correspondence
 * between requests the user makes to open chapters and URLs, and clients.
 */

internal class SR2WebViewClient(
  private val requestLocation: String,
  private val future: CompletableFuture<Any>,
  private val commandQueue: SR2Controller,
) : WebViewClient() {

  companion object {
    private val faviconData: ByteArray =
      SR2WebViewClient::class.java.getResourceAsStream("/org/librarysimplified/r2/vanilla/favicon.ico")
        ?.readBytes()
        ?: throw IllegalStateException("Missing favicon resource")
  }

  private val logger =
    LoggerFactory.getLogger(SR2WebViewClient::class.java)
  private val errors =
    ConcurrentHashMap<String, String>()

  override fun shouldOverrideUrlLoading(
    view: WebView,
    url: String,
  ): Boolean {
    this.logger.debug("shouldOverrideUrlLoading: {}", url)
    this.commandQueue.submitCommand(SR2Command.OpenLink(url))
    return true
  }

  override fun shouldOverrideUrlLoading(
    view: WebView,
    request: WebResourceRequest,
  ): Boolean {
    this.logger.debug("shouldOverrideUrlLoading: {}", request.url)
    this.commandQueue.submitCommand(SR2Command.OpenLink(request.url.toString()))
    return true
  }

  override fun shouldInterceptRequest(
    view: WebView?,
    request: WebResourceRequest?,
  ): WebResourceResponse? {
    if (request != null) {
      val url = request.url?.toString() ?: ""

      if (url.startsWith(PREFIX_ASSETS)) {
        return this.commandQueue.openAsset(url.removePrefix(PREFIX_ASSETS))
      }

      if (url.startsWith(PREFIX_PUBLICATION)) {
        return this.commandQueue.openPublicationResource(url.removePrefix(PREFIX_PUBLICATION))
      }

      if (url.endsWith("favicon.ico")) {
        return WebResourceResponse(
          "image/vnd.microsoft.icon",
          null,
          200,
          "OK",
          null,
          faviconData.inputStream(),
        )
      }
    }
    return super.shouldInterceptRequest(view, request)
  }

  override fun onLoadResource(
    view: WebView,
    url: String,
  ) {
    this.logger.debug("onLoadResource: {}", url)
    super.onLoadResource(view, url)
  }

  override fun onPageFinished(
    view: WebView,
    url: String,
  ) {
    this.logger.debug("onPageFinished: {}", url)

    if (this.isStartingURL(url)) {
      try {
        if (this.errors.isEmpty()) {
          this.logger.debug("onPageFinished: {} succeeded", url)
          this.commandQueue.submitCommand(SR2Command.HighlightCurrentTerms)
          this.future.complete(Unit)
          return
        } else {
          this.logger.error("onPageFinished: {} failed with {} errors", url, this.errors.size)
          this.future.completeExceptionally(
            SR2WebViewLoadException("Failed to load $requestLocation", this.errors.toMap()),
          )
          return
        }
      } finally {
        this.logger.debug("onPageFinished: completed future")
      }
    }

    super.onPageFinished(view, url)
  }

  /**
   * Check to see if the given URL matches the initial request location. We first do a
   * straightforward comparison, and if the comparison fails, we then do a comparison
   * with URL-decoded contents. Most of the time, the first comparison will succeed, but
   * the URL-decoded comparison may be required for URLs that contain spaces. Some EPUB
   * files contain filenames with spaces, despite the recommendations of both the epubcheck
   * tool and the EPUB standard.
   */

  private fun isStartingURL(url: String): Boolean {
    if (url == this.requestLocation) {
      return true
    }

    val decoded = URLDecoder.decode(url, "UTF-8")
    return decoded == this.requestLocation
  }

  override fun onReceivedError(
    view: WebView,
    request: WebResourceRequest,
    error: WebResourceError,
  ) {
    if (Build.VERSION.SDK_INT >= 23) {
      this.logger.error(
        "onReceivedError: {}: {} {}",
        request.url,
        error.errorCode,
        error.description,
      )
    }

    this.errors[request.url.toString()] = "${error.errorCode} ${error.description}"
    super.onReceivedError(view, request, error)
  }

  override fun onReceivedHttpError(
    view: WebView,
    request: WebResourceRequest,
    errorResponse: WebResourceResponse,
  ) {
    this.logger.error(
      "onReceivedHttpError: {}: {} {}",
      request.url,
      errorResponse.statusCode,
      errorResponse.reasonPhrase,
    )

    this.errors[request.url.toString()] = "${errorResponse.statusCode} ${errorResponse.reasonPhrase}"
    super.onReceivedHttpError(view, request, errorResponse)
  }

  override fun onReceivedError(
    view: WebView,
    errorCode: Int,
    description: String,
    failingUrl: String,
  ) {
    this.logger.error(
      "onReceivedError: {}: {} {}",
      failingUrl,
      errorCode,
      description,
    )

    this.errors[failingUrl] = "$errorCode $description"
    super.onReceivedError(view, errorCode, description, failingUrl)
  }
}
