package com.rukavina.gymbuddy.ui.exercises.page

import androidx.annotation.DrawableRes
import com.rukavina.gymbuddy.R

/**
 * Images bundled with the app for built-in exercises, by exercise id
 * (Plan 9 step 01b). Made with Google Gemini from the prompts in
 * design/exercise-images/prompts.html; source and licence in
 * design/DESIGN.md §10. Exercises not listed show no image.
 */
internal object ExerciseImages {

    private val byId: Map<String, Int> = mapOf(
        "019fe506-359b-7a6f-82f9-90f196c28c0a" to R.drawable.ex_barbell_back_squat,
        "019fe506-35d1-7779-835a-03fad2a0e63b" to R.drawable.ex_front_squat,
        "019fe506-359c-7fb2-942a-65d8449bbd9a" to R.drawable.ex_conventional_deadlift,
        "019fe506-35d4-7f59-af83-ad2466c8d39c" to R.drawable.ex_sumo_deadlift,
    )

    /** Every exercise id that has an image (for tests). */
    val ids: Set<String> get() = byId.keys

    @DrawableRes
    fun forId(id: String): Int? = byId[id.lowercase()]
}
