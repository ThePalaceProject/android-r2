package org.librarysimplified.r2.demo

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import androidx.core.net.toUri
import org.librarysimplified.r2.api.SR2ScrollingMode
import org.slf4j.LoggerFactory
import java.io.File

object DemoModel {

  @Volatile
  var allowCopyPaste: Boolean = true

  private val logger =
    LoggerFactory.getLogger(DemoModel::class.java)

  private val epubURL =
    "https://ataxia.io7m.com/2026/04/30/jack-black_you-cant-win.epub"

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

  fun downloadEpub() {
    val context =
      DemoApplication.application

    val request = DownloadManager.Request(epubURL.toUri())
      .setTitle("Downloading 'You can't Win'")
      .setDescription("In progress...")
      .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
      .setDestinationInExternalPublicDir(
        Environment.DIRECTORY_DOCUMENTS,
        "jack-black_you-cant-win.epub",
      )

    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    dm.enqueue(request)
  }
}
