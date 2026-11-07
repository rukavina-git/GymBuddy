package com.rukavina.gymbuddy.ui.you.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.domain.model.ActivityLevel
import com.rukavina.gymbuddy.domain.model.ExperienceLevel
import com.rukavina.gymbuddy.domain.model.FitnessGoal
import com.rukavina.gymbuddy.domain.model.Gender
import com.rukavina.gymbuddy.ui.design.AppBottomSheet
import com.rukavina.gymbuddy.ui.design.AppLinkButton
import com.rukavina.gymbuddy.ui.design.AppTextField
import com.rukavina.gymbuddy.ui.design.BackTopBar
import com.rukavina.gymbuddy.ui.design.PrimaryButton
import com.rukavina.gymbuddy.ui.progress.weightText
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.FullShape
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import com.rukavina.gymbuddy.ui.you.BodyWheel
import com.rukavina.gymbuddy.ui.you.CmWheel
import com.rukavina.gymbuddy.ui.you.FeetInchesWheels
import com.rukavina.gymbuddy.ui.you.WeightWheels
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import kotlin.math.roundToInt
import com.rukavina.gymbuddy.ui.you.Avatar
import com.rukavina.gymbuddy.ui.you.Avatars
import com.rukavina.gymbuddy.ui.you.Choice
import com.rukavina.gymbuddy.ui.you.ChoiceSheet
import com.rukavina.gymbuddy.ui.you.Group
import com.rukavina.gymbuddy.ui.you.RowDivider
import com.rukavina.gymbuddy.ui.you.ValueRow
import com.rukavina.gymbuddy.utils.UnitConverter
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions

/** Raw sizes from R15-3/R15-4 (DESIGN.md §4): page avatar, edit badge, picker cell check. */
private val PageAvatar = 96.dp
private val Badge = 32.dp

internal enum class ProfileSheet { Picture, Experience, Height, BirthDate, Gender, Activity, Goal, TargetWeight }

/** Profile (R15-3). Name edits inline (F3b); the other rows open a picker sheet. */
@Composable
fun ProfileScreen(onBack: () -> Unit, onBodyWeight: () -> Unit, viewModel: ProfileViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var sheet by rememberSaveable { mutableStateOf<ProfileSheet?>(null) }
    ProfileContent(state, onBack, onBodyWeight, onOpen = { sheet = it }, onName = viewModel::setName)
    val close = { sheet = null }
    when (sheet) {
        ProfileSheet.Picture -> PictureSheet(state.avatarId, { viewModel.setAvatar(it); close() }, close)
        ProfileSheet.Experience -> ChoiceSheet(
            stringResource(R.string.profile_experience),
            ExperienceLevel.entries.map { Choice<ExperienceLevel?>(it, experienceLabel(it), experienceSub(it)) },
            state.experience,
            { viewModel.setExperience(it); close() },
            close,
        )
        ProfileSheet.Gender -> ChoiceSheet(
            stringResource(R.string.profile_gender),
            notSet<Gender>() + Gender.entries.map { Choice(it, genderLabel(it)) },
            state.gender,
            { viewModel.setGender(it); close() },
            close,
        )
        ProfileSheet.Activity -> ChoiceSheet(
            stringResource(R.string.profile_activity),
            notSet<ActivityLevel>() + ActivityLevel.entries.map { Choice(it, activityLabel(it), activitySub(it)) },
            state.activity,
            { viewModel.setActivity(it); close() },
            close,
        )
        ProfileSheet.Goal -> ChoiceSheet(
            stringResource(R.string.profile_goal),
            notSet<FitnessGoal>() + FitnessGoal.entries.map { Choice(it, goalLabel(it)) },
            state.goal,
            { viewModel.setGoal(it); close() },
            close,
        )
        ProfileSheet.Height -> HeightSheet(state.heightCm, state.units, viewModel::setHeight, close)
        ProfileSheet.TargetWeight -> TargetWeightSheet(
            current = state.targetWeightKg,
            start = state.targetWeightKg ?: state.bodyWeightKg ?: DEFAULT_TARGET_KG,
            units = state.units,
            onSave = viewModel::setTargetWeight,
            onDone = close,
        )
        ProfileSheet.BirthDate -> BirthDateDialog(state.birthDate, { viewModel.setBirthDate(it); close() }, close)
        null -> Unit
    }
}

@Composable
private fun <T> notSet(): List<Choice<T?>> = listOf(Choice(null, stringResource(R.string.profile_not_set)))

