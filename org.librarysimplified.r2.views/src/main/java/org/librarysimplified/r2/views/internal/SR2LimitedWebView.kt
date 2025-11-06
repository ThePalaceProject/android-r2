package org.librarysimplified.r2.views.internal

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.webkit.WebView
import org.slf4j.LoggerFactory

class SR2LimitedWebView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
) : WebView(context, attrs) {

  private val logger =
    LoggerFactory.getLogger(SR2LimitedWebView::class.java)

  init {
    this.isFocusable = true
    this.isFocusableInTouchMode = true
    this.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    this.settings.javaScriptEnabled = true
  }

  private var keyboardControlListener: (KeyEvent) -> Unit = { }

  fun setKeyboardControlListener(
    listener: (KeyEvent) -> Unit,
  ) {
    this.keyboardControlListener = listener
  }

  override fun onCreateInputConnection(
    outAttrs: EditorInfo,
  ): InputConnection {
    /*
     * It's necessary to provide a no-op connection to allow IME to work, and to ensure that
     * we don't break various accessibility mechanisms.
     */

    return object : BaseInputConnection(this, false) {
      override fun commitText(
        text: CharSequence?,
        newCursorPosition: Int,
      ): Boolean {
        return true
      }

      override fun sendKeyEvent(event: KeyEvent?): Boolean {
        if (event != null && this@SR2LimitedWebView.handleFilteredKey(event)) {
          return true
        }
        return false
      }
    }
  }

  override fun dispatchKeyEvent(
    event: KeyEvent,
  ): Boolean {
    if (this.handleFilteredKey(event)) {
      return true
    }
    return super.dispatchKeyEvent(event)
  }

  private fun handleFilteredKey(
    event: KeyEvent,
  ): Boolean {
    if (event.action != KeyEvent.ACTION_DOWN) {
      return false
    }

    try {
      this.keyboardControlListener.invoke(event)
    } catch (e: Throwable) {
      this.logger.debug("Keyboard control listener raised an exception: ", e)
    }
    return true
  }
}
