package com.rukavina.gymbuddy.ui.template

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.domain.repository.WorkoutTemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Ends a [PendingTemplateDeletes] entry once its snackbar is gone:
 * Undo puts the template back, anything else writes the tombstone.
 * Lives with MainScreen so leaving the template page doesn't cancel it.
 */
@HiltViewModel
class TemplateDeleteViewModel @Inject constructor(
    private val pending: PendingTemplateDeletes,
    private val repository: WorkoutTemplateRepository,
) : ViewModel() {

    fun undo(templateId: String) = pending.remove(templateId)

    fun commit(templateId: String) {
        viewModelScope.launch {
            try {
                repository.deleteTemplate(templateId)
            } finally {
                pending.remove(templateId)
            }
        }
    }
}
