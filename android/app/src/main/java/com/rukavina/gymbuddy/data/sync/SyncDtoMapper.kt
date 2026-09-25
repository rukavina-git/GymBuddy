package com.rukavina.gymbuddy.data.sync

import com.rukavina.gymbuddy.data.local.entity.ExerciseEntity
import com.rukavina.gymbuddy.data.local.entity.PerformedExerciseEntity
import com.rukavina.gymbuddy.data.local.entity.PerformedExerciseWithSets
import com.rukavina.gymbuddy.data.local.entity.TemplateExerciseEntity
import com.rukavina.gymbuddy.data.local.entity.UserExerciseStateEntity
import com.rukavina.gymbuddy.data.local.entity.UserProfileEntity
import com.rukavina.gymbuddy.data.local.entity.UserTemplateStateEntity
import com.rukavina.gymbuddy.data.local.entity.WorkoutSessionEntity
import com.rukavina.gymbuddy.data.local.entity.WorkoutSetEntity
import com.rukavina.gymbuddy.data.local.entity.WorkoutTemplateEntity
import com.rukavina.gymbuddy.data.remote.generated.models.Exercise
import com.rukavina.gymbuddy.data.remote.generated.models.PerformedExercise
import com.rukavina.gymbuddy.data.remote.generated.models.TemplateExercise
import com.rukavina.gymbuddy.data.remote.generated.models.UserExerciseState
import com.rukavina.gymbuddy.data.remote.generated.models.UserProfile
import com.rukavina.gymbuddy.data.remote.generated.models.UserTemplateState
import com.rukavina.gymbuddy.data.remote.generated.models.WorkoutSession
import com.rukavina.gymbuddy.data.remote.generated.models.WorkoutSet
import com.rukavina.gymbuddy.data.remote.generated.models.WorkoutTemplate
import com.rukavina.gymbuddy.domain.model.SyncState
import java.net.URI
import java.util.UUID
import com.rukavina.gymbuddy.data.remote.generated.models.ActivityLevel as DtoActivityLevel
import com.rukavina.gymbuddy.data.remote.generated.models.DifficultyLevel as DtoDifficultyLevel
import com.rukavina.gymbuddy.data.remote.generated.models.EntitySource as DtoEntitySource
import com.rukavina.gymbuddy.data.remote.generated.models.Equipment as DtoEquipment
import com.rukavina.gymbuddy.data.remote.generated.models.ExerciseCategory as DtoExerciseCategory
import com.rukavina.gymbuddy.data.remote.generated.models.ExerciseTrackingType as DtoExerciseTrackingType
import com.rukavina.gymbuddy.data.remote.generated.models.ExerciseType as DtoExerciseType
import com.rukavina.gymbuddy.data.remote.generated.models.FitnessGoal as DtoFitnessGoal
import com.rukavina.gymbuddy.data.remote.generated.models.Gender as DtoGender
import com.rukavina.gymbuddy.data.remote.generated.models.MuscleGroup as DtoMuscleGroup
import com.rukavina.gymbuddy.data.remote.generated.models.SetType as DtoSetType

/** A WorkoutSession aggregate as Room stores it: parent row plus every child row. */
data class SessionAggregate(
    val session: WorkoutSessionEntity,
    val performedExercises: List<PerformedExerciseEntity>,
    val sets: List<WorkoutSetEntity>
)

/** A WorkoutTemplate aggregate as Room stores it. */
data class TemplateAggregate(
    val template: WorkoutTemplateEntity,
    val templateExercises: List<TemplateExerciseEntity>
)

/**
 * Room entity <-> generated API DTO. Room keys everything by String; the
 * spec's `format: uuid` makes the generated DTOs carry java.util.UUID,
 * so every id and soft reference is converted here and nowhere else.
 *
 * Entity -> DTO throws IllegalArgumentException for a local id that is
 * not a UUID - the push path treats that entity as unsendable rather
 * than inventing an id. DTO -> entity always yields SYNCED rows: the
 * server's copy is by definition in sync.
 *
 * Enums are converted by constant name. The spec's enums are generated
 * from the same values the domain enums declare, so a name missing on
 * one side is a contract break and fails loudly.
 */
