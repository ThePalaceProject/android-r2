package org.librarysimplified.r2.demo

import android.content.Context
import org.slf4j.LoggerFactory
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutput
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.HashMap
import java.util.concurrent.Executors

/**
 * This object provides an excessively simple persistent database.
 */

class DemoDatabase(val context: Context) {

  private val logger =
    LoggerFactory.getLogger(DemoDatabase::class.java)

  private val ioExecutor =
    Executors.newSingleThreadExecutor { runnable ->
      val thread = Thread(runnable)
      thread.name = "org.librarysimplified.r2.demo.DemoDatabase[${thread.id}"
      thread
    }

  private val lastRead =
    this.loadMap()

  data class Locator(
    val chapterIndex: Int,
    val progress: Double
  ): Serializable

  fun lastReadingPosition(
    bookId: String
  ): Locator {
    return this.lastRead[bookId] ?: Locator(0, 0.0)
  }

  fun saveReadingPosition(
    bookId: String,
    chapterIndex: Int,
    progress: Double
  ) {
    this.lastRead[bookId] = Locator(chapterIndex, progress)
    this.ioExecutor.execute { this.saveMap() }
  }

  private fun loadMap(): HashMap<String, Locator> {
    return try {
      val file = File(this.context.filesDir, "lastRead.dat")
      file.inputStream().use { stream ->
        ObjectInputStream(stream).use { objectStream ->
          objectStream.readObject() as HashMap<String, Locator>
        }
      }
    } catch (e: Exception) {
      this.logger.error("could not open last read database: ", e)
      HashMap()
    }
  }

  private fun saveMap() {
    try {
      val fileTmp =
        File(this.context.filesDir, "lastRead.dat.tmp")
      val file =
        File(this.context.filesDir, "lastRead.dat")

      fileTmp.outputStream().use { stream ->
        ObjectOutputStream(stream).use { objectStream ->
          objectStream.writeObject(this.lastRead)
        }
      }
      fileTmp.renameTo(file)
    } catch (e: Exception) {
      this.logger.error("could not save last read database: ", e)
    }
  }
}
