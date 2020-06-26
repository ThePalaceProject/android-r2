package org.librarysimplified.r2.demo

import android.content.Context
import org.joda.time.DateTime
import org.librarysimplified.r2.api.SR2Bookmark
import org.librarysimplified.r2.api.SR2Bookmark.Type
import org.librarysimplified.r2.api.SR2Bookmark.Type.EXPLICIT
import org.librarysimplified.r2.api.SR2Bookmark.Type.LAST_READ
import org.librarysimplified.r2.api.SR2Locator.SR2LocatorChapterEnd
import org.librarysimplified.r2.api.SR2Locator.SR2LocatorPercent
import org.slf4j.LoggerFactory
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.HashMap
import java.util.concurrent.Executors

/**
 * This object provides an excessively simple persistent database.
 */

class DemoDatabase(private val context: Context) {

  private val logger =
    LoggerFactory.getLogger(DemoDatabase::class.java)

  private val ioExecutor =
    Executors.newSingleThreadExecutor { runnable ->
      val thread = Thread(runnable)
      thread.name = "org.librarysimplified.r2.demo.DemoDatabase[${thread.id}"
      thread
    }

  private val bookmarks: HashMap<String, List<SerializableBookmark>> =
    this.loadMap()

  data class SerializableBookmark(
    val time: DateTime,
    val type: Type,
    val title: String,
    val chapterIndex: Int,
    val progress: Double
  ) : Serializable

  private fun countBookmarks(map: Map<String, List<SerializableBookmark>>): Int {
    return map.entries.fold(0, { acc : Int, entry -> acc + entry.value.size })
  }

  fun bookmarkFindLastReadLocation(
    bookId: String
  ): SR2Bookmark {
    val bookmarks = this.bookmarksFor(bookId)
    return bookmarks.find { bookmark -> bookmark.type == LAST_READ }
      ?: SR2Bookmark(
        date = DateTime.now(),
        type = LAST_READ,
        title = "",
        locator = SR2LocatorPercent(0, 0.0)
      )
  }

  fun bookmarksFor(bookId: String): List<SR2Bookmark> {
    return (this.bookmarks[bookId] ?: listOf())
      .map { bookmark ->
        SR2Bookmark(
          date = bookmark.time,
          type = bookmark.type,
          title = bookmark.title,
          locator = SR2LocatorPercent(bookmark.chapterIndex, bookmark.progress)
        )
      }
  }

  fun bookmarkDelete(
    bookId: String,
    bookmark: SR2Bookmark
  ) {
    if (bookmark.type == LAST_READ) {
      return
    }

    val existing = this.bookmarks[bookId]?.toMutableList() ?: mutableListOf()
    existing.remove(toSerializable(bookmark))
    this.bookmarks[bookId] = existing.toList()
    this.ioExecutor.execute { this.saveMap() }
  }

  fun bookmarkSave(
    bookId: String,
    bookmark: SR2Bookmark
  ) {
    val existing = this.bookmarks[bookId]?.toMutableList() ?: mutableListOf()

    when (bookmark.type) {
      EXPLICIT -> {

      }
      LAST_READ -> {
        existing.removeAll { it.type == LAST_READ }
      }
    }

    existing.add(toSerializable(bookmark))
    this.bookmarks[bookId] = existing.toList()
    this.ioExecutor.execute { this.saveMap() }
  }

  private fun toSerializable(
    bookmark: SR2Bookmark
  ): SerializableBookmark {
    return when (val locator = bookmark.locator) {
      is SR2LocatorPercent -> {
        SerializableBookmark(
          time = bookmark.date,
          type = bookmark.type,
          title = bookmark.title,
          chapterIndex = bookmark.locator.chapterIndex,
          progress = locator.chapterProgress
        )
      }
      is SR2LocatorChapterEnd -> {
        SerializableBookmark(
          time = bookmark.date,
          type = bookmark.type,
          title = bookmark.title,
          chapterIndex = bookmark.locator.chapterIndex,
          progress = 1.0
        )
      }
    }
  }

  private fun loadMap(): HashMap<String, List<SerializableBookmark>> {
    return try {
      this.logger.debug("loading bookmarks")

      val file = File(this.context.filesDir, "bookmarks.dat")
      file.inputStream().use { stream ->
        ObjectInputStream(stream).use { objectStream ->
          val map = objectStream.readObject() as HashMap<String, List<SerializableBookmark>>
          this.logger.debug("loaded {} bookmarks", this.countBookmarks(map))
          logBookmarks(map)
          map
        }
      }
    } catch (e: Exception) {
      this.logger.error("could not open bookmarks database: ", e)
      HashMap()
    }
  }

  private fun logBookmarks(map: HashMap<String, List<SerializableBookmark>>) {
    for (entry in map.entries) {
      for (bookmark in entry.value) {
        this.logger.debug("[{}][{}]: bookmark", entry.key, bookmark)
      }
    }
  }

  private fun saveMap() {
    try {
      this.logger.debug("saving {} bookmarks", this.countBookmarks(this.bookmarks))

      val fileTmp =
        File(this.context.filesDir, "bookmarks.dat.tmp")
      val file =
        File(this.context.filesDir, "bookmarks.dat")

      fileTmp.outputStream().use { stream ->
        ObjectOutputStream(stream).use { objectStream ->
          objectStream.writeObject(this.bookmarks)
        }
      }
      fileTmp.renameTo(file)
    } catch (e: Exception) {
      this.logger.error("could not save bookmarks database: ", e)
    }
  }
}
