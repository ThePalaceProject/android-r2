package org.librarysimplified.r2.views

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.joda.time.format.DateTimeFormatterBuilder
import org.librarysimplified.r2.api.SR2Bookmark
import org.librarysimplified.r2.api.SR2Locator
import org.librarysimplified.r2.views.SR2Adapters.bookmarkDiffCallback
import org.librarysimplified.r2.views.SR2TOCBookmarkAdapter.SR2TOCBookmarkViewHolder

class SR2TOCBookmarkAdapter(
  private val resources: Resources,
  private val onBookmarkSelected: (SR2Bookmark) -> Unit
) : ListAdapter<SR2Bookmark, SR2TOCBookmarkViewHolder>(bookmarkDiffCallback) {

  private val formatter =
    DateTimeFormatterBuilder()
      .appendYear(4, 5)
      .appendLiteral('-')
      .appendMonthOfYear(2)
      .appendLiteral('-')
      .appendDayOfMonth(2)
      .appendLiteral(' ')
      .appendHourOfDay(2)
      .appendLiteral(':')
      .appendMinuteOfDay(2)
      .appendLiteral(':')
      .appendSecondOfMinute(2)
      .toFormatter()

  class SR2TOCBookmarkViewHolder(
    val rootView: View
  ) : RecyclerView.ViewHolder(rootView) {
    val bookmarkIcon: ImageView =
      rootView.findViewById(R.id.bookmarkIcon)
    val bookmarkDate: TextView =
      rootView.findViewById(R.id.bookmarkDate)
    val bookmarkProgressText: TextView =
      rootView.findViewById(R.id.bookmarkProgressText)
    val bookmarkTitleText: TextView =
      rootView.findViewById(R.id.bookmarkTitle)
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): SR2TOCBookmarkViewHolder {
    val inflater =
      LayoutInflater.from(parent.context)
    val bookmarkView =
      inflater.inflate(R.layout.sr2_toc_bookmark_item, parent, false)
    return SR2TOCBookmarkViewHolder(bookmarkView)
  }

  override fun onBindViewHolder(
    holder: SR2TOCBookmarkViewHolder,
    position: Int
  ) {
    val bookmark = this.getItem(position)
    holder.rootView.setOnClickListener {
      holder.rootView.setOnClickListener(null)
      this.onBookmarkSelected.invoke(bookmark)
    }
    holder.bookmarkTitleText.text = bookmark.title
    holder.bookmarkDate.text = this.formatter.print(bookmark.date)
    holder.bookmarkProgressText.text =
      when (val locator = bookmark.locator) {
        is SR2Locator.SR2LocatorPercent -> {
          val percent = (locator.chapterProgress * 100.0).toInt()
          this.resources.getString(R.string.bookmarkProgressPercent, percent)
        }
        is SR2Locator.SR2LocatorChapterEnd ->
          this.resources.getString(R.string.bookmarkEnd)
      }
  }

  fun setBookmarks(bookmarksNow: List<SR2Bookmark>) {
    this.submitList(bookmarksNow)
  }
}
