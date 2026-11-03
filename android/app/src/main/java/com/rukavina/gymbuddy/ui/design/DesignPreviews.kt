package com.rukavina.gymbuddy.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme

/** Side-by-side check against Figma "01 System" → Components. */
@Composable
private fun Board(content: @Composable () -> Unit) {
    GymBuddyTheme {
        Column(
            Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { content() }
    }
}

@Preview(widthDp = 412)
@Composable
private fun ButtonsPreview() = Board {
    PrimaryButton("Done · set 4", {}, size = ButtonSize.Large, icon = AppIcons.Ui.Check)
    PrimaryButton("Start", {})
    PrimaryButton("Save", {}, size = ButtonSize.Small)
    PrimaryButton("Start", {}, enabled = false)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SecondaryButton("All sessions", {})
        OutlineButton("Cancel", {}, size = ButtonSize.Small)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppTextButton("Keep editing", {})
        DangerButton("Discard", {})
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButtonStyle.entries.forEach { AppIconButton(AppIcons.Ui.More, it.name, {}, style = it) }
        AppIconButton(AppIcons.Ui.Play, "Play", {}, style = IconButtonStyle.Lime, enabled = false)
    }
}

@Preview(widthDp = 412)
@Composable
private fun SelectionPreview() = Board {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppChip("Weight + reps", selected = false, onClick = {}, dropdown = true)
        AppChip("Weight + reps", selected = true, onClick = {})
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppSwitch(checked = false, onCheckedChange = {})
        AppSwitch(checked = true, onCheckedChange = {})
        AppSwitch(checked = false, onCheckedChange = {}, enabled = false)
    }
    SegmentedButtons(listOf("List", "Calendar"), selectedIndex = 0, onSelect = {})
}

@Preview(widthDp = 412)
@Composable
private fun FieldsPreview() = Board {
    AppTextField("", {}, label = "Email", placeholder = "you@example.com", helper = "Helper text")
    AppTextField("you@", {}, label = "Email", helper = "Enter a full email address", isError = true)
    SearchField("", {}, placeholder = "Search sessions")
    SearchField("Legs", {}, placeholder = "Search sessions")
}

@Preview(widthDp = 412)
@Composable
private fun RowsPreview() = Board {
    ListRow("Bench press", subtitle = "Chest · Barbell", glyph = AppIcons.Glyph.Barbell, onClick = {})
    InfoRow("Up next", "Incline Dumbbell Press", onClick = {})
    TemplateRow("Push A", "6 exercises · yesterday", onClick = {}, onStart = {})
    SessionCard("Legs", "64 min · Squat 110 kg × 5", "Sat 31 Oct", onClick = {}, isPr = true)
}

@Preview(widthDp = 412)
@Composable
private fun FeedbackPreview() = Board {
    Banner(BannerTone.Danger, "Set 4 isn't saved yet", "Phone storage is full.", actionLabel = "Storage settings", onAction = {})
    Banner(BannerTone.Info, "Needs internet", "Signing in needs a connection.")
    EmptyState(AppIcons.Ui.TrendingUp, "No workouts yet", "Finish a workout and it shows here.", actionLabel = "Start", onAction = {})
}

@Preview(widthDp = 412)
@Composable
private fun BarsPreview() = Board {
    LargeTitleTopBar("Push A", subtitle = "41:05") {
        AppIconButton(AppIcons.Ui.Search, "Search", {})
        AppIconButton(AppIcons.Ui.More, "More", {})
    }
    WorkoutTopBar("Push A", "41:05", "3 / 15", onCollapse = {}, onProgress = {}, onMore = {})
    NavTab.entries.forEach { AppNavBar(selected = it, onSelect = {}) }
}
