package com.rukavina.gymbuddy.ui.exercises.page

import androidx.annotation.DrawableRes
import com.rukavina.gymbuddy.R

/**
 * Images bundled with the app for built-in exercises, by exercise id
 * (Plan 9 step 01b). Made with Qwen-Image-2512 (Apache 2.0) in local
 * ComfyUI from design/exercise-images/local-prompts.json; source and
 * licence in design/DESIGN.md §10. Exercises not listed show no image.
 */
internal object ExerciseImages {

    // Gemini set removed 2026-11-22; Qwen-Image-2512 images get added here.
    private val byId: Map<String, Int> = mapOf()

    /** Every exercise id that has an image (for tests). */
    val ids: Set<String> get() = byId.keys

    @DrawableRes
    fun forId(id: String): Int? = byId[id.lowercase()]
}
