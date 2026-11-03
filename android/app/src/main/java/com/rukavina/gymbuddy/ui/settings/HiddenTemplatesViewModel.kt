package com.rukavina.gymbuddy.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
import com.rukavina.gymbuddy.domain.repository.WorkoutTemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Settings › Hidden templates (old screen until 7.10). What's left of the
 * v1 WorkoutTemplateViewModel after the 05b editor replaced its dialogs.
 */
@HiltViewModel
class HiddenTemplatesViewModel @Inject constructor(
    private val repository: WorkoutTemplateRepository,
) : ViewModel() {

    val hiddenTemplates: Flow<List<WorkoutTemplate>> = repository.getHiddenTemplates()

    fun unhideTemplate(templateId: String) {
        viewModelScope.launch {
            try {
                repository.unhideTemplate(templateId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // The row stays in the list; nothing else to undo.
            }
        }
    }
}
