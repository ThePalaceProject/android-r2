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

    this.webView.evaluateJavascript("scrollRight();") {
      this.logger.debug("scrollRight => {}", it)
      when (it) {
        "\"edge\"" -> {
          this.commandQueue.submitCommand(SR2Command.OpenChapterNext)
        }
        else -> {}
      }
    }
  }

  @UiThread
  override fun openPagePrevious() {
    UIThread.checkIsUIThread()

    this.webView.evaluateJavascript("scrollLeft();") {
      this.logger.debug("scrollLeft => {}", it)

      when (it) {
        "\"edge\"" -> {
          this.commandQueue.submitCommand(SR2Command.OpenChapterPrevious(atEnd = true))
        }
        else -> {}
      }
    }
  }

  @UiThread
  override fun openPageLast() {
    UIThread.checkIsUIThread()

    this.webView.evaluateJavascript("scrollToEnd();") {
      this.logger.debug("scrollToEnd => {}", it)
    }
  }

  override fun setFontFamily(value: String) {
    setUserProperty("fontFamily", value)
    setUserProperty("fontOverride", "readium-font-on")
  }

  override fun setTextSize(value: Int) {
    // Note: The js property name is 'fontSize' not 'textSize'
    setUserProperty("fontSize", "$value%")
  }

  override fun setTextAlign(value: String) {
    setUserProperty("textAlign", value)
  }

  @UiThread
  fun setUserProperty(name: String, value: String) {
    UIThread.checkIsUIThread()

    val script = "setProperty(\"--USER__${name}\", \"${value}\");"
    this.webView.evaluateJavascript(script) {
      this.logger.debug("evaluation result: {}", it)
    }
  }
}
