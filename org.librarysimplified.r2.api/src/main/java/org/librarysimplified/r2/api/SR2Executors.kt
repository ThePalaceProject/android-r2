package org.librarysimplified.r2.api

import org.slf4j.LoggerFactory
import java.util.concurrent.Executor
import java.util.concurrent.Executors

object SR2Executors {

  private val logger =
    LoggerFactory.getLogger(SR2Executors::class.java)

  val ioExecutor: Executor =
    Executors.newSingleThreadExecutor { r ->
      val thread = Thread(r)
      thread.name = "org.librarysimplified.r2.io"
      thread.setUncaughtExceptionHandler { t, e ->
        logger.error("Uncaught exception: ", e)
      }
      thread
    }
}
