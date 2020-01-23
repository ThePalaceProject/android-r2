package org.librarysimplified.r2.vanilla

import android.webkit.WebView
import androidx.annotation.UiThread
import org.librarysimplified.r2.api.SR2Command
import org.librarysimplified.r2.api.SR2ControllerCommandQueueType
import org.slf4j.LoggerFactory

/**
 * The default implementation of the Javascript API.
 */

internal class SR2JavascriptAPI(
  private val webView: WebView,
  private val commandQueue: SR2ControllerCommandQueueType
) : SR2JavascriptAPIType {

  private val logger =
    LoggerFactory.getLogger(SR2JavascriptAPI::class.java)

  @UiThread
  override fun openPageNext() {
    UIThread.checkIsUIThread()

    this.webView.evaluateJavascript("scrollRight();") { value ->
      this.logger.debug("scrollRight => {}", value)
      when (value) {
        "\"edge\"" -> {
          this.commandQueue.submitCommand(SR2Command.OpenChapterNext)
        }
        else -> {

        }
      }
    }
  }

  @UiThread
  override fun openPagePrevious() {
    UIThread.checkIsUIThread()

    this.webView.evaluateJavascript("scrollLeft();") { value ->
      this.logger.debug("scrollLeft => {}", value)

      when (value) {
        "\"edge\"" -> {
          this.commandQueue.submitCommand(SR2Command.OpenChapterPrevious(atEnd = true))
        }
        else -> {

        }
      }
    }
  }

  @UiThread
  override fun openPageLast() {
    UIThread.checkIsUIThread()

    this.webView.evaluateJavascript("scrollToEnd();") { value ->
      this.logger.debug("scrollToEnd => {}", value)
    }
  }
}
