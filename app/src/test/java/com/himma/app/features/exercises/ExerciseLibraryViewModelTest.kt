package com.himma.app.features.exercises

import com.himma.app.core.database.ExerciseRepository
import com.himma.app.domain.model.EquipmentType
import com.himma.app.domain.model.Exercise
import com.himma.app.domain.model.MuscleGroup
import com.himma.app.domain.text.ArabicTextNormalizer
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseLibraryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeExerciseRepo: FakeExerciseRepository
    private lateinit var fakePrefsRepo: FakeUserPreferencesRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeExerciseRepo = FakeExerciseRepository()
        fakePrefsRepo = FakeUserPreferencesRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `library seeds on init and filters by query, muscle, and equipment`() = runTest {
        val viewModel = ExerciseLibraryViewModel(fakeExerciseRepo, fakePrefsRepo)
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        assertTrue(fakeExerciseRepo.seededCalled)
        assertEquals(3, viewModel.uiState.value.exercises.size)

        // Filter by Arabic query
        viewModel.updateQuery("بنش")
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.exercises.size)
        assertEquals("bench", viewModel.uiState.value.exercises.first().id)

        // Clear and filter by muscle + toggle off
        viewModel.clearFilters()
        viewModel.selectMuscle(MuscleGroup.LEGS)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.exercises.size)
        assertEquals("squat", viewModel.uiState.value.exercises.first().id)

        // Tapping same muscle clears selection
        viewModel.selectMuscle(MuscleGroup.LEGS)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.selectedMuscle)
        assertEquals(3, viewModel.uiState.value.exercises.size)

        // Filter by equipment
        viewModel.selectEquipment(EquipmentType.CABLE)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.exercises.size)
        assertEquals("pushdown", viewModel.uiState.value.exercises.first().id)

        collector.cancel()
    }
}

class FakeExerciseRepository : ExerciseRepository {
    var seededCalled = false
    private val items = MutableStateFlow(
        listOf(
            Exercise(
                id = "bench",
                nameEn = "Barbell Bench Press",
                nameAr = "بنش برس بالبار",
                primaryMuscle = MuscleGroup.CHEST,
                secondaryMuscles = listOf(MuscleGroup.ARMS),
                equipment = EquipmentType.BARBELL,
                instructionsEn = "Press up",
                instructionsAr = "ادفع للأعلى",
                localMediaKey = "bench",
                defaultRestSeconds = 120,
                isCustom = false,
                createdAt = 1L,
                updatedAt = 1L,
            ),
            Exercise(
                id = "squat",
                nameEn = "Barbell Squat",
                nameAr = "سكوات بالبار",
                primaryMuscle = MuscleGroup.LEGS,
                secondaryMuscles = listOf(MuscleGroup.CORE),
                equipment = EquipmentType.BARBELL,
                instructionsEn = "Squat down",
                instructionsAr = "انزل للأسفل",
                localMediaKey = "squat",
                defaultRestSeconds = 150,
                isCustom = false,
                createdAt = 1L,
                updatedAt = 1L,
            ),
            Exercise(
                id = "pushdown",
                nameEn = "Triceps Pushdown",
                nameAr = "دفع ترايسبس بالكابل",
                primaryMuscle = MuscleGroup.ARMS,
                secondaryMuscles = emptyList(),
                equipment = EquipmentType.CABLE,
                instructionsEn = "Push down",
                instructionsAr = "ادفع للأسفل",
                localMediaKey = "pushdown",
                defaultRestSeconds = 60,
                isCustom = false,
                createdAt = 1L,
                updatedAt = 1L,
            ),
        ),
    )

    override suspend fun ensureSeeded() {
        seededCalled = true
    }

    override fun observeExercises(
        query: String,
        muscleFilter: MuscleGroup?,
        equipmentFilter: EquipmentType?,
    ): Flow<List<Exercise>> = items.map { list ->
        val q = query.trim()
        val normalizedQ = ArabicTextNormalizer.normalize(q)
        list.filter { ex ->
            (muscleFilter == null || ex.primaryMuscle == muscleFilter) &&
                (equipmentFilter == null || ex.equipment == equipmentFilter) &&
                (
                    q.isEmpty() ||
                        ex.nameEn.contains(q, ignoreCase = true) ||
                        ex.nameAr.contains(q) ||
                        (normalizedQ.isNotEmpty() && ArabicTextNormalizer.normalize(ex.nameAr).contains(normalizedQ))
                    )
        }
    }

    override fun observeExerciseById(id: String): Flow<Exercise?> =
        items.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun getExerciseById(id: String): Exercise? =
        items.value.firstOrNull { it.id == id }

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
    ): String {
        val id = existingId ?: "custom_1"
        val resolvedAr = nameAr.trim().ifEmpty { nameEn.trim() }
        val resolvedEn = nameEn.trim().ifEmpty { nameAr.trim() }
        val created = Exercise(
            id = id,
            nameEn = resolvedEn,
            nameAr = resolvedAr,
            primaryMuscle = primaryMuscle,
            secondaryMuscles = secondaryMuscles.filter { it != primaryMuscle },
            equipment = equipment,
            instructionsEn = instructionsEn.trim(),
            instructionsAr = instructionsAr.trim(),
            localMediaKey = null,
            defaultRestSeconds = defaultRestSeconds,
            isCustom = true,
            createdAt = 10L,
            updatedAt = 20L,
        )
        items.value = items.value.filterNot { it.id == id } + created
        return id
    }

    override suspend fun deleteCustomExercise(id: String) {
        items.value = items.value.filterNot { it.id == id && it.isCustom }
    }
}
