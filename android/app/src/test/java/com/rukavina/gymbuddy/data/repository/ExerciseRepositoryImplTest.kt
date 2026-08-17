package com.rukavina.gymbuddy.data.repository

import androidx.room.Room
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import com.rukavina.gymbuddy.domain.model.DifficultyLevel
import com.rukavina.gymbuddy.domain.model.Exercise
import com.rukavina.gymbuddy.domain.model.ExerciseCategory
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.ExerciseType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExerciseRepositoryImplTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ExerciseRepositoryImpl
    private val fixedClock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(org.robolectric.RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ExerciseRepositoryImpl(db.exerciseDao(), db.userExerciseStateDao(), fixedClock)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun exercise(id: String, name: String = "Bench Press") = Exercise(
        id = id,
        name = name,
        primaryMuscles = emptyList(),
        secondaryMuscles = emptyList(),
        difficulty = DifficultyLevel.INTERMEDIATE,
        equipmentNeeded = emptyList(),
        category = ExerciseCategory.STRENGTH,
        exerciseType = ExerciseType.COMPOUND,
        trackingType = ExerciseTrackingType.WEIGHT_REPS
    )

    @Test
    fun `createExercise stamps updatedAt from the injected Clock, not the system clock`() = runBlocking {
        repository.createExercise(exercise("ex-1"))

        val read = repository.getExerciseById("ex-1")!!
        assertEquals(fixedClock.millis(), read.updatedAt)
    }

    @Test
    fun `deleteExercise tombstones instead of removing the row`() = runBlocking {
        repository.createExercise(exercise("ex-1"))

        repository.deleteExercise("ex-1")

        assertNull(repository.getExerciseById("ex-1"))
        db.openHelper.readableDatabase.query(
            "SELECT deletedAt FROM exercises WHERE id = 'ex-1'"
        ).use { cursor ->
            assertTrue("the row must still exist after a tombstone delete", cursor.moveToFirst())
            assertEquals(fixedClock.millis(), cursor.getLong(0))
        }
    }

    @Test
    fun `getAllExercises excludes deleted exercises but keeps other live exercises`() = runBlocking {
        repository.createExercise(exercise("ex-1", name = "Bench Press"))
        repository.createExercise(exercise("ex-2", name = "Squat"))

        repository.deleteExercise("ex-1")

        val all = repository.getAllExercises().first()
        assertEquals(listOf("ex-2"), all.map { it.id })
    }

    @Test
    fun `hideExercise excludes it from getAllExercises and unhideExercise restores it`() = runBlocking {
        repository.createExercise(exercise("ex-1"))

        repository.hideExercise("ex-1")
        assertTrue(repository.getAllExercises().first().none { it.id == "ex-1" })
        assertEquals(listOf("ex-1"), repository.getHiddenExercises().first().map { it.id })

        repository.unhideExercise("ex-1")
        assertEquals(listOf("ex-1"), repository.getAllExercises().first().map { it.id })
    }

    @Test
    fun `unhideAllExercises clears every hidden flag at once`() = runBlocking {
        repository.createExercise(exercise("ex-1"))
        repository.createExercise(exercise("ex-2"))
        repository.hideExercise("ex-1")
        repository.hideExercise("ex-2")

        repository.unhideAllExercises()

        assertTrue(repository.getHiddenExercises().first().isEmpty())
    }

    @Test
    fun `updateExerciseNote persists a note and getExerciseNote reads it back, independent of hidden state`() = runBlocking {
        repository.createExercise(exercise("ex-1"))

        repository.updateExerciseNote("ex-1", "Elbows tucked")

        assertEquals("Elbows tucked", repository.getExerciseNote("ex-1"))
    }

    @Test
    fun `updateExercise re-stamps updatedAt from the Clock at the time of the call`() = runBlocking {
        repository.createExercise(exercise("ex-1"))

        val laterClock = Clock.fixed(fixedClock.instant().plusSeconds(3600), ZoneOffset.UTC)
        val laterRepository = ExerciseRepositoryImpl(db.exerciseDao(), db.userExerciseStateDao(), laterClock)
        laterRepository.updateExercise(exercise("ex-1", name = "Bench Press (edited)"))

        val read = repository.getExerciseById("ex-1")!!
        assertEquals("Bench Press (edited)", read.name)
        assertEquals(laterClock.millis(), read.updatedAt)
    }
}
