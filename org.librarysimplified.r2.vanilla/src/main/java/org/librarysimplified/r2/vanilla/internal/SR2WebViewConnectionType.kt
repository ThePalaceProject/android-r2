package org.librarysimplified.r2.vanilla.internal

import com.google.common.util.concurrent.ListenableFuture
import java.io.Closeable

/**
 * A connection to a web view.
 */

internal interface SR2WebViewConnectionType : Closeable {

  fun openURL(
    location: String
  ): ListenableFuture<*>

  fun executeJS(
    f: (SR2JavascriptAPIType) -> ListenableFuture<*>
  ): ListenableFuture<Any>
}
