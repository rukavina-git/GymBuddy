package com.rukavina.gymbuddy.data.repository

import androidx.room.Room
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import com.rukavina.gymbuddy.data.local.db.DatabaseHolder
import com.rukavina.gymbuddy.data.local.entity.SyncEntityType
import com.rukavina.gymbuddy.data.local.seeder.WorkoutTemplateSeeder
import com.rukavina.gymbuddy.data.sync.OutboxRecorder
import com.rukavina.gymbuddy.data.sync.SyncFixtures
import com.rukavina.gymbuddy.data.sync.SyncLocalStore
import com.rukavina.gymbuddy.domain.model.ExperienceLevel
import com.rukavina.gymbuddy.domain.model.TrainingDays
import com.rukavina.gymbuddy.data.remote.generated.models.StarterPlan as DtoStarterPlan
import com.rukavina.gymbuddy.data.remote.generated.models.TrainingDays as DtoTrainingDays
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
import java.util.UUID

/** Plan 6 step 06: starter plans load with the template library; the chosen plan lives on the profile. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StarterPlanRepositoryTest {

    private val clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
    private val context = RuntimeEnvironment.getApplication()
    private lateinit var db: AppDatabase
    private lateinit var holder: DatabaseHolder
    private lateinit var plans: StarterPlanRepository
    private lateinit var profiles: UserProfileRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        holder = DatabaseHolder.fixed(db, owner = UID)
        plans = StarterPlanRepository(holder, clock)
        profiles = UserProfileRepository(holder, OutboxRecorder(holder, clock), clock)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `a first install seeds the 3 starter plans, pointing at live bundled templates`() = runBlocking {
        WorkoutTemplateSeeder(holder).seedIfNeeded(context)

        val loaded = plans.observePlans().first()
        assertEquals(listOf("Full Body", "Upper / Lower", "Push / Pull / Legs"), loaded.map { it.title })
        assertEquals(
            listOf(TrainingDays.ONE_TO_THREE, TrainingDays.FOUR, TrainingDays.FIVE_TO_SEVEN),
            loaded.map { it.suggestedFor }
        )
        val dao = db.workoutTemplateDao()
        val upperLower = loaded[1].templateIds.map { dao.getTemplateById(it)!!.template.title }
        assertEquals(listOf("Upper A", "Lower A", "Upper B", "Lower B"), upperLower)
        loaded.flatMap { it.templateIds }.forEach { assertFalse(dao.getTemplateById(it)!!.template.deprecated) }
        db.query("SELECT COUNT(*) FROM workout_templates WHERE deprecated = 1", null).use { c ->
            c.moveToFirst()
            assertEquals(13, c.getInt(0))
        }
        // The rule onboarding will use picks from what was seeded.
        assertEquals("Upper / Lower", com.rukavina.gymbuddy.domain.usecase.template.StarterPlans
            .suggestPlan(loaded, null, ExperienceLevel.SOME)?.title)
    }

    @Test
    fun `a reference refresh replaces the plans`() = runBlocking {
        WorkoutTemplateSeeder(holder).seedIfNeeded(context)
        val planId = UUID.randomUUID()
        val t1 = UUID.randomUUID()
        val t2 = UUID.randomUUID()

        SyncLocalStore(holder, clock).replaceDefaultTemplates(
            version = 7,
            templates = emptyList(),
            plans = listOf(
                DtoStarterPlan(id = planId, title = "New", templateIds = listOf(t2, t1), deprecated = false, suggestedFor = DtoTrainingDays.FOUR)
            )
        )

        val loaded = plans.getPlans().single()
        assertEquals(planId.toString(), loaded.id)
        assertEquals(listOf(t2.toString(), t1.toString()), loaded.templateIds)
        assertEquals(TrainingDays.FOUR, loaded.suggestedFor)
        assertEquals(7, db.templateVersionDao().getCurrentVersion()!!.version)
    }

    @Test
    fun `setting the plan keeps the other profile fields and queues the profile`() = runBlocking {
        val stored = SyncFixtures.profile(UID).copy(experienceLevel = ExperienceLevel.NEW, avatarId = "owl", trainingDays = TrainingDays.FOUR)
        profiles.saveProfile(stored)
        db.syncDao().wipeOutbox()

        assertTrue(plans.setActivePlan(PLAN_ID))

        val after = profiles.getProfile(UID)!!
        assertEquals(PLAN_ID, after.activePlanId)
        assertEquals(stored.copy(activePlanId = PLAN_ID, updatedAt = after.updatedAt, syncState = after.syncState, revision = after.revision), after)
        assertNotNull(db.syncDao().getOutboxEntry(SyncEntityType.USER_PROFILE, UID))

        assertTrue(plans.setActivePlan(null))
        assertNull(profiles.getProfile(UID)!!.activePlanId)
    }

    @Test
    fun `setting the plan without a profile writes nothing`() = runBlocking {
        assertFalse(plans.setActivePlan(PLAN_ID))
        assertTrue(db.syncDao().getOutbox().isEmpty())
    }

    private companion object {
        const val UID = "uid-a"
        const val PLAN_ID = "01a19f0f-dc18-7975-8b8f-61819042e23a"
    }
}
