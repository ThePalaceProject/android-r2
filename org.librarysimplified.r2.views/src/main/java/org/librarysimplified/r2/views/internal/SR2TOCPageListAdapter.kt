package org.librarysimplified.r2.views.internal

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.librarysimplified.r2.api.SR2PrintPageEntry
import org.librarysimplified.r2.views.R
import org.librarysimplified.r2.views.internal.SR2DiffUtils.printPageEntryCallback
import org.librarysimplified.r2.views.internal.SR2TOCPageListAdapter.SR2PrintPageEntryViewHolder

internal class SR2TOCPageListAdapter(
  private val resources: Resources,
  private val onPageEntrySelected: (SR2PrintPageEntry) -> Unit,
) : ListAdapter<SR2PrintPageEntry, SR2PrintPageEntryViewHolder>(printPageEntryCallback) {
  class SR2PrintPageEntryViewHolder(
    val rootView: View,
  ) : RecyclerView.ViewHolder(rootView) {
    val pageIcon: ImageView =
      this.rootView.findViewById(R.id.pageIcon)
    val pageLabelText: TextView =
      this.rootView.findViewById(R.id.pageLabel)
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int,
  ): SR2PrintPageEntryViewHolder {
    val inflater =
      LayoutInflater.from(parent.context)
    val pageView =
      inflater.inflate(R.layout.sr2_toc_page_item, parent, false)
    return SR2PrintPageEntryViewHolder(pageView)
  }

  override fun onBindViewHolder(
    holder: SR2PrintPageEntryViewHolder,
    position: Int,
  ) {
    val page = this.getItem(position)
    val label = page.label

    holder.rootView.setOnClickListener {
      holder.rootView.setOnClickListener(null)
      this.onPageEntrySelected.invoke(page)
    }

    holder.pageLabelText.text = label
    holder.rootView.contentDescription =
      this.resources.getString(R.string.tocAccessPage, label)
  }

  fun setPrintPageEntries(entriesNow: List<SR2PrintPageEntry>) {
    this.submitList(entriesNow)
  }
}
