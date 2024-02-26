package org.librarysimplified.r2.views.internal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import org.librarysimplified.r2.api.SR2Command
import org.librarysimplified.r2.api.SR2Locator.SR2LocatorPercent
import org.librarysimplified.r2.api.SR2TOCEntry
import org.librarysimplified.r2.ui_thread.SR2UIThread
import org.librarysimplified.r2.views.R
import org.librarysimplified.r2.views.SR2ReaderModel
import org.librarysimplified.r2.views.SR2ReaderViewCommand.SR2ReaderViewNavigationTOCClose

internal class SR2TOCChaptersFragment : Fragment() {

  private lateinit var chaptersError: View
  private lateinit var chapterList: RecyclerView
  private lateinit var chapterAdapter: SR2TOCChapterAdapter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.chapterAdapter =
      SR2TOCChapterAdapter(
        resources = this.resources,
        onTOCEntrySelected = { this.onTOCEntrySelected(it) },
      )
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
    val layout =
      inflater.inflate(R.layout.sr2_toc_chapters, container, false)
    this.chaptersError =
      layout.findViewById(R.id.tocChaptersError)
    this.chapterList =
      layout.findViewById(R.id.tocChaptersList)

    this.chapterList.adapter = this.chapterAdapter
    this.chapterList.setHasFixedSize(true)
    this.chapterList.setItemViewCacheSize(32)
    (this.chapterList.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
    return layout
  }

  override fun onStart() {
    super.onStart()

    val toc = SR2ReaderModel.controller().bookMetadata.tableOfContents
    if (toc.isEmpty()) {
      this.chaptersError.isVisible = true
      this.chapterList.isGone = true
      return
    }

    this.chapterAdapter.setTableOfContentsEntries(toc)
  }

  private fun onTOCEntrySelected(entry: SR2TOCEntry) {
    SR2ReaderModel.controller()
      .submitCommand(
        SR2Command.OpenChapter(
          SR2LocatorPercent(
            chapterHref = entry.href,
            chapterProgress = 0.0,
          ),
        ),
      )

    SR2UIThread.runOnUIThreadDelayed(
      { SR2ReaderModel.submitViewCommand(SR2ReaderViewNavigationTOCClose) },
      SR2TOC.tocSelectionDelay(),
    )
  }
}
