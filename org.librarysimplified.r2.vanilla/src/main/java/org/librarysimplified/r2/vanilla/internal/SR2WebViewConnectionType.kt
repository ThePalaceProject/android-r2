package org.librarysimplified.r2.vanilla.internal

import java.util.concurrent.CompletableFuture

/**
 * A connection to a web view.
 */

internal interface SR2WebViewConnectionType {

  fun openURL(
    location: String,
  ): CompletableFuture<*>

  fun executeJS(
    f: (SR2JavascriptAPIType) -> CompletableFuture<*>,
  ): CompletableFuture<*>
}
