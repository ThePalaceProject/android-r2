package org.librarysimplified.r2.vanilla.internal

import android.webkit.WebView
import androidx.annotation.UiThread
import com.fasterxml.jackson.databind.ObjectMapper
import org.librarysimplified.r2.api.SR2ColorScheme
import org.librarysimplified.r2.api.SR2Font
import org.librarysimplified.r2.api.SR2PublisherCSS.SR2_PUBLISHER_DEFAULT_CSS_DISABLED
import org.librarysimplified.r2.api.SR2PublisherCSS.SR2_PUBLISHER_DEFAULT_CSS_ENABLED
import org.librarysimplified.r2.api.SR2Theme
import org.librarysimplified.r2.ui_thread.SR2UIThread
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

/**
 * The default implementation of the Javascript API.
 */

internal class SR2JavascriptAPI(
  private val webView: WebView,
) : SR2JavascriptAPIType {
  private val logger =
    LoggerFactory.getLogger(SR2JavascriptAPI::class.java)

  private val mapper =
    ObjectMapper()

  @UiThread
  private fun executeJavascript(script: String): CompletableFuture<String> {
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
  ): CompletableFuture<String> = this.executeJavascript("readium.highlightSearchingTerms(\"$searchingTerms\", $clearHighlight);")

  @UiThread
  override fun openPageNext(): CompletableFuture<*> = this.executeJavascript("api.turnPageRight();")

  @UiThread
  override fun openPagePrevious(): CompletableFuture<*> = this.executeJavascript("api.turnPageLeft();")

  @UiThread
  override fun openPageLast(): CompletableFuture<String> = this.executeJavascript("api.goToPosition(1.0);")

  override fun setSettings(value: SR2Theme): CompletableFuture<*> {
    val o = this.mapper.createObjectNode()
    o.put("colorScheme",
      when (value.colorScheme) {
        SR2ColorScheme.DARK_TEXT_LIGHT_BACKGROUND -> "SR2_BLACK_ON_WHITE"
        SR2ColorScheme.LIGHT_TEXT_DARK_BACKGROUND -> "SR2_WHITE_ON_BLACK"
        SR2ColorScheme.DARK_TEXT_ON_SEPIA -> "SR2_BLACK_ON_SEPIA"
      }
    )
    o.put("font",
      when (value.font) {
        SR2Font.FONT_SANS -> "SR2_FONT_SANS_SERIF"
        SR2Font.FONT_SERIF -> "SR2_FONT_SERIF"
        SR2Font.FONT_OPEN_DYSLEXIC -> "SR2_FONT_OPENDYSLEXIC"
      }
    )
    when (value.publisherCSS) {
      SR2_PUBLISHER_DEFAULT_CSS_ENABLED -> {
        o.put("font", "SR2_FONT_PUBLISHER")
      }

      SR2_PUBLISHER_DEFAULT_CSS_DISABLED -> {
        // Nothing required.
      }
    }
    o.put("fontSizePercent", value.textSize * 100.0)
    return this.executeJavascript("api.putSettings($o);")
  }

  @UiThread
  override fun setProgression(progress: Double): CompletableFuture<String> = this.executeJavascript("api.goToPosition($progress);")

  @UiThread
  override fun scrollToId(id: String): CompletableFuture<*> = this.executeJavascript("api.goToId(\"$id\");")
}
