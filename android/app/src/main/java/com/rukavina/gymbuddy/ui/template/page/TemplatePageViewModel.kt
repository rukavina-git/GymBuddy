package com.rukavina.gymbuddy.ui.template.page

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.domain.id.IdGenerator
import com.rukavina.gymbuddy.domain.model.EntitySource
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.domain.model.SyncState
import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
import com.rukavina.gymbuddy.domain.repository.WorkoutTemplateRepository
import com.rukavina.gymbuddy.domain.usecase.template.TemplateStats
import com.rukavina.gymbuddy.ui.template.PendingTemplateDeletes
import com.rukavina.gymbuddy.ui.template.TemplateLine
import com.rukavina.gymbuddy.ui.template.TemplateLines
import com.rukavina.gymbuddy.ui.template.TemplateSources
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class TemplatePageUiState(
    val loaded: Boolean = false,
    /** Null once loaded: the template is gone (deleted, hidden elsewhere). */
    val line: TemplateLine? = null,
    val units: PreferredUnits = PreferredUnits.METRIC,
    val busy: Boolean = false,
    val today: LocalDate = LocalDate.MIN,
    val zone: ZoneId = ZoneId.systemDefault(),
)

sealed interface TemplatePageEvent {
    /** A copy was made: open its page, and its editor when [edit]. */
    data class Open(val templateId: String, val edit: Boolean) : TemplatePageEvent
    /** Delete asked: leave and offer Undo. The tombstone waits for the snackbar. */
    data class Deleted(val templateId: String, val title: String) : TemplatePageEvent
    data object Hidden : TemplatePageEvent
    data object Failed : TemplatePageEvent
}

/** Template page (Figma Page A 92:12491, Starter page 93:13464). */
@HiltViewModel
class TemplatePageViewModel @Inject constructor(
    savedState: SavedStateHandle,
    sources: TemplateSources,
    private val repository: WorkoutTemplateRepository,
    private val pending: PendingTemplateDeletes,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
) : ViewModel() {

    val templateId: String = checkNotNull(savedState[ARG_TEMPLATE_ID]) { "templateId missing" }

    private val busy = MutableStateFlow(false)
    private val templates = sources.templates

    val uiState: StateFlow<TemplatePageUiState> = combine(
        templates, sources.sessions, sources.plans, sources.units, busy,
    ) { all, sessions, plans, units, isBusy ->
        TemplatePageUiState(
            loaded = true,
            line = all.firstOrNull { it.id == templateId }?.let { TemplateLines(sessions, plans).line(it) },
            units = units,
            busy = isBusy,
            today = LocalDate.now(clock),
            zone = clock.zone,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TemplatePageUiState())

    private val _events = Channel<TemplatePageEvent>(Channel.BUFFERED)
    val events: Flow<TemplatePageEvent> = _events.receiveAsFlow()

    /**
     * Duplicate (yours) or Make a copy (starter): a CUSTOM copy with new
     * ids, named by [name] (n-th free candidate). A starter copy keeps
     * where it came from in derivedFromId. [edit]: pencil on a starter.
     */
    fun copy(edit: Boolean, name: (title: String, n: Int) -> String) = runBusy {
        val source = current() ?: return@runBusy
        val taken = templates.first().map { it.title }
        val copy = source.copy(
            id = idGenerator.newId(),
            title = TemplateStats.copyTitle(taken) { n -> name(source.title, n) },
            templateExercises = source.templateExercises.map { it.copy(id = idGenerator.newId()) },
            source = EntitySource.CUSTOM,
            ownerId = null,
            derivedFromId = if (source.source == EntitySource.DEFAULT) source.id else source.derivedFromId,
            deprecated = false,
            updatedAt = 0L,
            deletedAt = null,
            revision = 0,
            syncState = SyncState.PENDING,
        )
        repository.createTemplate(copy)
        _events.send(TemplatePageEvent.Open(copy.id, edit))
    }

    /** Hide this plan (starter): goes to Settings › Hidden templates. */
    fun hide() = runBusy {
        repository.hideTemplate(templateId)
        _events.send(TemplatePageEvent.Hidden)
    }

    /** Yours only. The list hides it now; the delete runs if Undo isn't pressed. */
    fun delete() {
        val title = uiState.value.line?.title ?: return
        pending.add(templateId)
        _events.trySend(TemplatePageEvent.Deleted(templateId, title))
    }

    private suspend fun current(): WorkoutTemplate? = templates.first().firstOrNull { it.id == templateId }

    private fun runBusy(block: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.send(TemplatePageEvent.Failed)
            } finally {
                busy.value = false
            }
        }
    }

    companion object {
        const val ARG_TEMPLATE_ID = "templateId"
    }
}
