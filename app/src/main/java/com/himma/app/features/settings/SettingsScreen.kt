package com.himma.app.features.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.himma.app.R
import com.himma.app.core.datastore.UserPreferences
import com.himma.app.domain.model.AppLanguage
import com.himma.app.domain.model.ThemeMode
import com.himma.app.domain.model.WeightUnit

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val savedMessage = stringResource(id = R.string.settings_profile_saved)

    LaunchedEffect(viewModel, savedMessage) {
        viewModel.events.collect { event ->
            when (event) {
                SettingsEvent.ProfileSaved -> onShowMessage(savedMessage)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Local Profile Card
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.settings_section_profile),
                    style = MaterialTheme.typography.titleMedium,
                )

                OutlinedTextField(
                    value = state.displayNameInput,
                    onValueChange = viewModel::updateDisplayName,
                    label = { Text(stringResource(id = R.string.onboarding_name_label)) },
                    placeholder = { Text(stringResource(id = R.string.onboarding_name_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = state.bodyWeightInput,
                    onValueChange = viewModel::updateBodyWeightInput,
                    label = {
                        val unitText = when (state.preferences.weightUnit) {
                            WeightUnit.KG -> stringResource(id = R.string.unit_kg)
                            WeightUnit.LBS -> stringResource(id = R.string.unit_lbs)
                        }
                        Text("${stringResource(id = R.string.onboarding_body_weight_label)} ($unitText)")
                    },
                    placeholder = { Text(stringResource(id = R.string.onboarding_body_weight_placeholder)) },
                    isError = state.bodyWeightError,
                    supportingText = if (state.bodyWeightError) {
                        { Text(stringResource(id = R.string.validation_invalid_weight)) }
                    } else {
                        null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                FilledTonalButton(
                    onClick = viewModel::saveProfile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text(stringResource(id = R.string.settings_save_profile))
                }
            }
        }

        // Training Preferences Card
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.settings_section_preferences),
                    style = MaterialTheme.typography.titleMedium,
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(id = R.string.onboarding_language_label),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                    ) {
                        AppLanguage.entries.forEachIndexed { index, lang ->
                            SegmentedButton(
                                selected = state.preferences.language == lang,
                                onClick = { viewModel.setLanguage(lang) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = AppLanguage.entries.size,
                                ),
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Text(
                                    text = when (lang) {
                                        AppLanguage.ARABIC -> stringResource(id = R.string.language_ar)
                                        AppLanguage.ENGLISH -> stringResource(id = R.string.language_en)
                                    },
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(id = R.string.onboarding_unit_label),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                    ) {
                        WeightUnit.entries.forEachIndexed { index, unit ->
                            SegmentedButton(
                                selected = state.preferences.weightUnit == unit,
                                onClick = { viewModel.setWeightUnit(unit) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = WeightUnit.entries.size,
                                ),
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Text(
                                    text = when (unit) {
                                        WeightUnit.KG -> stringResource(id = R.string.unit_kg)
                                        WeightUnit.LBS -> stringResource(id = R.string.unit_lbs)
                                    },
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(id = R.string.onboarding_rest_label),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        UserPreferences.REST_PRESETS.forEach { preset ->
                            FilterChip(
                                selected = state.preferences.defaultRestSeconds == preset,
                                onClick = { viewModel.setDefaultRestSeconds(preset) },
                                label = {
                                    Text(stringResource(id = R.string.seconds_short, preset))
                                },
                                modifier = Modifier.heightIn(min = 48.dp),
                            )
                        }
                    }
                }
            }
        }

        // Material 3 Appearance Card
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.settings_section_appearance),
                    style = MaterialTheme.typography.titleMedium,
                )

                Text(
                    text = stringResource(id = R.string.settings_theme_label),
                    style = MaterialTheme.typography.titleSmall,
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = state.preferences.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            label = {
                                val labelRes = when (mode) {
                                    ThemeMode.AMOLED_DARK -> R.string.theme_amoled_dark
                                    ThemeMode.DARK -> R.string.theme_dark
                                    ThemeMode.LIGHT -> R.string.theme_light
                                    ThemeMode.SYSTEM -> R.string.theme_system
                                }
                                Text(stringResource(id = labelRes))
                            },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp),
                        ) {
                            Text(
                                text = stringResource(id = R.string.settings_dynamic_color),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = stringResource(id = R.string.settings_dynamic_color_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = state.preferences.useDynamicColor,
                            onCheckedChange = viewModel::setUseDynamicColor,
                        )
                    }
                }
            }
        }

        // Local Storage & Testing Card
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.settings_section_storage),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(id = R.string.settings_storage_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = viewModel::resetOnboarding,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text(stringResource(id = R.string.settings_reset_onboarding))
                }
            }
        }
    }
}
