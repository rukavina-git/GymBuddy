package com.rukavina.gymbuddy.data.repository

import androidx.room.Room
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import com.rukavina.gymbuddy.data.local.db.DatabaseHolder
import com.rukavina.gymbuddy.data.local.entity.OutboxOperation
import com.rukavina.gymbuddy.data.local.entity.SyncEntityType
import com.rukavina.gymbuddy.data.sync.OutboxRecorder
import com.rukavina.gymbuddy.data.sync.SyncFixtures
import com.rukavina.gymbuddy.data.sync.SyncLocalStore
import com.rukavina.gymbuddy.domain.model.BodyWeightEntry
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
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** Plan 6 step 04: body weight writes, the outbox, and decision 1A (newest sets the profile weight). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BodyWeightRepositoryImplTest {

    private val clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
    private lateinit var db: AppDatabase
    private lateinit var holder: DatabaseHolder
    private lateinit var repository: BodyWeightRepositoryImpl
    private lateinit var profiles: UserProfileRepository
    private lateinit var store: SyncLocalStore

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        holder = DatabaseHolder.fixed(db, owner = "uid-a")
        val outbox = OutboxRecorder(holder, clock)
        repository = BodyWeightRepositoryImpl(holder, outbox, clock)
        profiles = UserProfileRepository(holder, outbox, clock)
        store = SyncLocalStore(holder, clock)
    }

    @After
    fun tearDown() = db.close()

    private fun entry(id: String, measuredAt: Long, kg: Float) = BodyWeightEntry(id, measuredAt, kg)

    @Test
    fun `saving queues the entry and lists it newest first`() = runBlocking {
        repository.save(entry("00000000-0000-7000-8000-000000000001", 1_000, 80f))
        repository.save(entry("00000000-0000-7000-8000-000000000002", 2_000, 79f))

        assertEquals(listOf(79f, 80f), repository.observeEntries().first().map { it.weightKg })
        assertEquals(2, store.pendingCountsByType()[SyncEntityType.BODY_WEIGHT_ENTRY])
    }

    @Test
    fun `the newest entry sets the profile weight and queues the profile`() = runBlocking {
        profiles.saveProfile(SyncFixtures.profile("uid-a").copy(weight = 85f))
        db.syncDao().wipeOutbox()

        repository.save(entry("00000000-0000-7000-8000-000000000001", 5_000, 81.5f))

        assertEquals(81.5f, profiles.getProfile("uid-a")!!.weight)
        val pending = store.outbox().map { it.entityType }.toSet()
        assertEquals(setOf(SyncEntityType.BODY_WEIGHT_ENTRY, SyncEntityType.USER_PROFILE), pending)
    }

    @Test
    fun `an older entry leaves the profile weight alone`() = runBlocking {
        profiles.saveProfile(SyncFixtures.profile("uid-a").copy(weight = 85f))
        repository.save(entry("00000000-0000-7000-8000-000000000002", 5_000, 81.5f))
        db.syncDao().wipeOutbox()

        repository.save(entry("00000000-0000-7000-8000-000000000001", 1_000, 90f))

        assertEquals(81.5f, profiles.getProfile("uid-a")!!.weight)
        assertEquals(listOf(SyncEntityType.BODY_WEIGHT_ENTRY), store.outbox().map { it.entityType })
    }

    @Test
    fun `delete tombstones the entry and queues a DELETE`() = runBlocking {
        val id = "00000000-0000-7000-8000-000000000001"
        repository.save(entry(id, 1_000, 80f))

        repository.delete(id)

        assertTrue(repository.observeEntries().first().isEmpty())
        assertEquals(clock.millis(), db.bodyWeightDao().getIncludingDeleted(id)!!.deletedAt)
        assertEquals(OutboxOperation.DELETE, store.outbox().single().operation)
    }

    @Test
    fun `the logout wipe removes entries`() = runBlocking {
        repository.save(entry("00000000-0000-7000-8000-000000000001", 1_000, 80f))

        store.wipeEverythingForLogout()

        assertNull(db.bodyWeightDao().getNewest())
    }
}
