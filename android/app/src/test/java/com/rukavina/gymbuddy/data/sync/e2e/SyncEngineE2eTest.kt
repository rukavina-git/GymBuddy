package com.rukavina.gymbuddy.data.sync.e2e

import com.rukavina.gymbuddy.data.local.entity.ExerciseEntity
import com.rukavina.gymbuddy.data.local.entity.ExerciseVersionEntity
import com.rukavina.gymbuddy.data.local.entity.SyncEntityType
import com.rukavina.gymbuddy.data.local.entity.SyncLogOutcome
import com.rukavina.gymbuddy.data.local.entity.TemplateVersionEntity
import com.rukavina.gymbuddy.data.local.entity.UserExerciseStateEntity
import com.rukavina.gymbuddy.data.local.entity.WorkoutTemplateEntity
import com.rukavina.gymbuddy.data.remote.generated.models.MutationResult
import com.rukavina.gymbuddy.data.sync.PullPageObserver
import com.rukavina.gymbuddy.data.sync.SyncFixtures
import com.rukavina.gymbuddy.domain.model.DifficultyLevel
import com.rukavina.gymbuddy.domain.model.EntitySource
import com.rukavina.gymbuddy.domain.model.ExerciseCategory
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.ExerciseType
import com.rukavina.gymbuddy.domain.model.MuscleGroup
import com.rukavina.gymbuddy.domain.model.SyncState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/**
 * Phase 4 sync scenarios 2-10, end to end: the app's real repositories,
 * outbox and SyncEngine over in-memory Room, talking HTTP to the real
 * backend and its real Postgres. No mocks anywhere on the push/pull
 * path. Each test uses fresh uids so tests never see each other's data.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncEngineE2eTest {

    private val devices = mutableListOf<TestDevice>()

    private fun device(uid: String) = TestDevice(uid).also { devices += it }

    /** Anything stamped after this came from the server, not a device's fixed 2020 clock. */
    private val testStart = Instant.parse("2026-01-01T00:00:00Z").toEpochMilli()

    @Before
    fun requireBackend() = E2eBackend.assumeConfigured()

    @After
    fun tearDown() = devices.forEach { it.close() }

    // ---- Scenario 2 ----

    @Test
    fun `2 - pushing a new entity is APPLIED, adopts the server revision and updatedAt, and clears the outbox`() = runBlocking<Unit> {
        val uid = E2eBackend.newUid("s2")
        val phone = device(uid)
        val exercise = SyncFixtures.exercise("Scenario 2 Curl")
        phone.exercises.createExercise(exercise)
        assertEquals(1, phone.syncDao.outboxCount())
        assertEquals(phone.clock.millis(), phone.syncDao.getExerciseIncludingDeleted(exercise.id)!!.updatedAt)

        val summary = phone.engine().push()

        assertEquals(1, summary.count(MutationResult.Status.APPLIED))
        val local = phone.syncDao.getExerciseIncludingDeleted(exercise.id)!!
        assertEquals(1, local.revision)
        assertTrue("updatedAt must be the server's, not the 2020 device clock", local.updatedAt > testStart)
        assertEquals(SyncState.SYNCED, local.syncState)
        assertEquals(0, phone.syncDao.outboxCount())

        // The adopted values are exactly what the server stored.
        val other = device(uid)
        other.engine().pull()
        val server = other.syncDao.getExerciseIncludingDeleted(exercise.id)!!
        assertEquals(server.revision, local.revision)
        assertEquals(server.updatedAt, local.updatedAt)
    }

    // ---- Scenario 3 ----

    @Test
    fun `3 - a conflicting push is CONFLICT, local state is overwritten with the server's, and the discarded change is logged`() = runBlocking<Unit> {
        val uid = E2eBackend.newUid("s3")
        val phoneA = device(uid)
        val phoneB = device(uid)
        val session = SyncFixtures.session(title = "Original")
        phoneA.sessions.createWorkoutSession(session)
        phoneA.engine().sync()
        phoneB.engine().sync()
        assertEquals(1, phoneB.syncDao.getSessionIncludingDeleted(session.id)!!.revision)

        phoneA.sessions.updateWorkoutSession(session.copy(title = "Edited on A"))
        phoneA.engine().sync()
        phoneB.sessions.updateWorkoutSession(session.copy(title = "Edited on B"))

        val summary = phoneB.engine().sync()

        assertEquals(1, summary.push.count(MutationResult.Status.CONFLICT))
        val local = phoneB.syncDao.getSessionIncludingDeleted(session.id)!!
        assertEquals("Edited on A", local.title)
        assertEquals(2, local.revision)
        assertEquals(SyncState.SYNCED, local.syncState)
        assertEquals(0, phoneB.syncDao.outboxCount())

        val logged = phoneB.store.log().single()
        assertEquals(SyncLogOutcome.CONFLICT, logged.outcome)
        assertEquals(session.id, logged.entityId)
        assertTrue("the discarded edit itself is recorded", logged.discardedPayload!!.contains("Edited on B"))

        // The server never took B's edit.
        phoneA.engine().sync()
        assertEquals("Edited on A", phoneA.syncDao.getSessionIncludingDeleted(session.id)!!.title)
    }

    // ---- Scenario 4 ----

    @Test
    fun `4 - an invalid entity is INVALID, dropped from the outbox, logged, and never retried`() = runBlocking<Unit> {
        val uid = E2eBackend.newUid("s4")
        val phone = device(uid)
        val invalid = SyncFixtures.session(title = "   ") // blank title: the server rejects it
        phone.sessions.createWorkoutSession(invalid)

        val first = phone.engine().push()
        assertEquals(1, first.count(MutationResult.Status.INVALID))
        assertEquals(0, phone.syncDao.outboxCount())
        assertEquals(SyncLogOutcome.INVALID, phone.store.log().single().outcome)
        assertEquals(SyncState.CONFLICTED, phone.syncDao.getSessionIncludingDeleted(invalid.id)!!.syncState)

        val second = phone.engine().push()
        assertEquals("nothing left to send, so no request at all", 0, second.requests)
        assertEquals(1, phone.store.log().size)

        val other = device(uid)
        other.engine().pull()
        assertNull(other.syncDao.getSessionIncludingDeleted(invalid.id))
    }

    // ---- Scenario 5 ----

    @Test
    fun `5 - a full initial pull populates every entity type on a fresh device`() = runBlocking<Unit> {
        val uid = E2eBackend.newUid("s5")
        val phoneA = device(uid)
        val session = SyncFixtures.session(
            performedExercises = listOf(
                SyncFixtures.performedExercise(listOf(SyncFixtures.set(0), SyncFixtures.set(1)), orderIndex = 0),
                SyncFixtures.performedExercise(listOf(SyncFixtures.set(0)), orderIndex = 1),
            )
        )
        val exercise = SyncFixtures.exercise()
        val template = SyncFixtures.template(templateExercises = listOf(SyncFixtures.templateExercise(0), SyncFixtures.templateExercise(1)))
        phoneA.sessions.createWorkoutSession(session)
        phoneA.exercises.createExercise(exercise)
        phoneA.templates.createTemplate(template)
        phoneA.exercises.hideExercise(exercise.id)
        phoneA.exercises.updateExerciseNote(exercise.id, "keep elbows in")
        phoneA.templates.hideTemplate(template.id)
        phoneA.profiles.saveProfile(SyncFixtures.profile(uid, name = "Scenario Five"))
        phoneA.engine().sync()

        val fresh = device(uid)
        val summary = fresh.engine().sync()

        assertEquals(
            mapOf(
                SyncEntityType.WORKOUT_SESSION to 1,
                SyncEntityType.EXERCISE to 1,
                SyncEntityType.WORKOUT_TEMPLATE to 1,
                SyncEntityType.USER_EXERCISE_STATE to 1,
                SyncEntityType.USER_TEMPLATE_STATE to 1,
                SyncEntityType.USER_PROFILE to 1,
            ),
            summary.pull.applied
        )
        // Field-for-field identical to the device that created them
        // (which has itself adopted the server's metadata).
        assertEquals(phoneA.syncDao.getSessionIncludingDeleted(session.id), fresh.syncDao.getSessionIncludingDeleted(session.id))
        assertEquals(
            phoneA.db.workoutSessionDao().getPerformedExercisesWithSets(session.id),
            fresh.db.workoutSessionDao().getPerformedExercisesWithSets(session.id)
        )
        assertEquals(phoneA.syncDao.getExerciseIncludingDeleted(exercise.id), fresh.syncDao.getExerciseIncludingDeleted(exercise.id))
        assertEquals(phoneA.syncDao.getTemplateIncludingDeleted(template.id), fresh.syncDao.getTemplateIncludingDeleted(template.id))
        assertEquals(phoneA.syncDao.getTemplateExercises(template.id), fresh.syncDao.getTemplateExercises(template.id))
        assertEquals(phoneA.db.userExerciseStateDao().getState(exercise.id), fresh.db.userExerciseStateDao().getState(exercise.id))
        assertEquals(phoneA.db.userTemplateStateDao().getState(template.id), fresh.db.userTemplateStateDao().getState(template.id))
        assertEquals("Scenario Five", fresh.profiles.getProfile(uid)!!.name)
        assertEquals(phoneA.syncDao.getProfileIncludingDeleted(uid), fresh.syncDao.getProfileIncludingDeleted(uid))
        assertTrue(fresh.exercises.getHiddenExercises().first().any { it.id == exercise.id })
        assertNotNull(fresh.store.cursor())
    }

    // ---- Scenario 6 ----

    @Test
    fun `6 - a pull killed mid-pagination keeps the last committed cursor and resumes without skipping data`() = runBlocking<Unit> {
        val uid = E2eBackend.newUid("s6")
        val phoneA = device(uid)
        val phoneB = device(uid)
        phoneB.engine().sync() // B has a delta cursor from here on
        val created = (1..5).map { SyncFixtures.session(title = "Paged $it") }
        created.forEach { phoneA.sessions.createWorkoutSession(it) }
        phoneA.engine().push()

        val committedCursors = mutableListOf<String>()
        var sessionsAfterPage0 = -1
        val crashing = phoneB.engine(pageSize = 2, observer = PullPageObserver { pageIndex, nextCursor ->
            if (pageIndex == 1) throw IllegalStateException("simulated process death during page 2")
            committedCursors += nextCursor
            sessionsAfterPage0 = phoneB.sessions.getAllWorkoutSessions().first().size
        })

        val crash = runCatching { crashing.pull() }.exceptionOrNull()

        assertEquals("simulated process death during page 2", crash?.message)
        assertEquals("only page 1's cursor was ever committed", committedCursors.single(), phoneB.store.cursor())
        val afterCrash = phoneB.sessions.getAllWorkoutSessions().first().map { it.id }.toSet()
        assertEquals("page 2's writes rolled back with its cursor", sessionsAfterPage0, afterCrash.size)
        assertTrue(afterCrash.size in 1..4)

        // A fresh engine on the same database - as after a restart.
        val resumed = phoneB.engine(pageSize = 2).pull()

        assertTrue(resumed.pages >= 2)
        val all = phoneB.sessions.getAllWorkoutSessions().first().map { it.id }
        assertEquals(created.map { it.id }.toSet(), all.toSet())
        assertEquals("each session exactly once", all.size, all.toSet().size)
    }

    // ---- Scenario 7 ----

    @Test
    fun `7 - a pulled deletion applies through the same upsert path as any other change`() = runBlocking<Unit> {
        val uid = E2eBackend.newUid("s7")
        val phoneA = device(uid)
        val phoneB = device(uid)
        val session = SyncFixtures.session()
        phoneA.sessions.createWorkoutSession(session)
        phoneA.engine().sync()
        phoneB.engine().sync()
        assertNotNull(phoneB.sessions.getWorkoutSessionById(session.id))

        phoneA.sessions.deleteWorkoutSession(session.id)
        phoneA.engine().sync()
        val summary = phoneB.engine().pull()

        // Counted as an ordinary session apply: there is no separate deletion path.
        assertEquals(mapOf(SyncEntityType.WORKOUT_SESSION to 1), summary.applied)
        val tombstone = phoneB.syncDao.getSessionIncludingDeleted(session.id)!!
        assertNotNull(tombstone.deletedAt)
        assertTrue("deletedAt is the server's stamp", tombstone.deletedAt!! > testStart)
        assertEquals(2, tombstone.revision)
        assertEquals(SyncState.SYNCED, tombstone.syncState)
        assertNull(phoneB.sessions.getWorkoutSessionById(session.id))
        assertFalse(phoneB.sessions.getAllWorkoutSessions().first().any { it.id == session.id })
    }

    // ---- Scenario 8 ----

    @Test
    fun `8 - an expired cursor flushes the outbox, wipes local user data and resyncs from scratch`() = runBlocking<Unit> {
        val uid = E2eBackend.newUid("s8")
        val phoneA = device(uid)
        val phoneB = device(uid)
        val doomed = SyncFixtures.exercise("Doomed")
        val survivor = SyncFixtures.exercise("Survivor")
        phoneA.exercises.createExercise(doomed)
        phoneA.exercises.createExercise(survivor)
        phoneA.engine().sync()
        phoneB.engine().sync() // B's cursor now sits before the deletion below

        phoneA.exercises.deleteExercise(doomed.id)
        phoneA.engine().sync()
        E2eBackend.runRetention() // prunes the tombstone and moves the user's watermark past B's cursor

        // B has an unpushed change when it discovers the expiry.
        val unpushed = SyncFixtures.session(title = "Logged offline on B")
        phoneB.sessions.createWorkoutSession(unpushed)
        val staleCursor = phoneB.store.cursor()

        val summary = phoneB.engine().pull()

        assertTrue(summary.cursorExpired)
        assertEquals(0, phoneB.syncDao.outboxCount())
        val flushed = phoneB.syncDao.getSessionIncludingDeleted(unpushed.id)!!
        assertEquals("the unpushed session reached the server before the wipe and came back in the resync", 1, flushed.revision)
        assertEquals(SyncState.SYNCED, flushed.syncState)
        assertNull("the pruned entity is gone after the wipe", phoneB.syncDao.getExerciseIncludingDeleted(doomed.id))
        assertNotNull(phoneB.syncDao.getExerciseIncludingDeleted(survivor.id))
        assertNotEquals(staleCursor, phoneB.store.cursor())

        // And the new cursor is valid.
        phoneB.engine().pull()
    }

    // ---- Scenario 9 ----

    @Test
    fun `9 - a reference version mismatch replaces DEFAULT rows only and leaves overlay rows untouched`() = runBlocking<Unit> {
        val phone = device(E2eBackend.newUid("s9"))
        val staleDefault = defaultExercise("Retired Movement")
        val custom = SyncFixtures.exercise("My Own")
        phone.db.exerciseDao().insertExercise(staleDefault)
        phone.exercises.createExercise(custom)
        phone.db.exerciseVersionDao().setVersion(ExerciseVersionEntity(version = -1))
        phone.db.workoutTemplateDao().insertTemplate(WorkoutTemplateEntity(id = SyncFixtures.id(), title = "Retired Template", source = EntitySource.DEFAULT))
        phone.db.templateVersionDao().setVersion(TemplateVersionEntity(version = -1))
        val serverLibrary = phone.apis.reference.getReferenceExercises().body()!!
        val keptDefaultId = serverLibrary.exercises.first().id.toString()
        val overlays = listOf(
            UserExerciseStateEntity(exerciseId = staleDefault.id, isHidden = true, note = "orphan-to-be", revision = 3),
            UserExerciseStateEntity(exerciseId = keptDefaultId, isFavorite = true, note = "still here", revision = 5),
            UserExerciseStateEntity(exerciseId = custom.id, isHidden = true, revision = 1),
        )
        overlays.forEach { phone.db.userExerciseStateDao().upsert(it) }

        val summary = phone.engine().syncReferenceData()

        assertTrue(summary.exercisesReplaced)
        assertTrue(summary.templatesReplaced)
        val defaults = phone.db.exerciseDao().getDefaultExercises().first()
        assertEquals(serverLibrary.exercises.filter { !it.deprecated }.map { it.id.toString() }.toSet(), defaults.map { it.id }.toSet())
        assertNull(phone.syncDao.getExerciseIncludingDeleted(staleDefault.id))
        assertNotNull("CUSTOM rows are not reference data", phone.syncDao.getExerciseIncludingDeleted(custom.id))
        overlays.forEach { assertEquals("overlay rows untouched", it, phone.db.userExerciseStateDao().getState(it.exerciseId)) }
        assertEquals(serverLibrary.version, phone.store.exerciseLibraryVersion())
        assertFalse(phone.db.workoutTemplateDao().getDefaultTemplates().first().any { it.template.title == "Retired Template" })
        assertTrue(phone.db.workoutTemplateDao().getDefaultTemplates().first().isNotEmpty())

        // Versions now match: nothing is replaced again.
        val again = phone.engine().syncReferenceData()
        assertFalse(again.exercisesReplaced)
        assertFalse(again.templatesReplaced)
    }

    private fun defaultExercise(name: String) = ExerciseEntity(
        id = SyncFixtures.id(),
        name = name,
        primaryMuscles = listOf(MuscleGroup.CHEST),
        secondaryMuscles = emptyList(),
        description = null,
        instructions = emptyList(),
        tips = emptyList(),
        difficulty = DifficultyLevel.BEGINNER,
        equipmentNeeded = emptyList(),
        category = ExerciseCategory.STRENGTH,
        exerciseType = ExerciseType.COMPOUND,
        trackingType = ExerciseTrackingType.WEIGHT_REPS,
        videoUrl = null,
        thumbnailUrl = null,
        source = EntitySource.DEFAULT,
        ownerId = null,
        derivedFromId = null
    )

    // ---- Scenario 10 ----

    @Test
    fun `10 - pushing a session with fewer sets removes the extras on other devices after their next pull`() = runBlocking<Unit> {
        val uid = E2eBackend.newUid("s10")
        val phoneA = device(uid)
        val phoneB = device(uid)
        val sets = listOf(SyncFixtures.set(0), SyncFixtures.set(1), SyncFixtures.set(2))
        val performed = SyncFixtures.performedExercise(sets)
        val session = SyncFixtures.session(performedExercises = listOf(performed))
        phoneA.sessions.createWorkoutSession(session)
        phoneA.engine().sync()
        phoneB.engine().sync()
        assertEquals(sets.map { it.id }, setIdsOn(phoneB, session.id))

        phoneA.sessions.updateWorkoutSession(session.copy(performedExercises = listOf(performed.copy(sets = listOf(sets[0])))))
        phoneA.engine().sync()
        phoneB.engine().sync()

        assertEquals(listOf(sets[0].id), setIdsOn(phoneB, session.id))
        assertEquals(
            "the removed sets are gone from the table, not just hidden",
            1,
            phoneB.db.query("SELECT COUNT(*) FROM workout_sets", null).use { it.moveToFirst(); it.getInt(0) }
        )
    }

    private suspend fun setIdsOn(device: TestDevice, sessionId: String) =
        device.db.workoutSessionDao().getPerformedExercisesWithSets(sessionId).flatMap { pe -> pe.sets.map { it.id } }
}
