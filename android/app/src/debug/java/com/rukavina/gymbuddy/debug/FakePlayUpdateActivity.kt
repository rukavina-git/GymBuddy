package com.rukavina.gymbuddy.debug

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView

/**
 * Debug only: stands in for Play's full-screen update so the start/exit
 * transitions can be checked without a Play install. Back = the user
 * backs out (RESULT_CANCELED), like on Play.
 */
class FakePlayUpdateActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        setContentView(
            TextView(this).apply {
                setText(com.rukavina.gymbuddy.R.string.debug_fake_update)
                textSize = 20f
                gravity = Gravity.CENTER
                setTextColor(Color.BLACK)
                setBackgroundColor(Color.WHITE)
            },
        )
    }
}