object SyncDtoMapper {

    // ---- Exercise ----

    fun toDto(e: ExerciseEntity) = Exercise(
        id = uuid(e.id),
        name = e.name,
        primaryMuscles = e.primaryMuscles.map { it.convert<DtoMuscleGroup>() },
        secondaryMuscles = e.secondaryMuscles.map { it.convert<DtoMuscleGroup>() },
        instructions = e.instructions,
        tips = e.tips,
        difficulty = e.difficulty.convert<DtoDifficultyLevel>(),
        equipmentNeeded = e.equipmentNeeded.map { it.convert<DtoEquipment>() },
        category = e.category.convert<DtoExerciseCategory>(),
        exerciseType = e.exerciseType.convert<DtoExerciseType>(),
        trackingType = e.trackingType.convert<DtoExerciseTrackingType>(),
        source = e.source.convert<DtoEntitySource>(),
        deprecated = e.deprecated,
        updatedAt = e.updatedAt,
        revision = e.revision,
        description = e.description,
        videoUrl = uri(e.videoUrl),
        thumbnailUrl = uri(e.thumbnailUrl),
        ownerId = e.ownerId,
        derivedFromId = e.derivedFromId?.let(::uuid),
        deletedAt = e.deletedAt
    )

    fun toEntity(d: Exercise) = ExerciseEntity(
        id = d.id.toString(),
        name = d.name,
        primaryMuscles = d.primaryMuscles.map { it.convert() },
        secondaryMuscles = d.secondaryMuscles.map { it.convert() },
        description = d.description,
        instructions = d.instructions,
        tips = d.tips,
        difficulty = d.difficulty.convert(),
        equipmentNeeded = d.equipmentNeeded.map { it.convert() },
        category = d.category.convert(),
        exerciseType = d.exerciseType.convert(),
        trackingType = d.trackingType.convert(),
        videoUrl = d.videoUrl?.toString(),
        thumbnailUrl = d.thumbnailUrl?.toString(),
        source = d.source.convert(),
        ownerId = d.ownerId,
        derivedFromId = d.derivedFromId?.toString(),
        deprecated = d.deprecated,
        updatedAt = d.updatedAt,
        deletedAt = d.deletedAt,
        revision = d.revision,
        syncState = SyncState.SYNCED
    )

    // ---- WorkoutSession aggregate ----

    fun toDto(session: WorkoutSessionEntity, performedExercises: List<PerformedExerciseWithSets>) = WorkoutSession(
        id = uuid(session.id),
        startedAt = session.startedAt,
        durationSeconds = session.durationSeconds,
        title = session.title,
        performedExercises = performedExercises.map { toDto(it) },
        updatedAt = session.updatedAt,
        revision = session.revision,
        endedAt = session.endedAt,
        notes = session.notes,
        templateId = session.templateId?.let(::uuid),
        templateTitle = session.templateTitle,
        deletedAt = session.deletedAt
    )

    private fun toDto(pe: PerformedExerciseWithSets) = PerformedExercise(
        id = uuid(pe.performedExercise.id),
        exerciseId = uuid(pe.performedExercise.exerciseId),
        orderIndex = pe.performedExercise.orderIndex,
        exerciseName = pe.performedExercise.exerciseName,
        exerciseCategory = pe.performedExercise.exerciseCategory.convert(),
        exerciseTrackingType = pe.performedExercise.exerciseTrackingType.convert(),
        exercisePrimaryMuscles = pe.performedExercise.exercisePrimaryMuscles.map { it.convert<DtoMuscleGroup>() },
        sets = pe.sets.map { toDto(it) },
        supersetGroup = pe.performedExercise.supersetGroup
    )

