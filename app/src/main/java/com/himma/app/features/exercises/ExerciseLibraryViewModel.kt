package com.himma.app.features.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.himma.app.core.database.ExerciseRepository
import com.himma.app.core.datastore.UserPreferencesRepository
import com.himma.app.domain.model.AppLanguage
import com.himma.app.domain.model.EquipmentType
import com.himma.app.domain.model.Exercise
import com.himma.app.domain.model.MuscleGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExerciseLibraryUiState(
    val query: String = "",
    val selectedMuscle: MuscleGroup? = null,
    val selectedEquipment: EquipmentType? = null,
    val language: AppLanguage = AppLanguage.ARABIC,
    val exercises: List<Exercise> = emptyList(),
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExerciseLibraryViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val queryFlow = MutableStateFlow("")
    private val muscleFilterFlow = MutableStateFlow<MuscleGroup?>(null)
    private val equipmentFilterFlow = MutableStateFlow<EquipmentType?>(null)
    private val isSeedingFlow = MutableStateFlow(true)

    private val filterCriteria = combine(
        queryFlow,
        muscleFilterFlow,
        equipmentFilterFlow,
    ) { query, muscle, equipment ->
        Triple(query, muscle, equipment)
    }

    private val exercisesFlow = filterCriteria.flatMapLatest { (query, muscle, equipment) ->
        exerciseRepository.observeExercises(
            query = query,
            muscleFilter = muscle,
            equipmentFilter = equipment,
        )
    }

    val uiState: StateFlow<ExerciseLibraryUiState> = combine(
        filterCriteria,
        preferencesRepository.preferences,
        exercisesFlow,
        isSeedingFlow,
    ) { (query, muscle, equipment), prefs, exercises, isSeeding ->
        ExerciseLibraryUiState(
            query = query,
            selectedMuscle = muscle,
            selectedEquipment = equipment,
            language = prefs.language,
            exercises = exercises,
            isLoading = isSeeding && exercises.isEmpty(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = ExerciseLibraryUiState(),
    )

    init {
        viewModelScope.launch {
            exerciseRepository.ensureSeeded()
            isSeedingFlow.value = false
        }
    }

    fun updateQuery(query: String) {
        queryFlow.value = query
    }

    fun selectMuscle(muscle: MuscleGroup?) {
        muscleFilterFlow.value = if (muscleFilterFlow.value == muscle) null else muscle
    }

    fun selectEquipment(equipment: EquipmentType?) {
        equipmentFilterFlow.value = if (equipmentFilterFlow.value == equipment) null else equipment
    }

    fun clearFilters() {
        queryFlow.value = ""
        muscleFilterFlow.value = null
        equipmentFilterFlow.value = null
    }
}