@Composable
internal fun ProfileContent(
    state: ProfileUiState,
    onBack: () -> Unit,
    onBodyWeight: () -> Unit,
    onOpen: (ProfileSheet) -> Unit,
    onName: (String) -> Unit,
) {
    val d = GymBuddyTheme.dimens
    val cs = MaterialTheme.colorScheme
    val dash = stringResource(R.string.profile_dash)
    Surface(Modifier.fillMaxSize(), color = cs.background) {
        Column(Modifier.fillMaxSize()) {
            BackTopBar(onBack = onBack)
            if (state.loading) return@Column
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = d.insetScreen, end = d.insetScreen, bottom = d.section),
                verticalArrangement = Arrangement.spacedBy(d.stackTight),
            ) {
                item(key = "title") {
                    Text(
                        stringResource(R.string.you_profile),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                }
                item(key = "avatar") { AvatarHeader(state) { onOpen(ProfileSheet.Picture) } }
                item(key = "name") {
                    NameField(state.name, onName)
                    val line = when {
                        state.isGuest -> stringResource(R.string.you_guest_title)
                        state.viaGoogle && state.email.isNotBlank() -> stringResource(R.string.profile_email_google, state.email)
                        else -> state.email
                    }
                    if (line.isNotBlank()) {
                        Text(
                            line,
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant,
                            modifier = Modifier.padding(start = d.hairline, top = d.stackTight),
                        )
                    }
                }
                item(key = "training") {
                    Group(stringResource(R.string.profile_group_training)) {
                        ValueRow(
                            stringResource(R.string.profile_body_weight),
                            state.bodyWeightKg?.let { weightText(it, state.units) } ?: dash,
                            onBodyWeight,
                        )
                        RowDivider()
                        ValueRow(
                            stringResource(R.string.profile_experience),
                            state.experience?.let { experienceLabel(it) } ?: dash,
                            { onOpen(ProfileSheet.Experience) },
                        )
                    }
                }
                item(key = "about") {
                    Group(stringResource(R.string.profile_group_about)) {
                        ValueRow(
                            stringResource(R.string.profile_height),
                            state.heightCm?.let { heightText(it, state.units) } ?: dash,
                            { onOpen(ProfileSheet.Height) },
                        )
                        RowDivider()
                        ValueRow(stringResource(R.string.profile_birth_date), state.birthDate?.let(::birthText) ?: dash, { onOpen(ProfileSheet.BirthDate) })
                        RowDivider()
                        ValueRow(stringResource(R.string.profile_gender), state.gender?.let { genderLabel(it) } ?: dash, { onOpen(ProfileSheet.Gender) })
                        RowDivider()
                        ValueRow(stringResource(R.string.profile_activity), state.activity?.let { activityLabel(it) } ?: dash, { onOpen(ProfileSheet.Activity) })
                        RowDivider()
                        ValueRow(stringResource(R.string.profile_goal), state.goal?.let { goalLabel(it) } ?: dash, { onOpen(ProfileSheet.Goal) })
                        RowDivider()
                        ValueRow(
                            stringResource(R.string.profile_target_weight),
                            state.targetWeightKg?.let { weightText(it, state.units) } ?: dash,
                            { onOpen(ProfileSheet.TargetWeight) },
                        )
                    }
                }
            }
        }
    }
}

/** Birth dates are stored as UTC midnight (the date picker's millis). */
private fun birthText(millis: Long): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())

@Composable
private fun AvatarHeader(state: ProfileUiState, onClick: () -> Unit) {
    val d = GymBuddyTheme.dimens
    val cs = MaterialTheme.colorScheme
    val change = stringResource(R.string.profile_change_picture)
    Column(
        Modifier.fillMaxWidth().padding(vertical = d.stackTight),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(d.stackTight),
    ) {
        Box(
            Modifier
                .clip(FullShape)
                .selectable(selected = false, onClick = onClick, role = Role.Button)
                .clearAndSetSemantics { contentDescription = change; role = Role.Button },
        ) {
            Avatar(state.avatarId, state.name.ifBlank { state.email }, PageAvatar)
            Box(
                Modifier.align(Alignment.BottomEnd).size(Badge).background(cs.surfaceContainerHigh, FullShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(AppIcons.Ui.Edit), null, Modifier.size(d.iconSmall))
            }
        }
        AppLinkButton(change, onClick)
    }
}

