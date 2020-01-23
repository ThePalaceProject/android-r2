package org.librarysimplified.r2.vanilla

import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import org.slf4j.LoggerFactory

/**
 * A web chrome client that provides logging.
 */

class SR2WebChromeClient : WebChromeClient() {

  private val logger =
    LoggerFactory.getLogger(SR2WebChromeClient::class.java)

  override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
    this.logger.debug(
      "onConsoleMessage: {}:{}: {}",
      consoleMessage.sourceId(),
      consoleMessage.lineNumber(),
      consoleMessage.message()
    )
    return super.onConsoleMessage(consoleMessage)
  }

  override fun onConsoleMessage(
    message: String,
    lineNumber: Int,
    sourceID: String
  ) {
    this.logger.debug("onConsoleMessage: {}:{}: {}", sourceID, lineNumber, message)
    super.onConsoleMessage(message, lineNumber, sourceID)
  }
}
