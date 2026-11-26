package com.rukavina.gymbuddy.ui.you

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.data.feedback.FEEDBACK_MAX_LENGTH
import com.rukavina.gymbuddy.data.feedback.FeedbackOutbox
import com.rukavina.gymbuddy.data.feedback.FeedbackSubmitResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedbackUiState(
    val text: String = "",
    val sending: Boolean = false,
    /** Set once Send went through: the screen shows the matching thank-you. */
    val result: FeedbackSubmitResult? = null,
    /** Saving on the phone failed (disk full ...). Text is kept. */
    val failed: Boolean = false,
) {
    val length: Int get() = text.trim().length
    val tooLong: Boolean get() = length > FEEDBACK_MAX_LENGTH
    val canSend: Boolean get() = length > 0 && !tooLong && !sending
}

/** You › Send feedback (Plan 10 step 06b). */
@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val outbox: FeedbackOutbox,
) : ViewModel() {
    private val _state = MutableStateFlow(FeedbackUiState())
    val state: StateFlow<FeedbackUiState> = _state.asStateFlow()

    fun onTextChange(text: String) = _state.update { it.copy(text = text, failed = false) }

    fun send() {
        val current = _state.value
        if (!current.canSend) return
        _state.update { it.copy(sending = true, failed = false) }
        viewModelScope.launch {
            try {
                val result = outbox.submit(current.text)
                _state.update { it.copy(sending = false, result = result) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "feedback not saved", e)
                _state.update { it.copy(sending = false, failed = true) }
            }
        }
    }

    private companion object {
        const val TAG = "Feedback"
    }
}
