package com.rukavina.gymbuddy.data.sync

import androidx.room.Room
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import com.rukavina.gymbuddy.data.local.db.DatabaseHolder
import com.rukavina.gymbuddy.data.local.entity.SyncEntityType
import com.rukavina.gymbuddy.data.repository.BodyWeightRepositoryImpl
import com.rukavina.gymbuddy.data.sync.remote.SyncApis
import com.rukavina.gymbuddy.domain.model.BodyWeightEntry
import com.rukavina.gymbuddy.domain.model.SyncState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
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

/** Plan 6 step 04: a body weight entry pushes and pulls through the real engine against a canned server. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BodyWeightSyncTest {

    private val clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
    private lateinit var db: AppDatabase
    private lateinit var holder: DatabaseHolder
    private lateinit var store: SyncLocalStore
    private lateinit var repository: BodyWeightRepositoryImpl
    private val pushBodies = mutableListOf<String>()
    private var pullBody = """{"nextCursor":"c1","hasMore":false}"""

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        holder = DatabaseHolder.fixed(db, owner = "uid-a")
        store = SyncLocalStore(holder, clock)
        repository = BodyWeightRepositoryImpl(holder, OutboxRecorder(holder, clock), clock)
    }

    @After
    fun tearDown() = db.close()

    private fun engine(): SyncEngine {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            val push = request.url.encodedPath.endsWith("/sync/push")
            val body = if (push) {
                val sent = Buffer().also { request.body!!.writeTo(it) }.readUtf8()
                pushBodies += sent
                """{"results":[{"entityType":"BODY_WEIGHT_ENTRY","entityId":"$ID","status":"APPLIED","updatedAt":42,"revision":1}]}"""
            } else {
                pullBody
            }
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("ok")
                .body(body.toResponseBody("application/json".toMediaType())).build()
        }.build()
        return SyncEngine(store, SyncApis.create("http://canned.invalid/", { "token" }, client), mutex = holder.lock)
    }

    @Test
    fun `a saved entry is pushed and adopts the server revision`() = runBlocking {
        repository.save(BodyWeightEntry(ID, 1_700_000_000_000, 80.5f))

        engine().sync()

        val sent = pushBodies.single()
        assertTrue(sent, sent.contains("\"bodyWeightEntries\"") && sent.contains(ID) && sent.contains("80.5"))
        val row = db.bodyWeightDao().getIncludingDeleted(ID)!!
        assertEquals(1, row.revision)
        assertEquals(42L, row.updatedAt)
        assertEquals(SyncState.SYNCED, row.syncState)
        assertEquals(0, store.outboxCount())
    }

    @Test
    fun `a pulled entry and its tombstone are applied`() = runBlocking {
        pullBody = """{"bodyWeightEntries":[{"id":"$ID","measuredAt":5,"weightKg":77.25,"updatedAt":9,"revision":3,"deletedAt":null}],"nextCursor":"c1","hasMore":false}"""
        engine().sync()
        assertEquals(listOf(77.25f), repository.observeEntries().first().map { it.weightKg })

        pullBody = """{"bodyWeightEntries":[{"id":"$ID","measuredAt":5,"weightKg":77.25,"updatedAt":10,"revision":4,"deletedAt":10}],"nextCursor":"c2","hasMore":false}"""
        engine().sync()
        assertTrue(repository.observeEntries().first().isEmpty())
        assertEquals(4, db.bodyWeightDao().getRevision(ID))
        assertEquals(0, store.pendingCountsByType()[SyncEntityType.BODY_WEIGHT_ENTRY] ?: 0)
    }

    private companion object {
        const val ID = "0190a7e2-0000-7000-8000-000000000001"
    }
}
