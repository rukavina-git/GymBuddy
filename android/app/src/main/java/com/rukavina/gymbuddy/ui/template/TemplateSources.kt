package com.rukavina.gymbuddy.ui.template

import com.rukavina.gymbuddy.data.repository.AppPreferencesRepository
import com.rukavina.gymbuddy.data.repository.StarterPlanRepository
import com.rukavina.gymbuddy.data.repository.UserProfileRepository
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.domain.model.StarterPlan
import com.rukavina.gymbuddy.domain.model.WorkoutSession
import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
import com.rukavina.gymbuddy.domain.repository.WorkoutSessionRepository
import com.rukavina.gymbuddy.domain.repository.WorkoutTemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What Home, the Workouts tab and the template page read (Plan 7 05a).
 * An interface so their ViewModels test without Room. Templates that
 * are waiting on a delete Undo ([PendingTemplateDeletes]) are left out.
 */
interface TemplateSources {
    /** Visible templates (not hidden, not deprecated, not deleted). */
    val templates: Flow<List<WorkoutTemplate>>
    val sessions: Flow<List<WorkoutSession>>
    val plans: Flow<List<StarterPlan>>
    val activePlanId: Flow<String?>
    val units: Flow<PreferredUnits>
}

class RoomTemplateSources @Inject constructor(
    templateRepository: WorkoutTemplateRepository,
    sessionRepository: WorkoutSessionRepository,
    planRepository: StarterPlanRepository,
    profileRepository: UserProfileRepository,
    preferences: AppPreferencesRepository,
    pending: PendingTemplateDeletes,
) : TemplateSources {
    override val templates = combine(templateRepository.getAllTemplates(), pending.ids) { all, gone ->
        if (gone.isEmpty()) all else all.filter { it.id !in gone }
    }
    override val sessions = sessionRepository.getAllWorkoutSessions().map { s -> s.filter { it.deletedAt == null } }
    override val plans = planRepository.observePlans()
    override val activePlanId = profileRepository.observeProfile().map { it?.activePlanId }.distinctUntilChanged()
    override val units = preferences.preferredUnits
}

/**
 * Deletes waiting on their snackbar Undo (Figma notes: Delete (Undo)).
 * The repository has no undelete, so the tombstone is written only
 * once the snackbar goes without Undo; until then lists skip the id.
 */
@Singleton
class PendingTemplateDeletes @Inject constructor() {
    private val _ids = MutableStateFlow<Set<String>>(emptySet())
    val ids: StateFlow<Set<String>> = _ids.asStateFlow()

    fun add(id: String) = _ids.update { it + id }
    fun remove(id: String) = _ids.update { it - id }
}
