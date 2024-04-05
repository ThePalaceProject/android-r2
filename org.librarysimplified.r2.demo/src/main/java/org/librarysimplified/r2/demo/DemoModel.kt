package org.librarysimplified.r2.demo

import android.content.Context
import org.librarysimplified.r2.api.SR2ScrollingMode
import org.slf4j.LoggerFactory
import java.io.File

object DemoModel {

  private val logger =
    LoggerFactory.getLogger(DemoModel::class.java)

  private lateinit var databaseField: DemoDatabase

  @Volatile
  private var epubIdField: String? = null

  @Volatile
  private var epubFileField: File? = null

  @Volatile
  var scrollMode: SR2ScrollingMode =
    SR2ScrollingMode.SCROLLING_MODE_PAGINATED

  val database: DemoDatabase
    get() = this.databaseField

  val epubFile: File?
    get() = this.epubFileField

  val epubId: String?
    get() = this.epubIdField

  fun initialize(context: Context) {
    this.databaseField = DemoDatabase(context.filesDir)
  }

  fun setEpubAndId(data: Pair<File, String>) {
    this.epubFileField = data.first
    this.epubIdField = data.second
    this.logger.debug("setEpubAndId: {}", data)
  }

  fun clearEpubAndId() {
    this.epubFileField = null
    this.epubIdField = null
    this.logger.debug("clearEpubAndId")
  }
}
