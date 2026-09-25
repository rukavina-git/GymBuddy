package com.rukavina.gymbuddy.data.sync.e2e

import androidx.room.Room
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import com.rukavina.gymbuddy.data.repository.ExerciseRepositoryImpl
import com.rukavina.gymbuddy.data.repository.UserProfileRepository
import com.rukavina.gymbuddy.data.repository.WorkoutSessionRepositoryImpl
import com.rukavina.gymbuddy.data.repository.WorkoutTemplateRepositoryImpl
import com.rukavina.gymbuddy.data.sync.OutboxRecorder
import com.rukavina.gymbuddy.data.sync.PullPageObserver
import com.rukavina.gymbuddy.data.sync.SyncEngine
import com.rukavina.gymbuddy.data.sync.SyncLocalStore
import com.rukavina.gymbuddy.data.sync.remote.SyncApis
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.junit.Assume
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * The real backend these tests run against: backend/'s bootTestRun
 * (E2eBackendApplication) over docker/compose.yaml's Postgres. Same code
 * as production except for test-key token verification. See
 * docs/STATUS_2026-09-26.md §Phase 4 for how to run it.
 *
 * Tests skip unless `-Pe2eBaseUrl=http://localhost:8080` is passed to
 * Gradle, so an ordinary unit-test run doesn't need a server. If it is
 * passed and the server isn't reachable, they fail.
 */
object E2eBackend {
    val baseUrl: String = System.getProperty("gymbuddy.e2e.baseUrl").orEmpty()
    private val http = OkHttpClient()
    private val tokens = ConcurrentHashMap<String, String>()

    fun assumeConfigured() {
        Assume.assumeTrue("sync e2e tests need -Pe2eBaseUrl (see E2eBackend)", baseUrl.isNotBlank())
    }

    fun newUid(label: String) = "e2e-$label-${UUID.randomUUID()}"

    fun token(uid: String): String = tokens.getOrPut(uid) {
        val body = post("/e2e/token?uid=$uid")
        JSONObject(body).getString("token")
    }

    /** Runs the server's tombstone retention now (the e2e server's window is 0 days). */
    fun runRetention(): String = post("/e2e/retention")

    private fun post(path: String): String {
        val request = Request.Builder().url(baseUrl.trimEnd('/') + path).post(ByteArray(0).toRequestBody()).build()
        http.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "POST $path -> HTTP ${response.code}" }
            return response.body.string()
        }
    }
}

/**
 * One simulated phone: its own in-memory Room database, the app's real
 * repositories and sync engine, signed in as [uid] against the real
 * backend. Two TestDevices with the same uid are two devices of one user.
 *
 * Local clocks are fixed in 2020, so any timestamp that later looks
 * current can only have come from the server.
 */
class TestDevice(val uid: String) {
    val clock: Clock = Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC)
    val db: AppDatabase = Room.inMemoryDatabaseBuilder(org.robolectric.RuntimeEnvironment.getApplication(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val outbox = OutboxRecorder(db, clock)
    val sessions = WorkoutSessionRepositoryImpl(db.workoutSessionDao(), outbox, clock)
    val exercises = ExerciseRepositoryImpl(db.exerciseDao(), db.userExerciseStateDao(), outbox, clock)
    val templates = WorkoutTemplateRepositoryImpl(db.workoutTemplateDao(), db.userTemplateStateDao(), outbox, clock)
    val profiles = UserProfileRepository(db.userProfileDao(), outbox, clock)
    val store = SyncLocalStore(db, clock)
    val apis: SyncApis = SyncApis.create(E2eBackend.baseUrl, { E2eBackend.token(uid) })

    val syncDao get() = db.syncDao()

    fun engine(pageSize: Int = SyncEngine.DEFAULT_PAGE_SIZE, observer: PullPageObserver = PullPageObserver { _, _ -> }) =
        SyncEngine(store, apis, pageSize = pageSize, pageObserver = observer)

    fun close() = db.close()
}
