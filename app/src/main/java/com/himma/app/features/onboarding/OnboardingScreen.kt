package com.himma.app.features.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.himma.app.R
import com.himma.app.core.datastore.UserPreferences
import com.himma.app.domain.model.AppLanguage
import com.himma.app.domain.model.WeightUnit

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_himma_dumbbell),
                contentDescription = stringResource(id = R.string.app_name),
                modifier = Modifier.size(width = 128.dp, height = 72.dp),
            )

            Text(
                text = stringResource(id = R.string.onboarding_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )

            Text(
                text = stringResource(id = R.string.onboarding_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            AssistChip(
                onClick = {},
                label = {
                    Text(
                        text = stringResource(id = R.string.onboarding_local_badge),
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
            )

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Language selector
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
                                    selected = state.language == lang,
                                    onClick = { viewModel.selectLanguage(lang) },
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

                    // Weight Unit selector
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
                                    selected = state.weightUnit == unit,
                                    onClick = { viewModel.selectWeightUnit(unit) },
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

                    // Default Rest Timer presets
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
                                    selected = state.defaultRestSeconds == preset,
                                    onClick = { viewModel.selectRestSeconds(preset) },
                                    label = {
                                        Text(
                                            text = stringResource(id = R.string.seconds_short, preset),
                                        )
                                    },
                                    modifier = Modifier.heightIn(min = 48.dp),
                                )
                            }
                        }
                    }

                    // Optional Display Name
                    OutlinedTextField(
                        value = state.displayName,
                        onValueChange = viewModel::updateDisplayName,
                        label = { Text(stringResource(id = R.string.onboarding_name_label)) },
                        placeholder = { Text(stringResource(id = R.string.onboarding_name_placeholder)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Optional Body Weight
                    OutlinedTextField(
                        value = state.bodyWeightInput,
                        onValueChange = viewModel::updateBodyWeightInput,
                        label = {
                            val unitLabel = when (state.weightUnit) {
                                WeightUnit.KG -> stringResource(id = R.string.unit_kg)
                                WeightUnit.LBS -> stringResource(id = R.string.unit_lbs)
                            }
                            Text("${stringResource(id = R.string.onboarding_body_weight_label)} ($unitLabel)")
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
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = viewModel::completeOnboarding,
                    enabled = !state.isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.onboarding_start_cta),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}
