package org.librarysimplified.r2.views.internal

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable

object SR2Ripples {

  fun createRippleDrawableForLightBackground(): RippleDrawable {
    val rippleColorDark =
      Color.parseColor("#33000000")

    return RippleDrawable(
      ColorStateList.valueOf(rippleColorDark),
      null,
      ColorDrawable(Color.WHITE),
    )
  }

  fun createRippleDrawableForDarkBackground(): RippleDrawable {
    val rippleColorLight =
      Color.parseColor("#33FFFFFF")

    return RippleDrawable(
      ColorStateList.valueOf(rippleColorLight),
      null,
      ColorDrawable(Color.WHITE),
    )
  }
}
