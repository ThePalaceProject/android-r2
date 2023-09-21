package org.librarysimplified.r2.views.search

import android.text.Html
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.librarysimplified.r2.views.R
import org.readium.r2.shared.publication.Locator

class SR2SearchResultAdapter(private val onItemClicked: (Locator) -> Unit) :
  PagingDataAdapter<Locator, SR2SearchResultAdapter.LocatorViewHolder>(DIFF_ITEM_CALLBACK) {

  companion object {
    private val DIFF_ITEM_CALLBACK = object : DiffUtil.ItemCallback<Locator>() {
      override fun areItemsTheSame(oldItem: Locator, newItem: Locator): Boolean =
        oldItem == newItem

      override fun areContentsTheSame(oldItem: Locator, newItem: Locator): Boolean =
        oldItem == newItem
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocatorViewHolder {
    return LocatorViewHolder(
      LayoutInflater.from(parent.context).inflate(R.layout.sr2_search_result, parent, false),
    )
  }

  override fun onBindViewHolder(holder: LocatorViewHolder, position: Int) {
    val locator = getItem(position) ?: return
    holder.bind(locator)
  }

  inner class LocatorViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private fun highlightResult(locator: Locator): Spanned {
      val html =
        "${locator.text.before}<span style=\"background:yellow;\"><b>${locator.text.highlight}" +
          "</b></span>${locator.text.after}"
      return Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
    }

    fun bind(locator: Locator) {
      (itemView as TextView).apply {
        text = highlightResult(locator)
        setOnClickListener {
          onItemClicked(locator)
        }
      }
    }
  }
}
