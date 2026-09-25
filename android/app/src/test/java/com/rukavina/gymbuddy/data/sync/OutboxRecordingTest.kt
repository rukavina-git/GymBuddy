package com.rukavina.gymbuddy.data.sync

import androidx.room.Room
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import com.rukavina.gymbuddy.data.local.entity.OutboxOperation
import com.rukavina.gymbuddy.data.local.entity.SyncEntityType
import com.rukavina.gymbuddy.data.repository.ExerciseRepositoryImpl
import com.rukavina.gymbuddy.data.repository.UserProfileRepository
import com.rukavina.gymbuddy.data.repository.WorkoutSessionRepositoryImpl
import com.rukavina.gymbuddy.data.repository.WorkoutTemplateRepositoryImpl
import com.rukavina.gymbuddy.domain.model.EntitySource
import com.rukavina.gymbuddy.domain.model.SyncState
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Every local write to a synced entity must leave exactly one outbox row
 * (entity type, id, operation), written in the same transaction as the
 * entity. DEFAULT reference rows must never be queued.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OutboxRecordingTest {

    private lateinit var db: AppDatabase
    private val clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
    private lateinit var sessions: WorkoutSessionRepositoryImpl
    private lateinit var exercises: ExerciseRepositoryImpl
    private lateinit var templates: WorkoutTemplateRepositoryImpl
    private lateinit var profiles: UserProfileRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(org.robolectric.RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val outbox = OutboxRecorder(db, clock)
        sessions = WorkoutSessionRepositoryImpl(db.workoutSessionDao(), outbox, clock)
        exercises = ExerciseRepositoryImpl(db.exerciseDao(), db.userExerciseStateDao(), outbox, clock)
        templates = WorkoutTemplateRepositoryImpl(db.workoutTemplateDao(), db.userTemplateStateDao(), outbox, clock)
        profiles = UserProfileRepository(db.userProfileDao(), outbox, clock)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun outbox() = db.syncDao().getOutbox()

    @Test
    fun `each synced entity type is queued with its type, id and operation`() = runBlocking {
        val session = SyncFixtures.session()
        val exercise = SyncFixtures.exercise()
        val template = SyncFixtures.template()
        sessions.createWorkoutSession(session)
        exercises.createExercise(exercise)
        templates.createTemplate(template)
        exercises.hideExercise(exercise.id)
        templates.hideTemplate(template.id)
        profiles.saveProfile(SyncFixtures.profile("uid-1"))

        val queued = outbox().associate { (it.entityType to it.entityId) to it.operation }
        assertEquals(
            mapOf(
                (SyncEntityType.WORKOUT_SESSION to session.id) to OutboxOperation.UPSERT,
                (SyncEntityType.EXERCISE to exercise.id) to OutboxOperation.UPSERT,
                (SyncEntityType.WORKOUT_TEMPLATE to template.id) to OutboxOperation.UPSERT,
                (SyncEntityType.USER_EXERCISE_STATE to exercise.id) to OutboxOperation.UPSERT,
                (SyncEntityType.USER_TEMPLATE_STATE to template.id) to OutboxOperation.UPSERT,
                (SyncEntityType.USER_PROFILE to "uid-1") to OutboxOperation.UPSERT,
            ),
            queued
        )
    }

    @Test
    fun `repeated writes to one entity coalesce into one row with a bumped changeVersion`() = runBlocking {
        val session = SyncFixtures.session()
        sessions.createWorkoutSession(session)
        sessions.updateWorkoutSession(session.copy(title = "Edit 1"))
        sessions.updateWorkoutSession(session.copy(title = "Edit 2"))

        val row = outbox().single()
        assertEquals(3L, row.changeVersion)
        assertEquals(OutboxOperation.UPSERT, row.operation)
    }

    @Test
    fun `a delete is queued as DELETE and leaves a PENDING tombstone to push`() = runBlocking {
        val exercise = SyncFixtures.exercise()
        exercises.createExercise(exercise)
        exercises.deleteExercise(exercise.id)

        assertEquals(OutboxOperation.DELETE, outbox().single().operation)
        val tombstone = db.syncDao().getExerciseIncludingDeleted(exercise.id)!!
        assertEquals(clock.millis(), tombstone.deletedAt)
        assertEquals(SyncState.PENDING, tombstone.syncState)
    }

    @Test
    fun `DEFAULT exercises are never queued`() = runBlocking {
        exercises.createExercise(SyncFixtures.exercise().copy(source = EntitySource.DEFAULT))
        assertEquals(emptyList<Any>(), outbox())
    }

    @Test
    fun `unhide all queues every row it changes`() = runBlocking {
        val a = SyncFixtures.exercise()
        val b = SyncFixtures.exercise()
        exercises.hideExercise(a.id)
        exercises.hideExercise(b.id)
        db.syncDao().wipeOutbox()

        exercises.unhideAllExercises()

        assertEquals(setOf(a.id, b.id), outbox().map { it.entityId }.toSet())
    }

    @Test
    fun `a write keeps the stored revision, not the caller's stale copy`() = runBlocking {
        val session = SyncFixtures.session()
        sessions.createWorkoutSession(session)
        // As if a push had been APPLIED at revision 4.
        db.syncDao().setSessionSyncMeta(session.id, revision = 4, updatedAt = 99L, syncState = SyncState.SYNCED)

        sessions.updateWorkoutSession(session.copy(title = "Edited from a stale screen", revision = 1))

        val stored = db.syncDao().getSessionIncludingDeleted(session.id)!!
        assertEquals(4, stored.revision)
        assertEquals(SyncState.PENDING, stored.syncState)
    }

    @Test
    fun `a write that fails inside the transaction leaves no outbox row`() = runBlocking {
        // Updating a session that was never created: the parent UPDATE
        // matches nothing, so inserting its performed exercises violates
        // their foreign key mid-transaction.
        val neverCreated = SyncFixtures.session()

        val result = runCatching { sessions.updateWorkoutSession(neverCreated) }

        assertEquals(true, result.isFailure)
        assertNull(db.syncDao().getOutboxEntry(SyncEntityType.WORKOUT_SESSION, neverCreated.id))
    }
}
