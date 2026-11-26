package com.himma.app.features.exercises

import androidx.lifecycle.SavedStateHandle
import com.himma.app.core.database.ExerciseRepository
import com.himma.app.domain.model.EquipmentType
import com.himma.app.domain.model.Exercise
import com.himma.app.domain.model.ExerciseType
import com.himma.app.domain.model.MuscleGroup
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
class ExerciseDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var prefsRepo: FakeUserPreferencesRepository
    private lateinit var exerciseRepo: FakeDetailExerciseRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        prefsRepo = FakeUserPreferencesRepository()
        exerciseRepo = FakeDetailExerciseRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `deleteCustomExercise deletes custom exercise once and emits Deleted event`() = runTest {
        val viewModel = ExerciseDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("exerciseId" to "custom_1")),
            exerciseRepository = exerciseRepo,
            preferencesRepository = prefsRepo,
        )
        val events = mutableListOf<ExerciseDetailEvent>()
        val uiCollector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        val eventCollector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { events.add(it) }
        }
        advanceUntilIdle()

        viewModel.deleteCustomExercise()
        viewModel.deleteCustomExercise()
        advanceUntilIdle()

        assertEquals(listOf("custom_1"), exerciseRepo.deletedIds)
        assertEquals(1, events.size)
        assertEquals(ExerciseDetailEvent.Deleted, events.first())

        uiCollector.cancel()
        eventCollector.cancel()
    }

    @Test
    fun `deleteCustomExercise ignores non-custom exercises`() = runTest {
        val viewModel = ExerciseDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("exerciseId" to "seed_1")),
            exerciseRepository = exerciseRepo,
            preferencesRepository = prefsRepo,
        )
        val uiCollector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.deleteCustomExercise()
        advanceUntilIdle()

        assertTrue(exerciseRepo.deletedIds.isEmpty())
        uiCollector.cancel()
    }
}

private class FakeDetailExerciseRepository : ExerciseRepository {
    val deletedIds = mutableListOf<String>()
    private val items = MutableStateFlow(
        listOf(
            Exercise(
                id = "custom_1",
                nameEn = "Custom Fly",
                nameAr = "تفتيح مخصص",
                primaryMuscle = MuscleGroup.CHEST,
                secondaryMuscles = emptyList(),
                equipment = EquipmentType.CABLE,
                instructionsEn = "",
                instructionsAr = "",
                localMediaKey = null,
                defaultRestSeconds = 90,
                isCustom = true,
                createdAt = 1L,
                updatedAt = 1L,
            ),
            Exercise(
                id = "seed_1",
                nameEn = "Bench Press",
                nameAr = "بنش مستوي",
                primaryMuscle = MuscleGroup.CHEST,
                secondaryMuscles = emptyList(),
                equipment = EquipmentType.BARBELL,
                instructionsEn = "",
                instructionsAr = "",
                localMediaKey = null,
                defaultRestSeconds = 90,
                isCustom = false,
                createdAt = 1L,
                updatedAt = 1L,
            ),
        ),
    )

    override suspend fun ensureSeeded() {}

    override fun observeExercises(
        query: String,
        muscleFilter: MuscleGroup?,
        equipmentFilter: EquipmentType?,
    ): Flow<List<Exercise>> = items

    override fun observeExerciseById(id: String): Flow<Exercise?> {
        return items.map { list -> list.firstOrNull { it.id == id } }
    }

    override suspend fun getExerciseById(id: String): Exercise? {
        return items.value.firstOrNull { it.id == id }
    }

    override suspend fun saveCustomExercise(
        existingId: String?,
        nameAr: String,
        nameEn: String,
        primaryMuscle: MuscleGroup,
        secondaryMuscles: List<MuscleGroup>,
        equipment: EquipmentType,
        defaultRestSeconds: Int,
        instructionsAr: String,
        instructionsEn: String,
        exerciseType: ExerciseType,
    ): String = existingId ?: "custom_new"

    override suspend fun deleteCustomExercise(id: String) {
        deletedIds.add(id)
    }
}
