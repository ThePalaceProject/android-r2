package org.librarysimplified.r2.vanilla.internal

import java.io.IOException

class SR2WebViewDisconnectedException(
  override val message: String
) : IOException(message)
