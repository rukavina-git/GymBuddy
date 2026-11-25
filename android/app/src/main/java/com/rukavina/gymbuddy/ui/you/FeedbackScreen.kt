package com.rukavina.gymbuddy.ui.you

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.data.feedback.FEEDBACK_MAX_LENGTH
import com.rukavina.gymbuddy.data.feedback.FeedbackSubmitResult
import com.rukavina.gymbuddy.ui.design.AppTextField
import com.rukavina.gymbuddy.ui.design.BackTopBar
import com.rukavina.gymbuddy.ui.design.PrimaryButton
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme

/** Counter shows from here on, so short messages stay calm. */
private const val COUNTER_FROM = FEEDBACK_MAX_LENGTH - 200

/** You › Send feedback (Plan 10 step 06b): one field, Send, then a thank-you. */
@Composable
fun FeedbackScreen(onBack: () -> Unit, viewModel: FeedbackViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FeedbackContent(state, onBack, viewModel::onTextChange, viewModel::send)
}

@Composable
internal fun FeedbackContent(
    state: FeedbackUiState,
    onBack: () -> Unit,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    val d = GymBuddyTheme.dimens
    val cs = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxSize(), color = cs.background) {
        Column(Modifier.fillMaxSize().imePadding()) {
            BackTopBar(onBack = onBack, title = stringResource(R.string.you_feedback))
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = d.insetScreen),
                verticalArrangement = Arrangement.spacedBy(d.group),
            ) {
                val result = state.result
                if (result != null) {
                    Done(result, onBack)
                    return@Column
                }
                AppTextField(
                    value = state.text,
                    onValueChange = onTextChange,
                    label = stringResource(R.string.feedback_label),
                    placeholder = stringResource(R.string.feedback_placeholder),
                    singleLine = false,
                    minLines = 6,
                    enabled = !state.sending,
                    isError = state.tooLong,
                    helper = when {
                        state.tooLong -> (state.length - FEEDBACK_MAX_LENGTH).let { pluralStringResource(R.plurals.feedback_too_long, it, it) }
                        state.length >= COUNTER_FROM -> stringResource(R.string.feedback_counter, state.length, FEEDBACK_MAX_LENGTH)
                        else -> null
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                Text(
                    stringResource(R.string.feedback_what_is_sent),
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
                if (state.failed) {
                    Text(stringResource(R.string.feedback_failed), style = MaterialTheme.typography.bodyMedium, color = cs.error)
                }
                PrimaryButton(
                    text = stringResource(R.string.feedback_send),
                    onClick = onSend,
                    enabled = state.canSend,
                    loading = state.sending,
                    modifier = Modifier.fillMaxWidth().padding(bottom = d.section),
                )
            }
        }
    }
}

@Composable
private fun Done(result: FeedbackSubmitResult, onBack: () -> Unit) {
    val sent = result == FeedbackSubmitResult.SENT
    Text(
        stringResource(if (sent) R.string.feedback_sent_title else R.string.feedback_queued_title),
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(top = GymBuddyTheme.dimens.group),
    )
    Text(
        stringResource(if (sent) R.string.feedback_sent_body else R.string.feedback_queued_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    PrimaryButton(text = stringResource(R.string.feedback_done), onClick = onBack, modifier = Modifier.fillMaxWidth())
}
