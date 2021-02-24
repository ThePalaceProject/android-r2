package org.librarysimplified.r2.vanilla.internal

import android.webkit.WebView
import androidx.annotation.UiThread
import org.librarysimplified.r2.api.SR2Command
import org.librarysimplified.r2.api.SR2ControllerCommandQueueType
import org.librarysimplified.r2.ui_thread.SR2UIThread
import org.librarysimplified.r2.vanilla.internal.SR2ReadiumInternalTheme.DARK
import org.librarysimplified.r2.vanilla.internal.SR2ReadiumInternalTheme.DAY
import org.librarysimplified.r2.vanilla.internal.SR2ReadiumInternalTheme.LIGHT
import org.librarysimplified.r2.vanilla.internal.SR2ReadiumInternalTheme.NIGHT
import org.librarysimplified.r2.vanilla.internal.SR2ReadiumInternalTheme.SEPIA
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
    SR2UIThread.checkIsUIThread()

    this.webView.evaluateJavascript("readium.scrollRight();") {
      this.logger.debug("scrollRight => {}", it)
      when (it) {
        "false" -> {
          this.commandQueue.submitCommand(SR2Command.OpenChapterNext)
        }
        else -> {
        }
      }
    }
  }

  @UiThread
  override fun openPagePrevious() {
    SR2UIThread.checkIsUIThread()

    this.webView.evaluateJavascript("readium.scrollLeft();") {
      this.logger.debug("scrollLeft => {}", it)

      when (it) {
        "false" -> {
          this.commandQueue.submitCommand(SR2Command.OpenChapterPrevious(atEnd = true))
        }
        else -> {
        }
      }
    }
  }

  @UiThread
  override fun openPageLast() {
    SR2UIThread.checkIsUIThread()

    this.webView.evaluateJavascript("readium.scrollToEnd();") {
      this.logger.debug("scrollToEnd => {}", it)
    }
  }

  override fun setFontFamily(value: String) {
    setUserProperty("fontFamily", value)
    setUserProperty("fontOverride", "readium-font-on")
  }

  @UiThread
  override fun setTypeScale(value: Double) {
    setUserProperty("typeScale", "$value")
  }

  override fun setTextAlign(value: String) {
    setUserProperty("textAlign", value)
  }

  override fun setPageMargin(value: Double) {
    // Note: The js property name is 'pageMargins' plural
    setUserProperty("pageMargins", "$value")
  }

  override fun setLineHeight(value: Double) {
    setUserProperty("lineHeight", "$value")
  }

  override fun setLetterSpacing(value: Double) {
    setUserProperty("letterSpacing", "${value}em")
  }

  override fun setWordSpacing(value: Double) {
    setUserProperty("wordSpacing", "${value}rem")
  }

  override fun setTheme(value: SR2ReadiumInternalTheme) {
    when (value) {
      LIGHT, DAY -> {
        setUserProperty("appearance", "readium-default-on")
      }
      DARK, NIGHT -> {
        setUserProperty("appearance", "readium-night-on")
      }
      SEPIA -> {
        setUserProperty("appearance", "readium-sepia-on")
      }
    }
  }

  @UiThread
  override fun setProgression(progress: Double) {
    SR2UIThread.checkIsUIThread()

    this.webView.evaluateJavascript("readium.scrollToPosition($progress);") {
      this.logger.debug("scrollToPosition => {}", it)
    }
  }

  @UiThread
  fun setUserProperty(name: String, value: String) {
    SR2UIThread.checkIsUIThread()

    val script = "readium.setProperty(\"--USER__${name}\", \"${value}\");"
    this.webView.evaluateJavascript(script) {
      this.logger.debug("evaluation result: {}", it)
    }
  }
}
