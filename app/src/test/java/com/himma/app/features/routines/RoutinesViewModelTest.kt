package com.himma.app.features.routines

import com.himma.app.core.database.RoutineRepository
import com.himma.app.domain.model.EquipmentType
import com.himma.app.domain.model.Exercise
import com.himma.app.domain.model.MuscleGroup
import com.himma.app.domain.model.Routine
import com.himma.app.domain.model.RoutineExercise
import com.himma.app.domain.model.RoutineExerciseDraft
import com.himma.app.features.onboarding.FakeUserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoutinesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRoutineRepo: FakeRoutineRepository
    private lateinit var fakePrefsRepo: FakeUserPreferencesRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRoutineRepo = FakeRoutineRepository()
        fakePrefsRepo = FakeUserPreferencesRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `duplicate creates unlimited copy and soft delete supports undo restore`() = runTest {
        fakeRoutineRepo.saveRoutine(
            existingId = null,
            title = "يوم الدفع",
            notes = "تركيز على الصدر",
            exercises = emptyList(),
        )

        val viewModel = RoutinesViewModel(fakeRoutineRepo, fakePrefsRepo)
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.routines.size)
        val original = viewModel.uiState.value.routines.first()

        var duplicatedCallbackFired = false
        viewModel.duplicateRoutine(original.id, " (نسخة)") {
            duplicatedCallbackFired = true
        }
        advanceUntilIdle()

        assertTrue(duplicatedCallbackFired)
        assertEquals(2, viewModel.uiState.value.routines.size)
        assertEquals("يوم الدفع (نسخة)", viewModel.uiState.value.routines[1].title)

        var deletedCallbackFired = false
        viewModel.deleteRoutine(original) {
            deletedCallbackFired = true
        }
        advanceUntilIdle()

        assertTrue(deletedCallbackFired)
        assertEquals(1, viewModel.uiState.value.routines.size)

        viewModel.restoreRoutine(original.id)
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.routines.size)
        collector.cancel()
    }
}

class FakeRoutineRepository : RoutineRepository {
    private val routinesFlow = MutableStateFlow<List<Routine>>(emptyList())
    private var seq = 0

    override fun observeActiveRoutines(): Flow<List<Routine>> = routinesFlow.map { list ->
        list.filterNot { it.isDeleted }.sortedBy { it.orderIndex }
    }

    override fun observeRoutineById(routineId: String): Flow<Routine?> = routinesFlow.map { list ->
        list.firstOrNull { it.id == routineId }
    }

    override suspend fun getRoutineById(routineId: String): Routine? =
        routinesFlow.value.firstOrNull { it.id == routineId }

    override suspend fun saveRoutine(
        existingId: String?,
        title: String,
        notes: String,
        exercises: List<RoutineExerciseDraft>,
    ): String {
        val currentList = routinesFlow.value
        val existing = existingId?.let { id -> currentList.firstOrNull { it.id == id } }
        val resolvedId = existing?.id ?: "routine_${++seq}"
        val orderIndex = existing?.orderIndex ?: ((currentList.maxOfOrNull { it.orderIndex } ?: -1) + 1)

        val saved = Routine(
            id = resolvedId,
            title = title.trim(),
            notes = notes.trim(),
            orderIndex = orderIndex,
            createdAt = existing?.createdAt ?: 100L,
            updatedAt = 200L,
            isDeleted = false,
            exercises = exercises.mapIndexed { idx, draft ->
                val clean = draft.normalized()
                RoutineExercise(
                    id = clean.itemId,
                    routineId = resolvedId,
                    exercise = clean.exercise,
                    orderIndex = idx,
                    targetSets = clean.targetSets,
                    targetRepsMin = clean.targetRepsMin,
                    targetRepsMax = clean.targetRepsMax,
                    targetRestSeconds = clean.targetRestSeconds,
                )
            },
        )
        routinesFlow.value = currentList.filterNot { it.id == resolvedId } + saved
        return resolvedId
    }

    override suspend fun duplicateRoutine(
        routineId: String,
        copySuffix: String,
    ): String? {
        val original = routinesFlow.value.firstOrNull { it.id == routineId } ?: return null
        val newId = "routine_${++seq}"
        val nextOrder = (routinesFlow.value.maxOfOrNull { it.orderIndex } ?: -1) + 1
        val copied = original.copy(
            id = newId,
            title = "${original.title}$copySuffix",
            orderIndex = nextOrder,
            exercises = original.exercises.mapIndexed { index, item ->
                item.copy(
                    id = "${newId}_ex_$index",
                    routineId = newId,
                )
            },
        )
        routinesFlow.value = routinesFlow.value + copied
        return newId
    }

    override suspend fun softDeleteRoutine(routineId: String) {
        routinesFlow.value = routinesFlow.value.map {
            if (it.id == routineId) it.copy(isDeleted = true) else it
        }
    }

    override suspend fun restoreRoutine(routineId: String) {
        routinesFlow.value = routinesFlow.value.map {
            if (it.id == routineId) it.copy(isDeleted = false) else it
        }
    }
}
