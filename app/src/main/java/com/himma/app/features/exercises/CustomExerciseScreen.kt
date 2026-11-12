package com.himma.app.features.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.himma.app.R
import com.himma.app.core.datastore.UserPreferences
import com.himma.app.domain.model.EquipmentType
import com.himma.app.domain.model.MuscleGroup

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CustomExerciseScreen(
    onBackClick: () -> Unit,
    onSaved: (String) -> Unit,
    viewModel: CustomExerciseViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var muscleDropdownExpanded by remember { mutableStateOf(false) }
    var equipmentDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is CustomExerciseEvent.Saved -> onSaved(event.exerciseId)
            }
        }
    }

    val titleRes = if (state.editingId == null) {
        R.string.custom_exercise_create_title
    } else {
        R.string.custom_exercise_edit_title
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = titleRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(id = R.string.exercise_detail_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    OutlinedTextField(
                        value = state.nameAr,
                        onValueChange = viewModel::updateNameAr,
                        label = { Text(stringResource(id = R.string.custom_exercise_name_ar_label)) },
                        placeholder = { Text(stringResource(id = R.string.custom_exercise_name_ar_placeholder)) },
                        isError = state.nameError,
                        supportingText = if (state.nameError) {
                            { Text(stringResource(id = R.string.custom_exercise_name_required)) }
                        } else {
                            null
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = state.nameEn,
                        onValueChange = viewModel::updateNameEn,
                        label = { Text(stringResource(id = R.string.custom_exercise_name_en_label)) },
                        placeholder = { Text(stringResource(id = R.string.custom_exercise_name_en_placeholder)) },
                        isError = state.nameError,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Primary Muscle ExposedDropdownMenuBox
                    ExposedDropdownMenuBox(
                        expanded = muscleDropdownExpanded,
                        onExpandedChange = { muscleDropdownExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = stringResource(id = state.primaryMuscle.labelRes()),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(id = R.string.custom_exercise_muscle_label)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = muscleDropdownExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )
                        ExposedDropdownMenu(
                            expanded = muscleDropdownExpanded,
                            onDismissRequest = { muscleDropdownExpanded = false },
                        ) {
                            MuscleGroup.entries.forEach { muscle ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = muscle.labelRes())) },
                                    onClick = {
                                        viewModel.selectPrimaryMuscle(muscle)
                                        muscleDropdownExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    // Equipment ExposedDropdownMenuBox
                    ExposedDropdownMenuBox(
                        expanded = equipmentDropdownExpanded,
                        onExpandedChange = { equipmentDropdownExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = stringResource(id = state.equipment.labelRes()),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(id = R.string.custom_exercise_equipment_label)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = equipmentDropdownExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )
                        ExposedDropdownMenu(
                            expanded = equipmentDropdownExpanded,
                            onDismissRequest = { equipmentDropdownExpanded = false },
                        ) {
                            EquipmentType.coreTypes.forEach { equip ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = equip.labelRes())) },
                                    onClick = {
                                        viewModel.selectEquipment(equip)
                                        equipmentDropdownExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    // Secondary Muscles
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(id = R.string.custom_exercise_secondary_label),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            MuscleGroup.entries
                                .filter { it != state.primaryMuscle }
                                .forEach { muscle ->
                                    FilterChip(
                                        selected = muscle in state.secondaryMuscles,
                                        onClick = { viewModel.toggleSecondaryMuscle(muscle) },
                                        label = { Text(stringResource(id = muscle.labelRes())) },
                                        modifier = Modifier.heightIn(min = 48.dp),
                                    )
                                }
                        }
                    }

                    // Default Rest Timer
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(id = R.string.custom_exercise_rest_label),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            UserPreferences.REST_PRESETS.forEach { preset ->
                                FilterChip(
                                    selected = state.defaultRestSeconds == preset,
                                    onClick = { viewModel.selectRestSeconds(preset) },
                                    label = { Text(stringResource(id = R.string.seconds_short, preset)) },
                                    modifier = Modifier.heightIn(min = 48.dp),
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = state.instructionsAr,
                        onValueChange = viewModel::updateInstructionsAr,
                        label = { Text(stringResource(id = R.string.custom_exercise_instructions_ar_label)) },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = state.instructionsEn,
                        onValueChange = viewModel::updateInstructionsEn,
                        label = { Text(stringResource(id = R.string.custom_exercise_instructions_en_label)) },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Button(
                onClick = viewModel::saveExercise,
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.custom_exercise_save),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}
