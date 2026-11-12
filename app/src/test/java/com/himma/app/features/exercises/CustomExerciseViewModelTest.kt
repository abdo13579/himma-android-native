package com.himma.app.features.exercises

import androidx.lifecycle.SavedStateHandle
import com.himma.app.domain.model.EquipmentType
import com.himma.app.domain.model.MuscleGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CustomExerciseViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeExerciseRepo: FakeExerciseRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeExerciseRepo = FakeExerciseRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saveExercise requires at least one name and persists custom exercise`() = runTest {
        val viewModel = CustomExerciseViewModel(SavedStateHandle(), fakeExerciseRepo)
        val events = mutableListOf<CustomExerciseEvent>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { events.add(it) }
        }

        // Empty name -> validation error
        viewModel.saveExercise()
        assertTrue(viewModel.uiState.value.nameError)
        assertTrue(events.isEmpty())

        // Valid input -> saved and emits event
        viewModel.updateNameAr("تمرين ظهر مخصص")
        viewModel.selectPrimaryMuscle(MuscleGroup.BACK)
        viewModel.toggleSecondaryMuscle(MuscleGroup.ARMS)
        viewModel.selectEquipment(EquipmentType.CABLE)
        viewModel.selectRestSeconds(120)
        viewModel.saveExercise()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.nameError)
        assertEquals(1, events.size)
        val savedId = (events.first() as CustomExerciseEvent.Saved).exerciseId
        val stored = fakeExerciseRepo.observeExerciseById(savedId).first()
        assertEquals("تمرين ظهر مخصص", stored?.nameAr)
        assertEquals(MuscleGroup.BACK, stored?.primaryMuscle)
        assertEquals(listOf(MuscleGroup.ARMS), stored?.secondaryMuscles)
        assertTrue(stored?.isCustom == true)

        job.cancel()
    }

    @Test
    fun `editing existing custom exercise preloads fields and updates record`() = runTest {
        val existingId = fakeExerciseRepo.saveCustomExercise(
            existingId = null,
            nameAr = "اسم قديم",
            nameEn = "Old Name",
            primaryMuscle = MuscleGroup.CHEST,
            secondaryMuscles = listOf(MuscleGroup.SHOULDERS),
            equipment = EquipmentType.DUMBBELL,
            defaultRestSeconds = 90,
            instructionsAr = "خطوة قديمة",
            instructionsEn = "",
        )

        val viewModel = CustomExerciseViewModel(
            SavedStateHandle(mapOf("exerciseId" to existingId)),
            fakeExerciseRepo,
        )
        advanceUntilIdle()

        assertEquals("اسم قديم", viewModel.uiState.value.nameAr)
        assertEquals(MuscleGroup.CHEST, viewModel.uiState.value.primaryMuscle)

        viewModel.updateNameAr("اسم محدث")
        viewModel.saveExercise()
        advanceUntilIdle()

        val updated = fakeExerciseRepo.getExerciseById(existingId)
        assertEquals("اسم محدث", updated?.nameAr)
    }
}
