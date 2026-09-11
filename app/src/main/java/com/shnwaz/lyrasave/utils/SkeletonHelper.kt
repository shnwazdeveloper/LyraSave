package com.shnwaz.lyrasave.utils

import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation

object SkeletonHelper {

    fun start(view: View) {
        view.visibility = View.VISIBLE
        val anim = AlphaAnimation(0.35f, 1.0f).apply {
            duration = 750
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
        view.startAnimation(anim)
    }

    fun stop(view: View) {
        view.clearAnimation()
        view.visibility = View.GONE
    }
}
