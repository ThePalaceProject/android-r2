package org.librarysimplified.r2.vanilla.internal

import android.webkit.WebView
import androidx.annotation.UiThread
import org.librarysimplified.r2.api.SR2Command
import org.librarysimplified.r2.api.SR2ControllerCommandQueueType
import org.librarysimplified.r2.api.SR2PublisherCSS
import org.librarysimplified.r2.api.SR2PublisherCSS.SR2_PUBLISHER_DEFAULT_CSS_DISABLED
import org.librarysimplified.r2.api.SR2PublisherCSS.SR2_PUBLISHER_DEFAULT_CSS_ENABLED
import org.librarysimplified.r2.api.SR2ScrollingMode
import org.librarysimplified.r2.api.SR2ScrollingMode.SCROLLING_MODE_CONTINUOUS
import org.librarysimplified.r2.api.SR2ScrollingMode.SCROLLING_MODE_PAGINATED
import org.librarysimplified.r2.ui_thread.SR2UIThread
import org.librarysimplified.r2.vanilla.internal.SR2ReadiumInternalTheme.DARK
import org.librarysimplified.r2.vanilla.internal.SR2ReadiumInternalTheme.DAY
import org.librarysimplified.r2.vanilla.internal.SR2ReadiumInternalTheme.LIGHT
import org.librarysimplified.r2.vanilla.internal.SR2ReadiumInternalTheme.NIGHT
import org.librarysimplified.r2.vanilla.internal.SR2ReadiumInternalTheme.SEPIA
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

/**
 * The default implementation of the Javascript API.
 */

internal class SR2JavascriptAPI(
  private val webView: WebView,
  private val commandQueue: SR2ControllerCommandQueueType,
) : SR2JavascriptAPIType {

  private val logger =
    LoggerFactory.getLogger(SR2JavascriptAPI::class.java)

  @UiThread
  private fun executeJavascript(
    script: String,
  ): CompletableFuture<String> {
    SR2UIThread.checkIsUIThread()

    val future = CompletableFuture<String>()
    this.logger.debug("evaluating {}", script)
    this.webView.evaluateJavascript(script) {
      try {
        this.logger.debug("evaluated {} ⇒ {}", script, it)
      } finally {
        future.complete(it)
      }
    }
    return future
  }

  @UiThread
  override fun highlightSearchingTerms(
    searchingTerms: String,
    clearHighlight: Boolean,
  ): CompletableFuture<String> {
    return this.executeJavascript("readium.highlightSearchingTerms(\"$searchingTerms\", $clearHighlight);")
  }

  @UiThread
  override fun openPageNext(): CompletableFuture<String> {
    return this.executeJavascript("readium.scrollRight();")
      .thenApply { r: String ->
        return@thenApply when (r) {
          "false" -> {
            this.commandQueue.submitCommand(SR2Command.OpenChapterNext)
            r
          }
          else -> {
            r
          }
        }
      }
  }

  @UiThread
  override fun openPagePrevious(): CompletableFuture<String> {
    return this.executeJavascript("readium.scrollLeft();")
      .thenApply { r: String ->
        return@thenApply when (r) {
          "false" -> {
            this.commandQueue.submitCommand(SR2Command.OpenChapterPrevious(atEnd = true))
            r
          }
          else -> {
            r
          }
        }
      }
  }

  @UiThread
  override fun openPageLast(): CompletableFuture<String> {
    return this.executeJavascript("readium.scrollToEnd();")
  }

  @UiThread
  override fun setFontFamily(value: String): CompletableFuture<*> {
    return CompletableFuture.allOf(
      this.setUserProperty("fontFamily", value),
      this.setUserProperty("fontOverride", "readium-font-on"),
    )
  }

  @UiThread
  override fun setFontSize(value: Double): CompletableFuture<String> {
    val percent = (value * 100.0).toString() + "%"
    return this.setUserProperty("fontSize", percent)
  }

  override fun setTheme(value: SR2ReadiumInternalTheme): CompletableFuture<String> =
    when (value) {
      LIGHT, DAY ->
        this.setUserProperty("appearance", "readium-default-on")

      DARK, NIGHT ->
        this.setUserProperty("appearance", "readium-night-on")

      SEPIA ->
        this.setUserProperty("appearance", "readium-sepia-on")
    }

  @UiThread
  override fun setProgression(progress: Double): CompletableFuture<String> {
    return this.executeJavascript("readium.scrollToPosition($progress);")
  }

  @UiThread
  override fun broadcastReadingPosition(): CompletableFuture<*> {
    return this.executeJavascript("readium.broadcastReadingPosition();")
  }

  @UiThread
  override fun setScrollMode(mode: SR2ScrollingMode): CompletableFuture<*> {
    return this.setUserProperty(
      name = "scroll",
      value = when (mode) {
        SCROLLING_MODE_PAGINATED -> "readium-scroll-off"
        SCROLLING_MODE_CONTINUOUS -> "readium-scroll-on"
      },
    )
  }

  @UiThread
  override fun scrollToId(id: String): CompletableFuture<*> {
    return this.executeJavascript("readium.scrollToId(\"$id\");")
  }

  @UiThread
  override fun setPublisherCSS(
    css: SR2PublisherCSS,
  ): CompletableFuture<*> {
    return when (css) {
      SR2_PUBLISHER_DEFAULT_CSS_ENABLED ->
        CompletableFuture.allOf(
          this.setUserProperty("advancedSettings", ""),
          this.setUserProperty("fontOverride", ""),
        )

      SR2_PUBLISHER_DEFAULT_CSS_DISABLED ->
        CompletableFuture.allOf(
          this.setUserProperty("advancedSettings", "readium-advanced-on"),
          this.setUserProperty("fontOverride", "readium-font-on"),
        )
    }
  }

  @UiThread
  fun setUserProperty(
    name: String,
    value: String,
  ): CompletableFuture<String> {
    return this.executeJavascript("readium.setProperty(\"--USER__${name}\", \"${value}\");")
  }
}