/** Choose a picture (R15-4): one of 20 animals (F4: no letter option). TalkBack: "Fox, selected". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PictureSheet(current: String?, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val d = GymBuddyTheme.dimens
    val c = GymBuddyTheme.colors
    AppBottomSheet(onDismiss = onDismiss, title = stringResource(R.string.profile_choose_picture)) {
        Avatars.all.chunked(GRID_COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(d.stackTight)) {
                row.forEach { animal ->
                    val selected = animal.id == current
                    val label = stringResource(animal.name)
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(FullShape)
                            .then(if (selected) Modifier.border(BorderStroke(d.strokeEmphasis, c.cobalt), FullShape) else Modifier)
                            .selectable(selected = selected, role = Role.RadioButton, onClick = { onPick(animal.id) })
                            .semantics { contentDescription = label; this.selected = selected },
                        contentAlignment = Alignment.Center,
                    ) {
                        Avatar(animal.id, label, PickerCell)
                    }
                }
            }
        }
    }
}

private const val GRID_COLUMNS = 5
private val PickerCell = 56.dp

/**
 * F3b: what to save when the inline name field is left. Trimmed; null when
 * blank, too long or unchanged (blank reverts to [saved]).
 */
internal fun nameToSave(typed: String, saved: String): String? {
    val t = typed.trim()
    return t.takeIf { it.isNotEmpty() && it.length <= ProfileViewModel.MAX_NAME && it != saved }
}

