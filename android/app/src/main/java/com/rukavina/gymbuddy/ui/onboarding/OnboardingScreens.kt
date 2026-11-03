package com.rukavina.gymbuddy.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.domain.model.ExperienceLevel
import com.rukavina.gymbuddy.domain.model.FitnessGoal
import com.rukavina.gymbuddy.domain.model.StarterPlan
import com.rukavina.gymbuddy.domain.model.TrainingDays
import com.rukavina.gymbuddy.ui.design.AppIconButton
import com.rukavina.gymbuddy.ui.design.AppTextButton
import com.rukavina.gymbuddy.ui.design.ButtonSize
import com.rukavina.gymbuddy.ui.design.OutlineButton
import com.rukavina.gymbuddy.ui.design.PrimaryButton
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.FullShape
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import com.rukavina.gymbuddy.ui.theme.fadeThroughIn
import com.rukavina.gymbuddy.ui.theme.fadeThroughOut

/** Raw sizes from the Figma frames (DESIGN.md §4). */
private val BrandMarkLarge = 80.dp
private val WordmarkLarge = 32.dp
private val ChoiceRowHeight = 68.dp
private val SuggestedTagHeight = 24.dp

/** Setup questions with a progress bar; Ready has none. */
private val QUESTIONS = SetupStep.entries.filter { it != SetupStep.READY }

/** R16-1 Welcome: Get started (setup, then guest) or I have an account. */
@Composable
fun WelcomeScreen(onGetStarted: () -> Unit, onHaveAccount: () -> Unit) {
    val d = GymBuddyTheme.dimens
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(d.insetScreen),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Image(painterResource(AppIcons.Brand.AppIcon), null, Modifier.size(BrandMarkLarge))
        Spacer(Modifier.height(d.section))
        Image(
            painterResource(AppIcons.Brand.Wordmark),
            stringResource(R.string.cd_app_name),
            Modifier.height(WordmarkLarge).semantics { heading() },
        )
        Spacer(Modifier.height(d.stack))
        Text(
            stringResource(R.string.welcome_line),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1.4f))
        PrimaryButton(stringResource(R.string.welcome_get_started), onGetStarted, Modifier.fillMaxWidth(), size = ButtonSize.Medium)
        Spacer(Modifier.height(d.stack))
        OutlineButton(stringResource(R.string.welcome_have_account), onHaveAccount, Modifier.fillMaxWidth())
    }
}

/** R16-2 … R16-6b: one route; back walks the questions, then leaves. */
@Composable
fun SetupScreen(viewModel: OnboardingViewModel, onExit: () -> Unit, onDone: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { if (it == OnboardingEvent.Done) onDone() }
    }
    BackHandler(enabled = state.step != SetupStep.EXPERIENCE) { viewModel.back() }
    SetupContent(
        state = state,
        onBack = { if (!viewModel.back()) onExit() },
        onSkip = viewModel::skip,
        onExperience = viewModel::chooseExperience,
        onGoal = viewModel::chooseGoal,
        onDays = viewModel::chooseDays,
        onPlan = viewModel::choosePlan,
        onFinish = viewModel::finish,
    )
}

@Composable
internal fun SetupContent(
    state: OnboardingUiState,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onExperience: (ExperienceLevel) -> Unit,
    onGoal: (FitnessGoal) -> Unit,
    onDays: (TrainingDays?) -> Unit,
    onPlan: (String?) -> Unit,
    onFinish: () -> Unit,
) {
    AnimatedContent(
        targetState = state.step,
        transitionSpec = { fadeThroughIn togetherWith fadeThroughOut },
        label = "setup",
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) { step ->
        if (step == SetupStep.READY) {
            ReadyContent(signedIn = false, saving = state.saving, onFinish = onFinish)
        } else {
            QuestionFrame(step, onBack, onSkip) {
                when (step) {
                    SetupStep.EXPERIENCE -> ExperienceQuestion(state, onExperience)
                    SetupStep.GOAL -> GoalQuestion(state, onGoal)
                    SetupStep.DAYS -> DaysQuestion(state, onDays)
                    SetupStep.PLAN -> PlanQuestion(state, onPlan)
                    SetupStep.READY -> Unit
                }
            }
        }
    }
}

