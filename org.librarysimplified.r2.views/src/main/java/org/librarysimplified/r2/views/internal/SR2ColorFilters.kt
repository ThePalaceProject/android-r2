package org.librarysimplified.r2.views.internal

import android.graphics.ColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter

object SR2ColorFilters {

  val inversionMatrix: ColorMatrix =
    ColorMatrix(
      floatArrayOf(
        -1f, 0f, 0f, 0f, 255f,
        0f, -1f, 0f, 0f, 255f,
        0f, 0f, -1f, 0f, 255f,
        0f, 0f, 0f, 1f, 0f,
      ),
    )

  val inversionFilter: ColorFilter =
    ColorMatrixColorFilter(this.inversionMatrix)
}
