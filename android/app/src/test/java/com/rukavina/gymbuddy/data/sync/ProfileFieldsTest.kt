package com.rukavina.gymbuddy.data.sync

import androidx.room.Room
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import com.rukavina.gymbuddy.data.local.db.DatabaseHolder
import com.rukavina.gymbuddy.data.local.mapper.UserProfileMapper
import com.rukavina.gymbuddy.data.repository.AppPreferencesRepository
import com.rukavina.gymbuddy.data.repository.UserProfileRepository
import com.rukavina.gymbuddy.data.sync.remote.SyncApis
import com.rukavina.gymbuddy.domain.model.ExperienceLevel
import com.rukavina.gymbuddy.domain.model.TrainingDays
import com.rukavina.gymbuddy.domain.model.UserProfile
import com.rukavina.gymbuddy.testutil.MainDispatcherRule
import com.rukavina.gymbuddy.testutil.testPreferencesDataStore
import com.rukavina.gymbuddy.data.repository.BodyWeightRepositoryImpl
import com.rukavina.gymbuddy.ui.you.profile.ProfileViewModel
import com.rukavina.gymbuddy.ui.you.profile.SignInInfo
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** Plan 6 step 05: the new profile fields survive mapping, local saves and sync. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProfileFieldsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
    private lateinit var db: AppDatabase
    private lateinit var holder: DatabaseHolder
    private lateinit var profiles: UserProfileRepository
    private lateinit var store: SyncLocalStore

    private val full = SyncFixtures.profile(UID).copy(
        experienceLevel = ExperienceLevel.EXPERIENCED,
        avatarId = "red-panda",
        trainingDays = TrainingDays.FIVE_TO_SEVEN,
        healthDataConsentVersion = "2026-11",
        healthDataConsentAt = 1_762_000_000_000,
        activePlanId = "01a19f0f-dc18-7975-8b8f-61819042e23a"
    )

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder().setApplicationId("1:1:android:1").setApiKey("test-api-key").setProjectId("test-project").build()
            )
        }
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        holder = DatabaseHolder.fixed(db, owner = UID)
        profiles = UserProfileRepository(holder, OutboxRecorder(holder, clock), clock)
        store = SyncLocalStore(holder, clock)
    }

    @After
    fun tearDown() = db.close()

    private fun assertNewFields(expected: UserProfile, actual: UserProfile) {
        assertEquals(expected.experienceLevel, actual.experienceLevel)
        assertEquals(expected.avatarId, actual.avatarId)
        assertEquals(expected.trainingDays, actual.trainingDays)
        assertEquals(expected.healthDataConsentVersion, actual.healthDataConsentVersion)
        assertEquals(expected.healthDataConsentAt, actual.healthDataConsentAt)
        assertEquals(expected.activePlanId, actual.activePlanId)
    }

    @Test
    fun `room and wire mappers carry every new field`() {
        assertNewFields(full, UserProfileMapper.toDomain(UserProfileMapper.toEntity(full)))
        val wire = SyncDtoMapper.toDto(UserProfileMapper.toEntity(full))
        assertNewFields(full, UserProfileMapper.toDomain(SyncDtoMapper.toEntity(wire)))
    }

    @Test
    fun `saving from the profile screen keeps experience, avatar and consent`() = runBlocking {
        profiles.saveProfile(full)
        val viewModel = ProfileViewModel(
            profiles,
            BodyWeightRepositoryImpl(holder, OutboxRecorder(holder, clock), clock),
            AppPreferencesRepository(testPreferencesDataStore()),
            object : SignInInfo() { override fun read() = Info("", false) },
            clock,
        )

        viewModel.setName("Renamed")
        waitFor { profiles.getProfile(UID)?.name == "Renamed" }

        assertNewFields(full, profiles.getProfile(UID)!!)
    }

    @Test
    fun `the new fields push and come back from a pull`() = runBlocking {
        profiles.saveProfile(full)
        val pushed = mutableListOf<String>()
        val pulledProfile = """{"uid":"$UID","name":"Server","email":"s@example.com","joinedDate":1,"updatedAt":9,"revision":4,""" +
            """"experienceLevel":"NEW","avatarId":"owl","trainingDays":"FOUR","healthDataConsentVersion":"2026-12","healthDataConsentAt":7,""" +
            """"activePlanId":"01a19f0f-dc18-7975-8b8f-000000000002"}"""
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            val isPush = request.url.encodedPath.endsWith("/sync/push")
            val body = if (isPush) {
                pushed += Buffer().also { request.body!!.writeTo(it) }.readUtf8()
                """{"results":[{"entityType":"USER_PROFILE","entityId":"$UID","status":"APPLIED","updatedAt":8,"revision":3}]}"""
            } else {
                """{"userProfile":$pulledProfile,"nextCursor":"c1","hasMore":false}"""
            }
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("ok")
                .body(body.toResponseBody("application/json".toMediaType())).build()
        }.build()

        SyncEngine(store, SyncApis.create("http://canned.invalid/", { "token" }, client), mutex = holder.lock).sync()

        val sent = pushed.single()
        assertTrue(sent, listOf("\"experienceLevel\":\"EXPERIENCED\"", "\"avatarId\":\"red-panda\"", "\"trainingDays\":\"FIVE_TO_SEVEN\"", "\"healthDataConsentVersion\":\"2026-11\"", "\"activePlanId\":\"01a19f0f-dc18-7975-8b8f-61819042e23a\"").all { it in sent })
        val stored = profiles.getProfile(UID)!!
        assertEquals(ExperienceLevel.NEW, stored.experienceLevel)
        assertEquals("owl", stored.avatarId)
        assertEquals(TrainingDays.FOUR, stored.trainingDays)
        assertEquals("2026-12", stored.healthDataConsentVersion)
        assertEquals(7L, stored.healthDataConsentAt)
        assertEquals("01a19f0f-dc18-7975-8b8f-000000000002", stored.activePlanId)
    }

    private suspend fun waitFor(condition: suspend () -> Boolean) {
        repeat(200) {
            if (condition()) return
            Thread.sleep(10)
        }
        throw AssertionError("condition not met")
    }

    private companion object {
        const val UID = "uid-a"
    }
}
