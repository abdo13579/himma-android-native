package com.himma.app.features.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.himma.app.core.database.RoutineRepository
import com.himma.app.core.datastore.UserPreferencesRepository
import com.himma.app.domain.model.AppLanguage
import com.himma.app.domain.model.Routine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RoutinesUiState(
    val isLoading: Boolean = true,
    val routines: List<Routine> = emptyList(),
    val language: AppLanguage = AppLanguage.ARABIC,
)

@HiltViewModel
class RoutinesViewModel @Inject constructor(
    private val routineRepository: RoutineRepository,
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<RoutinesUiState> = combine(
        routineRepository.observeActiveRoutines(),
        userPreferencesRepository.preferences,
    ) { routines, prefs ->
        RoutinesUiState(
            isLoading = false,
            routines = routines,
            language = prefs.language,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RoutinesUiState(),
    )

    fun duplicateRoutine(
        routineId: String,
        copySuffix: String,
        onDuplicated: () -> Unit,
    ) {
        viewModelScope.launch {
            val newId = routineRepository.duplicateRoutine(routineId, copySuffix)
            if (newId != null) {
                onDuplicated()
            }
        }
    }

    fun deleteRoutine(
        routine: Routine,
        onDeleted: () -> Unit,
    ) {
        viewModelScope.launch {
            routineRepository.softDeleteRoutine(routine.id)
            onDeleted()
        }
    }

    fun restoreRoutine(routineId: String) {
        viewModelScope.launch {
            routineRepository.restoreRoutine(routineId)
        }
    }
}
