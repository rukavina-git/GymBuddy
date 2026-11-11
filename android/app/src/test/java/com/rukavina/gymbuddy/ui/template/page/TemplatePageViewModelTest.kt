package com.rukavina.gymbuddy.ui.template.page

import androidx.lifecycle.SavedStateHandle
import com.rukavina.gymbuddy.domain.model.EntitySource
import com.rukavina.gymbuddy.testutil.FakeTemplateSources
import com.rukavina.gymbuddy.testutil.FakeWorkoutTemplateRepository
import com.rukavina.gymbuddy.testutil.FixedIdGenerator
import com.rukavina.gymbuddy.testutil.MainDispatcherRule
import com.rukavina.gymbuddy.testutil.MutableClock
import com.rukavina.gymbuddy.testutil.TemplateFixtures.template
import com.rukavina.gymbuddy.ui.template.PendingTemplateDeletes
import com.rukavina.gymbuddy.ui.template.TemplateDeleteViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TemplatePageViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val starter = template("full", "Full Body", source = EntitySource.DEFAULT)
    private val mine = template("mine", "Push A")
    private val sources = FakeTemplateSources(templates = listOf(starter, mine, template("taken", "Full Body (copy)")))
    private val repository = FakeWorkoutTemplateRepository()
    private val pending = PendingTemplateDeletes()
    private val ids = FixedIdGenerator("c1", "c2", "c3")
    private val name = { title: String, n: Int -> if (n == 1) "$title (copy)" else "$title (copy $n)" }

    private fun vm(id: String) = TemplatePageViewModel(
        SavedStateHandle(mapOf(TemplatePageViewModel.ARG_TEMPLATE_ID to id)),
        sources, repository, pending, ids, MutableClock(),
    )

    @Test
    fun starterCopyIsCustomWithNewIdsFreeNameAndOpensEditor() = runTest {
        val vm = vm("full")
        vm.copy(edit = true, name = name)
        assertEquals(TemplatePageEvent.Open("c1", edit = true), vm.events.first())
        val copy = repository.created.single()
        assertEquals("c1", copy.id)
        assertEquals("Full Body (copy 2)", copy.title)
        assertEquals(EntitySource.CUSTOM, copy.source)
        assertEquals("full", copy.derivedFromId)
        assertEquals(listOf("c2", "c3"), copy.templateExercises.map { it.id })
    }

    @Test
    fun duplicateOfYoursKeepsNoDerivedIdAndStaysOnPage() = runTest {
        val vm = vm("mine")
        vm.copy(edit = false, name = name)
        assertEquals(TemplatePageEvent.Open("c1", edit = false), vm.events.first())
        assertEquals("Push A (copy)", repository.created.single().title)
        assertNull(repository.created.single().derivedFromId)
    }

    @Test
    fun hideHidesAndLeaves() = runTest {
        repository.createTemplate(starter)
        val vm = vm("full")
        vm.hide()
        assertEquals(TemplatePageEvent.Hidden, vm.events.first())
        assertEquals(listOf("full"), repository.getHiddenTemplates().first().map { it.id })
    }

    @Test
    fun deleteWaitsForUndoThenCommits() = runTest {
        val vm = vm("mine")
        backgroundScope.launch { vm.uiState.collect {} }
        vm.uiState.first { it.line != null }
        vm.delete()
        assertEquals(TemplatePageEvent.Deleted("mine", "Push A"), vm.events.first())
        assertTrue("mine" in pending.ids.value)

        val deletes = TemplateDeleteViewModel(pending, repository)
        deletes.undo("mine")
        assertFalse("mine" in pending.ids.value)

        pending.add("mine")
        deletes.commit("mine")
        assertFalse("mine" in pending.ids.value)
    }
}
