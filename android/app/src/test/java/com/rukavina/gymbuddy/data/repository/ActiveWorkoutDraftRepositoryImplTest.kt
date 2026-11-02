package com.rukavina.gymbuddy.data.repository

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import com.rukavina.gymbuddy.data.local.entity.ActiveWorkoutDraftEntity
import com.rukavina.gymbuddy.domain.model.ActiveWorkoutDraft
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.testutil.MutableClock
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ActiveWorkoutDraftRepositoryImplTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ActiveWorkoutDraftRepositoryImpl

    private val draft = ActiveWorkoutDraft(
        title = "Push Day",
        templateId = "template-1",
        templateTitle = "Push Day",
        startedAt = 1_000L,
        elapsedSeconds = 600L,
        isTimerRunning = true,
        savedAt = 2_000L,
        exercises = listOf(
            ActiveWorkoutDraft.Exercise(
                id = "ex-0",
                exerciseId = "ex-bench",
                exerciseName = "Bench Press",
                trackingType = ExerciseTrackingType.WEIGHT_REPS,
                plannedSets = 1,
                plannedReps = 8,
                restSeconds = null,
                sets = listOf(
                    ActiveWorkoutDraft.Set(
                        id = "set-0", setNumber = 1, reps = "8", weight = "82.5",
                        duration = "", distance = "", notes = "felt good", isCompleted = false
                    )
                )
            )
        )
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = ActiveWorkoutDraftRepositoryImpl(db.activeWorkoutDraftDao(), MutableClock())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `a saved draft loads back unchanged`() = runTest {
        repository.save(draft)
        assertEquals(draft, repository.load())
    }

    @Test
    fun `saving again replaces the single draft`() = runTest {
        repository.save(draft)
        repository.save(draft.copy(title = "Leg Day"))
        assertEquals("Leg Day", repository.load()?.title)
    }

    @Test
    fun `clear deletes the draft`() = runTest {
        repository.save(draft)
        repository.clear()
        assertNull(repository.load())
    }

    @Test
    fun `an unreadable draft loads as null and is deleted`() = runTest {
        db.activeWorkoutDraftDao().upsert(ActiveWorkoutDraftEntity(payloadJson = "{\"title\":1}", updatedAt = 0))
        assertNull(repository.load())
        assertNull(db.activeWorkoutDraftDao().get())
    }

    @Test
    fun `wiping local user data deletes the draft`() = runTest {
        repository.save(draft)
        db.syncDao().wipeLocalUserData()
        assertNull(repository.load())
    }
}