/** Name typed in place (F3b). Saves on Done or when focus leaves; an invalid name reverts. */
@Composable
private fun NameField(saved: String, onSave: (String) -> Unit) {
    var text by rememberSaveable(saved) { mutableStateOf(saved) }
    val focus = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    val tooLong = text.trim().length > ProfileViewModel.MAX_NAME
    AppTextField(
        value = text,
        onValueChange = { text = it },
        label = stringResource(R.string.profile_name),
        isError = tooLong,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
        modifier = Modifier.fillMaxWidth().onFocusChanged { f ->
            if (focused && !f.isFocused) {
                val save = nameToSave(text, saved)
                if (save != null) onSave(save) else text = saved
            }
            focused = f.isFocused
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
/** "175 cm", or "5 ft 9 in" in imperial. */
@Composable
private fun heightText(cm: Float, units: PreferredUnits): String =
    if (units == PreferredUnits.IMPERIAL) {
        val (ft, inch) = BodyWheel.feetInches(UnitConverter.cmToInches(cm))
        stringResource(R.string.profile_height_ft_in, ft, inch)
    } else {
        stringResource(R.string.weight_value, cm.roundToInt().toString(), stringResource(R.string.unit_cm))
    }

/** F6: height on a wheel (cm, or ft + in). [onSave] takes cm or total inches as text. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeightSheet(current: Float?, units: PreferredUnits, onSave: (String) -> Boolean, onDone: () -> Unit) {
    val d = GymBuddyTheme.dimens
    val imperial = units == PreferredUnits.IMPERIAL
    val startCm = current ?: DEFAULT_HEIGHT_CM
    var value by rememberSaveable {
        mutableIntStateOf(if (imperial) UnitConverter.cmToInches(startCm).roundToInt() else startCm.roundToInt())
    }
    AppBottomSheet(onDismiss = onDone, title = stringResource(R.string.profile_height)) {
        if (imperial) {
            FeetInchesWheels(value, { value = it })
        } else {
            val range = ProfileViewModel.HEIGHT_CM.start.toInt()..ProfileViewModel.HEIGHT_CM.endInclusive.toInt()
            CmWheel(value.coerceIn(range), range, { value = it })
        }
        PrimaryButton(
            stringResource(R.string.you_save),
            { if (onSave(value.toString())) onDone() },
            Modifier.fillMaxWidth().padding(top = d.stackTight),
        )
        if (current != null) {
            AppLinkButton(stringResource(R.string.profile_clear), { onSave(""); onDone() })
        }
    }
}

/** F6: target weight on whole + tenth wheels, in the display unit. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TargetWeightSheet(current: Float?, start: Float, units: PreferredUnits, onSave: (String) -> Boolean, onDone: () -> Unit) {
    val d = GymBuddyTheme.dimens
    val range = ProfileViewModel.WEIGHT_KG.let {
        kgToDisplay(it.start, units)..kgToDisplay(it.endInclusive, units)
    }
    var value by rememberSaveable { mutableFloatStateOf(kgToDisplay(start, units).coerceIn(range.start, range.endInclusive)) }
    val unit = UnitConverter.getWeightUnitLabel(units)
    AppBottomSheet(onDismiss = onDone, title = stringResource(R.string.profile_target_weight)) {
        WeightWheels(value, range, unit, stringResource(R.string.profile_target_weight), { value = it })
        PrimaryButton(
            stringResource(R.string.you_save),
            { if (onSave(value.toString())) onDone() },
            Modifier.fillMaxWidth().padding(top = d.stackTight),
        )
        if (current != null) {
            AppLinkButton(stringResource(R.string.profile_clear), { onSave(""); onDone() })
        }
    }
}

private fun kgToDisplay(kg: Float, units: PreferredUnits) =
    if (units == PreferredUnits.IMPERIAL) UnitConverter.kgToLbs(kg) else kg

private const val DEFAULT_HEIGHT_CM = 175f
private const val DEFAULT_TARGET_KG = 70f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDateDialog(current: Long?, onPicked: (Long?) -> Unit, onDismiss: () -> Unit) {
    val picker = rememberDatePickerState(initialSelectedDateMillis = current)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            AppLinkButton(stringResource(R.string.session_edit_ok), {
                val millis = picker.selectedDateMillis
                if (millis == null) onDismiss() else onPicked(millis)
            })
        },
        dismissButton = {
            Row {
                if (current != null) AppLinkButton(stringResource(R.string.profile_clear), { onPicked(null) })
                AppLinkButton(stringResource(R.string.cancel), onDismiss)
            }
        },
    ) {
        DatePicker(picker)
    }
}

@Composable
internal fun experienceLabel(e: ExperienceLevel) = stringResource(
    when (e) {
        ExperienceLevel.NEW -> R.string.setup_experience_new
        ExperienceLevel.SOME -> R.string.setup_experience_some
        ExperienceLevel.EXPERIENCED -> R.string.setup_experience_pro
    },
)

@Composable
private fun experienceSub(e: ExperienceLevel) = stringResource(
    when (e) {
        ExperienceLevel.NEW -> R.string.setup_experience_new_sub
        ExperienceLevel.SOME -> R.string.setup_experience_some_sub
        ExperienceLevel.EXPERIENCED -> R.string.setup_experience_pro_sub
    },
)

@Composable
internal fun genderLabel(g: Gender) = stringResource(
    when (g) {
        Gender.MALE -> R.string.profile_gender_male
        Gender.FEMALE -> R.string.profile_gender_female
        Gender.OTHER -> R.string.profile_gender_other
        Gender.PREFER_NOT_TO_SAY -> R.string.profile_gender_private
    },
)

@Composable
internal fun activityLabel(a: ActivityLevel) = stringResource(
    when (a) {
        ActivityLevel.SEDENTARY -> R.string.profile_activity_sedentary
        ActivityLevel.LIGHTLY_ACTIVE -> R.string.profile_activity_light
        ActivityLevel.MODERATELY_ACTIVE -> R.string.profile_activity_moderate
        ActivityLevel.VERY_ACTIVE -> R.string.profile_activity_very
        ActivityLevel.ATHLETE -> R.string.profile_activity_athlete
    },
)

@Composable
private fun activitySub(a: ActivityLevel) = stringResource(
    when (a) {
        ActivityLevel.SEDENTARY -> R.string.profile_activity_sedentary_sub
        ActivityLevel.LIGHTLY_ACTIVE -> R.string.profile_activity_light_sub
        ActivityLevel.MODERATELY_ACTIVE -> R.string.profile_activity_moderate_sub
        ActivityLevel.VERY_ACTIVE -> R.string.profile_activity_very_sub
        ActivityLevel.ATHLETE -> R.string.profile_activity_athlete_sub
    },
)

@Composable
internal fun goalLabel(g: FitnessGoal) = stringResource(
    when (g) {
        FitnessGoal.BUILD_MUSCLE -> R.string.profile_goal_muscle
        FitnessGoal.GET_STRONGER -> R.string.profile_goal_stronger
        FitnessGoal.LOSE_WEIGHT -> R.string.profile_goal_lose
        FitnessGoal.MAINTAIN -> R.string.profile_goal_maintain
        FitnessGoal.GENERAL_FITNESS -> R.string.profile_goal_general
        FitnessGoal.IMPROVE_ENDURANCE -> R.string.profile_goal_endurance
    },
)
