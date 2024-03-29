package org.librarysimplified.r2.demo

import android.content.Context
import org.librarysimplified.r2.api.SR2PageNumberingMode
import org.librarysimplified.r2.api.SR2ScrollingMode
import java.io.File

object DemoModel {

  private lateinit var databaseField: DemoDatabase

  @Volatile
  private var epubIdField: String? = null

  @Volatile
  private var epubFileField: File? = null

  @Volatile
  var scrollMode: SR2ScrollingMode =
    SR2ScrollingMode.SCROLLING_MODE_PAGINATED

  @Volatile
  var perChapterNumbering: SR2PageNumberingMode =
    SR2PageNumberingMode.PER_CHAPTER

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
  }
}
