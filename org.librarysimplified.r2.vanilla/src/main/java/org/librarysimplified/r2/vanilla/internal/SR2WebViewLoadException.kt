package org.librarysimplified.r2.vanilla.internal

import java.io.IOException

class SR2WebViewLoadException(
  override val message: String,
  val failures: Map<String, String>
) : IOException(message)
