package com.rukavina.gymbuddy.ui.theme

import androidx.annotation.DrawableRes
import com.rukavina.gymbuddy.R

/**
 * Own icon set (design/icons.md). Drawables are converted once from
 * design/icons + design/brand with svg2vectordrawable; re-export from
 * Figma and convert again, never hand-edit. IconParityTest keeps the
 * two in step.
 *
 * Tinting: `Icon(painterResource(id), tint = ...)` tints the whole
 * drawable, so Material fills and own strokes both take the tint.
 * Filled tab icons have cut-out detail: draw them with
 * `Color.Unspecified`, never tinted. Never tint any icon lime.
 */
object AppIcons {

    /** Material Symbols Rounded 400 (Apache 2.0). */
    object Ui {
        @DrawableRes val Add = R.drawable.ic_ui_add
        @DrawableRes val Alert = R.drawable.ic_ui_alert
        @DrawableRes val Back = R.drawable.ic_ui_back
        @DrawableRes val Backspace = R.drawable.ic_ui_backspace
        @DrawableRes val Bell = R.drawable.ic_ui_bell
        @DrawableRes val Check = R.drawable.ic_ui_check
        @DrawableRes val ChevronDown = R.drawable.ic_ui_chevron_down
        @DrawableRes val ChevronRight = R.drawable.ic_ui_chevron_right
        @DrawableRes val ChevronUp = R.drawable.ic_ui_chevron_up
        @DrawableRes val Clipboard = R.drawable.ic_ui_clipboard
        @DrawableRes val Close = R.drawable.ic_ui_close
        @DrawableRes val Cloud = R.drawable.ic_ui_cloud
        @DrawableRes val CloudOff = R.drawable.ic_ui_cloud_off
        @DrawableRes val Copy = R.drawable.ic_ui_copy
        @DrawableRes val Delete = R.drawable.ic_ui_delete
        @DrawableRes val Drag = R.drawable.ic_ui_drag
        @DrawableRes val DragDots = R.drawable.ic_ui_drag_dots
        @DrawableRes val Edit = R.drawable.ic_ui_edit
        @DrawableRes val Hide = R.drawable.ic_ui_hide
        @DrawableRes val Info = R.drawable.ic_ui_info
        @DrawableRes val List = R.drawable.ic_ui_list
        @DrawableRes val Logout = R.drawable.ic_ui_logout
        @DrawableRes val More = R.drawable.ic_ui_more
        @DrawableRes val Note = R.drawable.ic_ui_note
        @DrawableRes val Phone = R.drawable.ic_ui_phone
        @DrawableRes val Play = R.drawable.ic_ui_play
        @DrawableRes val Refresh = R.drawable.ic_ui_refresh
        @DrawableRes val Remove = R.drawable.ic_ui_remove
        @DrawableRes val Search = R.drawable.ic_ui_search
        @DrawableRes val Spinner = R.drawable.ic_ui_spinner
        @DrawableRes val Swap = R.drawable.ic_ui_swap
        @DrawableRes val Timer = R.drawable.ic_ui_timer
        @DrawableRes val TrendingUp = R.drawable.ic_ui_trending_up
        @DrawableRes val Undo = R.drawable.ic_ui_undo
        @DrawableRes val Warning = R.drawable.ic_ui_warning
    }

    /** Own gym glyphs, 2dp strokes. */
    object Glyph {
        @DrawableRes val Barbell = R.drawable.ic_glyph_barbell
        @DrawableRes val Bench = R.drawable.ic_glyph_bench
        @DrawableRes val Cable = R.drawable.ic_glyph_cable
        @DrawableRes val Cardio = R.drawable.ic_glyph_cardio
        @DrawableRes val Dumbbell = R.drawable.ic_glyph_dumbbell
        @DrawableRes val JumpRope = R.drawable.ic_glyph_jump_rope
        @DrawableRes val Kettlebell = R.drawable.ic_glyph_kettlebell
        @DrawableRes val Plate = R.drawable.ic_glyph_plate
        @DrawableRes val Pr = R.drawable.ic_glyph_pr
        @DrawableRes val PullUp = R.drawable.ic_glyph_pull_up
        @DrawableRes val Rest = R.drawable.ic_glyph_rest
        @DrawableRes val Scale = R.drawable.ic_glyph_scale
    }

    /** Bottom-bar tabs: outline when inactive, filled (never tinted) when active. */
    enum class Tab(@DrawableRes val outline: Int, @DrawableRes val filled: Int) {
        Home(R.drawable.ic_tab_home_outline, R.drawable.ic_tab_home_filled),
        Workouts(R.drawable.ic_tab_workouts_outline, R.drawable.ic_tab_workouts_filled),
        Exercises(R.drawable.ic_tab_exercises_outline, R.drawable.ic_tab_exercises_filled),
        Progress(R.drawable.ic_tab_progress_outline, R.drawable.ic_tab_progress_filled),
        You(R.drawable.ic_tab_you_outline, R.drawable.ic_tab_you_filled),
    }

    /** Own brand drawing. Scale, never redraw. Wordmark is never tinted (lime dot). */
    object Brand {
        @DrawableRes val Mark = R.drawable.brand_mark
        @DrawableRes val Wordmark = R.drawable.brand_wordmark
    }
}
