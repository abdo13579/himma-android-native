package com.himma.app.features.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.himma.app.core.database.RoutineRepository
import com.himma.app.core.database.UserProfileRepository
import com.himma.app.core.database.WorkoutRepository
import com.himma.app.core.datastore.UserPreferences
import com.himma.app.core.datastore.UserPreferencesRepository
import com.himma.app.domain.model.Routine
import com.himma.app.domain.model.UserProfile
import com.himma.app.domain.model.WorkoutSession
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class TodayUiState(
    val preferences: UserPreferences = UserPreferences(),
    val profile: UserProfile? = null,
    val activeDraft: WorkoutSession? = null,
    val routines: List<Routine> = emptyList(),
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    preferencesRepository: UserPreferencesRepository,
    profileRepository: UserProfileRepository,
    workoutRepository: WorkoutRepository,
    routineRepository: RoutineRepository,
) : ViewModel() {

    val uiState: StateFlow<TodayUiState> = combine(
        preferencesRepository.preferences,
        profileRepository.profile,
        workoutRepository.observeActiveDraft(),
        routineRepository.observeActiveRoutines(),
    ) { prefs, profile, activeDraft, routines ->
        TodayUiState(
            preferences = prefs,
            profile = profile,
            activeDraft = activeDraft,
            routines = routines,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodayUiState(),
    )
}
