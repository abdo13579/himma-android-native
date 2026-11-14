package com.himma.app.features.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.himma.app.core.database.RoutineRepository
import com.himma.app.core.database.UserProfileRepository
import com.himma.app.core.database.WorkoutRepository
import com.himma.app.core.datastore.UserPreferences
import com.himma.app.core.datastore.UserPreferencesRepository
import com.himma.app.core.updater.AppUpdateInfo
import com.himma.app.core.updater.AppUpdateRepository
import com.himma.app.domain.model.Routine
import com.himma.app.domain.model.UserProfile
import com.himma.app.domain.model.WorkoutSession
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodayUiState(
    val preferences: UserPreferences = UserPreferences(),
    val profile: UserProfile? = null,
    val activeDraft: WorkoutSession? = null,
    val routines: List<Routine> = emptyList(),
    val availableUpdate: AppUpdateInfo? = null,
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    preferencesRepository: UserPreferencesRepository,
    profileRepository: UserProfileRepository,
    workoutRepository: WorkoutRepository,
    routineRepository: RoutineRepository,
    private val appUpdateRepository: AppUpdateRepository,
) : ViewModel() {

    private val availableUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    private var hasCheckedUpdates = false

    init {
        viewModelScope.launch {
            val prefs = preferencesRepository.preferences.first()
            if (prefs.autoCheckUpdates && !hasCheckedUpdates) {
                hasCheckedUpdates = true
                appUpdateRepository.checkForUpdate().onSuccess { info ->
                    if (info.isUpdateAvailable) {
                        availableUpdate.value = info
                    }
                }
            }
        }
    }

    val uiState: StateFlow<TodayUiState> = combine(
        preferencesRepository.preferences,
        profileRepository.profile,
        workoutRepository.observeActiveDraft(),
        routineRepository.observeActiveRoutines(),
        availableUpdate,
    ) { prefs, profile, activeDraft, routines, update ->
        TodayUiState(
            preferences = prefs,
            profile = profile,
            activeDraft = activeDraft,
            routines = routines,
            availableUpdate = update,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodayUiState(),
    )

    fun dismissUpdateBanner() {
        availableUpdate.value = null
    }
}
