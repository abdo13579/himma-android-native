package com.himma.app.features.exercises

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.himma.app.core.database.ExerciseRepository
import com.himma.app.core.datastore.UserPreferencesRepository
import com.himma.app.domain.model.AppLanguage
import com.himma.app.domain.model.Exercise
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExerciseDetailUiState(
    val exercise: Exercise? = null,
    val language: AppLanguage = AppLanguage.ARABIC,
    val isLoading: Boolean = true,
)

sealed interface ExerciseDetailEvent {
    data object Deleted : ExerciseDetailEvent
}

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exerciseRepository: ExerciseRepository,
    preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val exerciseId: String = checkNotNull(savedStateHandle["exerciseId"])

    private val _events = MutableSharedFlow<ExerciseDetailEvent>()
    val events: SharedFlow<ExerciseDetailEvent> = _events.asSharedFlow()

    val uiState: StateFlow<ExerciseDetailUiState> = combine(
        exerciseRepository.observeExerciseById(exerciseId),
        preferencesRepository.preferences,
    ) { exercise, prefs ->
        ExerciseDetailUiState(
            exercise = exercise,
            language = prefs.language,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExerciseDetailUiState(),
    )

    fun deleteCustomExercise() {
        val current = uiState.value.exercise ?: return
        if (!current.isCustom) return
        viewModelScope.launch {
            exerciseRepository.deleteCustomExercise(current.id)
            _events.emit(ExerciseDetailEvent.Deleted)
        }
    }
}