@Composable
private fun QuestionFrame(step: SetupStep, onBack: () -> Unit, onSkip: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val d = GymBuddyTheme.dimens
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = d.hairline),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIconButton(AppIcons.Ui.Back, stringResource(R.string.cd_back), onBack)
            Spacer(Modifier.weight(1f))
            AppTextButton(stringResource(R.string.setup_skip), onSkip)
        }
        SetupProgress(QUESTIONS.indexOf(step) + 1, QUESTIONS.size)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(d.insetScreen),
            verticalArrangement = Arrangement.spacedBy(d.stack),
            content = content,
        )
    }
}

@Composable
private fun SetupProgress(current: Int, total: Int) {
    val d = GymBuddyTheme.dimens
    val label = stringResource(R.string.setup_progress, current, total)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = d.insetScreen, vertical = d.stackTight)
            .semantics(mergeDescendants = true) { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(d.stackTight),
    ) {
        repeat(total) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(d.hairline)
                    .background(
                        if (i < current) GymBuddyTheme.colors.cobalt else MaterialTheme.colorScheme.surfaceContainerHigh,
                        FullShape,
                    )
            )
        }
    }
}

@Composable
private fun QuestionTitle(title: String, subtitle: String? = null) {
    val d = GymBuddyTheme.dimens
    Column(Modifier.padding(top = d.stackTight, bottom = d.stack), verticalArrangement = Arrangement.spacedBy(d.stackTight)) {
        Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ExperienceQuestion(state: OnboardingUiState, onChoose: (ExperienceLevel) -> Unit) {
    QuestionTitle(stringResource(R.string.setup_experience_title))
    val options = listOf(
        Triple(ExperienceLevel.NEW, R.string.setup_experience_new, R.string.setup_experience_new_sub),
        Triple(ExperienceLevel.SOME, R.string.setup_experience_some, R.string.setup_experience_some_sub),
        Triple(ExperienceLevel.EXPERIENCED, R.string.setup_experience_pro, R.string.setup_experience_pro_sub),
    )
    options.forEach { (level, title, sub) ->
        ChoiceRow(
            title = stringResource(title),
            subtitle = stringResource(sub),
            selected = state.experience.answered && state.experience.value == level,
            onClick = { onChoose(level) },
        )
    }
}

@Composable
private fun GoalQuestion(state: OnboardingUiState, onChoose: (FitnessGoal) -> Unit) {
    QuestionTitle(stringResource(R.string.setup_goal_title))
    GOALS.forEach { (goal, title) ->
        ChoiceRow(
            title = stringResource(title),
            selected = state.goal.answered && state.goal.value == goal,
            onClick = { onChoose(goal) },
        )
    }
}

/** The five onboarding goals (owner decision 2A); MAINTAIN is only for older profiles. */
internal val GOALS = listOf(
    FitnessGoal.BUILD_MUSCLE to R.string.setup_goal_muscle,
    FitnessGoal.GET_STRONGER to R.string.setup_goal_strength,
    FitnessGoal.LOSE_WEIGHT to R.string.setup_goal_weight,
    FitnessGoal.GENERAL_FITNESS to R.string.setup_goal_fit,
    FitnessGoal.IMPROVE_ENDURANCE to R.string.setup_goal_endurance,
)

@Composable
private fun DaysQuestion(state: OnboardingUiState, onChoose: (TrainingDays?) -> Unit) {
    QuestionTitle(stringResource(R.string.setup_days_title), stringResource(R.string.setup_days_sub))
    val options = listOf(
        TrainingDays.ONE_TO_THREE to R.string.setup_days_1_3,
        TrainingDays.FOUR to R.string.setup_days_4,
        TrainingDays.FIVE_TO_SEVEN to R.string.setup_days_5_7,
        null to R.string.setup_days_unsure,
    )
    options.forEach { (days, title) ->
        ChoiceRow(
            title = stringResource(title),
            selected = state.days.answered && state.days.value == days,
            onClick = { onChoose(days) },
        )
    }
}

@Composable
private fun PlanQuestion(state: OnboardingUiState, onChoose: (String?) -> Unit) {
    QuestionTitle(stringResource(R.string.setup_plan_title), stringResource(R.string.setup_plan_sub))
    val suggested = state.suggested
    // The suggestion is cobalt until the user picks something else.
    val chosen = if (state.plan.answered) state.plan.value else suggested?.id
    state.plans.forEach { plan ->
        ChoiceRow(
            title = plan.title,
            subtitle = planSummary(plan),
            body = plan.description,
            tag = if (plan.id == suggested?.id) stringResource(R.string.setup_plan_suggested) else null,
            selected = chosen == plan.id,
            onClick = { onChoose(plan.id) },
        )
    }
    ChoiceRow(
        title = stringResource(R.string.setup_plan_none),
        subtitle = stringResource(R.string.setup_plan_none_sub),
        selected = state.plan.answered && state.plan.value == null,
        onClick = { onChoose(null) },
    )
    if (suggested != null) {
        Text(
            stringResource(
                when (if (state.days.answered) state.days.value else suggested.suggestedFor) {
                    TrainingDays.ONE_TO_THREE -> R.string.setup_plan_footer_1_3
                    TrainingDays.FOUR -> R.string.setup_plan_footer_4
                    TrainingDays.FIVE_TO_SEVEN -> R.string.setup_plan_footer_5_7
                    null -> R.string.setup_plan_footer_unsure
                }
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun planSummary(plan: StarterPlan): String {
    val workouts = pluralStringResource(R.plurals.setup_plan_workouts, plan.templateIds.size, plan.templateIds.size)
    val days = when (plan.suggestedFor) {
        TrainingDays.ONE_TO_THREE -> stringResource(R.string.setup_plan_days_1_3)
        TrainingDays.FOUR -> stringResource(R.string.setup_plan_days_4)
        TrainingDays.FIVE_TO_SEVEN -> stringResource(R.string.setup_plan_days_5_7)
        null -> null
    }
    return listOfNotNull(days, workouts).joinToString(" · ")
}

/** A setup answer: tap = choose and go on. Selected = 2dp cobalt ring ("you are here"). */
@Composable
private fun ChoiceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    subtitle: String? = null,
    body: String? = null,
    tag: String? = null,
) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    val selectedLabel = stringResource(R.string.setup_selected)
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = cs.surfaceContainer,
        border = if (selected) BorderStroke(d.strokeEmphasis, GymBuddyTheme.colors.cobalt) else null,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ChoiceRowHeight)
            .semantics { this.selected = selected },
    ) {
        Row(Modifier.padding(start = d.insetCard, end = d.stackTight, top = d.stack, bottom = d.stack), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(d.hairline)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(d.stackTight)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    if (tag != null) SuggestedTag(tag)
                }
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = GymBuddyTheme.colors.textSoft)
                if (body != null) Text(body, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
            Icon(
                painterResource(AppIcons.Ui.ChevronRight),
                if (selected) selectedLabel else null,
                Modifier.size(d.iconSmall),
                tint = cs.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SuggestedTag(text: String) {
    val d = GymBuddyTheme.dimens
    Surface(shape = FullShape, color = GymBuddyTheme.colors.cobaltTint) {
        Box(Modifier.heightIn(min = SuggestedTagHeight).padding(horizontal = d.stackTight), contentAlignment = Alignment.Center) {
            Text(text, style = GymBuddyTheme.type.tag, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/** R16-6b: one honest line, three tips, lime Let's go → Home. */
@Composable
internal fun ReadyContent(signedIn: Boolean, saving: Boolean, onFinish: () -> Unit) {
    val d = GymBuddyTheme.dimens
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(d.insetScreen),
    ) {
        Spacer(Modifier.weight(1f))
        Text(stringResource(R.string.ready_title), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(d.stackTight))
        Text(stringResource(R.string.ready_line), style = MaterialTheme.typography.bodyLarge, color = GymBuddyTheme.colors.textSoft)
        Spacer(Modifier.height(d.section))
        Text(stringResource(R.string.ready_tips), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(d.stackTight))
        Column(verticalArrangement = Arrangement.spacedBy(d.stackTight)) {
            listOf(
                R.string.ready_tip_keypad,
                R.string.ready_tip_swipe,
                if (signedIn) R.string.ready_tip_account else R.string.ready_tip_guest,
            ).forEach { tip ->
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(tip), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(d.insetCard))
                }
            }
        }
        Spacer(Modifier.weight(1.4f))
        PrimaryButton(stringResource(R.string.ready_go), onFinish, Modifier.fillMaxWidth(), enabled = !saving)
    }
}