    private fun toDto(s: WorkoutSetEntity) = WorkoutSet(
        id = uuid(s.id),
        setType = s.setType.convert<DtoSetType>(),
        isCompleted = s.isCompleted,
        orderIndex = s.orderIndex,
        weightKg = s.weightKg,
        reps = s.reps,
        durationSeconds = s.durationSeconds,
        distanceMeters = s.distanceMeters,
        restTakenSeconds = s.restTakenSeconds
    )

    fun toEntities(d: WorkoutSession): SessionAggregate {
        val sessionId = d.id.toString()
        val performed = d.performedExercises.map { pe ->
            PerformedExerciseEntity(
                id = pe.id.toString(),
                workoutSessionId = sessionId,
                exerciseId = pe.exerciseId.toString(),
                orderIndex = pe.orderIndex,
                exerciseName = pe.exerciseName,
                exerciseCategory = pe.exerciseCategory.convert(),
                exerciseTrackingType = pe.exerciseTrackingType.convert(),
                exercisePrimaryMuscles = pe.exercisePrimaryMuscles.map { it.convert() },
                supersetGroup = pe.supersetGroup
            )
        }
        val sets = d.performedExercises.flatMap { pe ->
            pe.sets.map { s ->
                WorkoutSetEntity(
                    id = s.id.toString(),
                    performedExerciseId = pe.id.toString(),
                    weightKg = s.weightKg,
                    reps = s.reps,
                    durationSeconds = s.durationSeconds,
                    distanceMeters = s.distanceMeters,
                    setType = s.setType.convert(),
                    isCompleted = s.isCompleted,
                    restTakenSeconds = s.restTakenSeconds,
                    orderIndex = s.orderIndex
                )
            }
        }
        val session = WorkoutSessionEntity(
            id = sessionId,
            startedAt = d.startedAt,
            endedAt = d.endedAt,
            durationSeconds = d.durationSeconds,
            title = d.title,
            notes = d.notes,
            templateId = d.templateId?.toString(),
            templateTitle = d.templateTitle,
            updatedAt = d.updatedAt,
            deletedAt = d.deletedAt,
            revision = d.revision,
            syncState = SyncState.SYNCED
        )
        return SessionAggregate(session, performed, sets)
    }

    // ---- WorkoutTemplate aggregate ----

    fun toDto(template: WorkoutTemplateEntity, templateExercises: List<TemplateExerciseEntity>) = WorkoutTemplate(
        id = uuid(template.id),
        title = template.title,
        templateExercises = templateExercises.sortedBy { it.orderIndex }.map { te ->
            TemplateExercise(
                id = uuid(te.id),
                exerciseId = uuid(te.exerciseId),
                exerciseName = te.exerciseName,
                exerciseTrackingType = te.exerciseTrackingType.convert(),
                plannedSets = te.plannedSets,
                orderIndex = te.orderIndex,
                plannedReps = te.plannedReps,
                restSeconds = te.restSeconds,
                plannedDurationSeconds = te.plannedDurationSeconds,
                plannedDistanceMeters = te.plannedDistanceMeters,
                plannedWeightKg = te.plannedWeightKg,
                notes = te.notes
            )
        },
        source = template.source.convert(),
        deprecated = template.deprecated,
        updatedAt = template.updatedAt,
        revision = template.revision,
        ownerId = template.ownerId,
        derivedFromId = template.derivedFromId?.let(::uuid),
        deletedAt = template.deletedAt
    )

