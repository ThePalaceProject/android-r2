package org.librarysimplified.r2.views.internal

import androidx.recyclerview.widget.DiffUtil
import org.librarysimplified.r2.api.SR2Bookmark

internal object SR2DiffUtils {

  val bookmarkDiffCallback: DiffUtil.ItemCallback<SR2Bookmark> =
    object : DiffUtil.ItemCallback<SR2Bookmark>() {
      override fun areItemsTheSame(
        oldItem: SR2Bookmark,
        newItem: SR2Bookmark
      ): Boolean =
        oldItem == newItem

      override fun areContentsTheSame(
        oldItem: SR2Bookmark,
        newItem: SR2Bookmark
      ): Boolean =
        oldItem == newItem
    }

  val tocEntryCallback: DiffUtil.ItemCallback<SR2TOCChapterItem> =
    object : DiffUtil.ItemCallback<SR2TOCChapterItem>() {
      override fun areItemsTheSame(
        oldItem: SR2TOCChapterItem,
        newItem: SR2TOCChapterItem
      ): Boolean =
        oldItem == newItem

      override fun areContentsTheSame(
        oldItem: SR2TOCChapterItem,
        newItem: SR2TOCChapterItem
      ): Boolean =
        oldItem == newItem
    }
}
