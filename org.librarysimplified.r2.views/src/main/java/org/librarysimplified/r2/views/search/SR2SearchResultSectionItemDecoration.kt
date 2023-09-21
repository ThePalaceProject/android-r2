package org.librarysimplified.r2.views.search

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import org.librarysimplified.r2.views.R

class SR2SearchResultSectionItemDecoration(
  private val context: Context,
  private val sectionListener: SR2SearchResultSectionListener,
) : RecyclerView.ItemDecoration() {

  private lateinit var headerTextView: TextView

  override fun getItemOffsets(
    outRect: Rect,
    view: View,
    parent: RecyclerView,
    state: RecyclerView.State,
  ) {
    super.getItemOffsets(outRect, view, parent, state)
    val index = parent.getChildAdapterPosition(view)
    initHeaderViewIfNeeded(parent)

    val sectionTitle = sectionListener.sectionTitle(index)

    if (sectionTitle.isNotBlank() &&
      sectionListener.isStartOfSection(index)
    ) {
      headerTextView.text = sectionTitle
      fixLayoutSize(headerTextView, parent)
      outRect.top = headerTextView.height
    }
  }

  override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
    super.onDrawOver(c, parent, state)
    initHeaderViewIfNeeded(parent)

    val children = parent.children.toList()
    children.forEach { child ->
      val index = parent.getChildAdapterPosition(child)

      val sectionTitle = sectionListener.sectionTitle(index)
      if (index != NO_POSITION && sectionTitle.isNotBlank() &&
        (sectionListener.isStartOfSection(index) || isTopChild(child, children))
      ) {
        headerTextView.text = sectionTitle
        fixLayoutSize(headerTextView, parent)
        drawHeader(c, child, headerTextView)
      }
    }
  }

  private fun initHeaderViewIfNeeded(parent: RecyclerView) {
    if (::headerTextView.isInitialized) {
      return
    }

    headerTextView =
      LayoutInflater.from(context).inflate(R.layout.sr2_search_result_header, parent, false)
        as TextView
  }

  private fun fixLayoutSize(view: View, parent: ViewGroup) {
    val widthSpec = View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.EXACTLY)
    val heightSpec = View.MeasureSpec.makeMeasureSpec(parent.height, View.MeasureSpec.UNSPECIFIED)
    val childWidth =
      ViewGroup.getChildMeasureSpec(
        widthSpec,
        parent.paddingStart + parent.paddingEnd,
        view.layoutParams.width,
      )
    val childHeight =
      ViewGroup.getChildMeasureSpec(
        heightSpec,
        parent.paddingTop + parent.paddingBottom,
        view.layoutParams.height,
      )
    view.measure(childWidth, childHeight)
    view.layout(0, 0, view.measuredWidth, view.measuredHeight)
  }

  private fun drawHeader(canvas: Canvas, child: View, headerView: View) {
    canvas.run {
      save()
      translate(0F, maxOf(0, child.top - headerView.height).toFloat())
      headerView.draw(this)
      restore()
    }
  }

  private fun isTopChild(child: View, children: List<View>): Boolean {
    val minimumChildTop = children.minOf { it.top }
    return child.top == minimumChildTop
  }
}