    fun toEntities(d: WorkoutTemplate): TemplateAggregate {
        val templateId = d.id.toString()
        return TemplateAggregate(
            template = WorkoutTemplateEntity(
                id = templateId,
                title = d.title,
                source = d.source.convert(),
                ownerId = d.ownerId,
                derivedFromId = d.derivedFromId?.toString(),
                deprecated = d.deprecated,
                updatedAt = d.updatedAt,
                deletedAt = d.deletedAt,
                revision = d.revision,
                syncState = SyncState.SYNCED
            ),
            templateExercises = d.templateExercises.map { te ->
                TemplateExerciseEntity(
                    id = te.id.toString(),
                    templateId = templateId,
                    exerciseId = te.exerciseId.toString(),
                    exerciseName = te.exerciseName,
                    exerciseTrackingType = te.exerciseTrackingType.convert(),
                    plannedSets = te.plannedSets,
                    plannedReps = te.plannedReps,
                    orderIndex = te.orderIndex,
                    restSeconds = te.restSeconds,
                    plannedDurationSeconds = te.plannedDurationSeconds,
                    plannedDistanceMeters = te.plannedDistanceMeters,
                    plannedWeightKg = te.plannedWeightKg,
                    notes = te.notes
                )
            }
        )
    }

    // ---- Overlay rows ----

    fun toDto(s: UserExerciseStateEntity) = UserExerciseState(
        exerciseId = uuid(s.exerciseId),
        isHidden = s.isHidden,
        isFavorite = s.isFavorite,
        updatedAt = s.updatedAt,
        revision = s.revision,
        note = s.note,
        defaultRestSeconds = s.defaultRestSeconds
    )

    fun toEntity(d: UserExerciseState) = UserExerciseStateEntity(
        exerciseId = d.exerciseId.toString(),
        isHidden = d.isHidden,
        isFavorite = d.isFavorite,
        note = d.note,
        defaultRestSeconds = d.defaultRestSeconds,
        updatedAt = d.updatedAt,
        revision = d.revision,
        syncState = SyncState.SYNCED
    )

    fun toDto(s: UserTemplateStateEntity) = UserTemplateState(
        templateId = uuid(s.templateId),
        isHidden = s.isHidden,
        isFavorite = s.isFavorite,
        updatedAt = s.updatedAt,
        revision = s.revision
    )

    fun toEntity(d: UserTemplateState) = UserTemplateStateEntity(
        templateId = d.templateId.toString(),
        isHidden = d.isHidden,
        isFavorite = d.isFavorite,
        updatedAt = d.updatedAt,
        revision = d.revision,
        syncState = SyncState.SYNCED
    )

    // ---- Profile ----

    fun toDto(p: UserProfileEntity) = UserProfile(
        uid = p.uid,
        name = p.name,
        email = p.email,
        joinedDate = p.joinedDate,
        updatedAt = p.updatedAt,
        revision = p.revision,
        profileImageUrl = uri(p.profileImageUrl),
        birthDate = p.birthDate,
        weight = p.weight,
        height = p.height,
        gender = p.gender?.convert<DtoGender>(),
        fitnessGoal = p.fitnessGoal?.convert<DtoFitnessGoal>(),
        activityLevel = p.activityLevel?.convert<DtoActivityLevel>(),
        targetWeight = p.targetWeight,
        bio = p.bio,
        deletedAt = p.deletedAt
    )

    fun toEntity(d: UserProfile) = UserProfileEntity(
        uid = d.uid,
        name = d.name,
        email = d.email,
        profileImageUrl = d.profileImageUrl?.toString(),
        birthDate = d.birthDate,
        weight = d.weight,
        height = d.height,
        gender = d.gender?.convert(),
        fitnessGoal = d.fitnessGoal?.convert(),
        activityLevel = d.activityLevel?.convert(),
        targetWeight = d.targetWeight,
        joinedDate = d.joinedDate,
        bio = d.bio,
        updatedAt = d.updatedAt,
        deletedAt = d.deletedAt,
        revision = d.revision,
        syncState = SyncState.SYNCED
    )

    // ---- helpers ----

    private fun uuid(value: String): UUID = UUID.fromString(value)

    /** A stored URL that isn't a valid URI is dropped rather than failing the whole entity. */
    private fun uri(value: String?): URI? = value?.let { runCatching { URI(it) }.getOrNull() }

    private inline fun <reified T : Enum<T>> Enum<*>.convert(): T = enumValueOf(name)
}
