package com.himma.app.features.exercises

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.PrecisionManufacturing
import androidx.compose.material.icons.rounded.SportsGymnastics
import androidx.compose.ui.graphics.vector.ImageVector
import com.himma.app.R
import com.himma.app.domain.model.EquipmentType
import com.himma.app.domain.model.MuscleGroup

@StringRes
fun MuscleGroup.labelRes(): Int = when (this) {
    MuscleGroup.CHEST -> R.string.muscle_chest
    MuscleGroup.BACK -> R.string.muscle_back
    MuscleGroup.LEGS -> R.string.muscle_legs
    MuscleGroup.SHOULDERS -> R.string.muscle_shoulders
    MuscleGroup.ARMS -> R.string.muscle_arms
    MuscleGroup.CORE -> R.string.muscle_core
}

@StringRes
fun EquipmentType.labelRes(): Int = when (this) {
    EquipmentType.BARBELL -> R.string.equipment_barbell
    EquipmentType.DUMBBELL -> R.string.equipment_dumbbell
    EquipmentType.CABLE -> R.string.equipment_cable
    EquipmentType.MACHINE -> R.string.equipment_machine
    EquipmentType.BODYWEIGHT -> R.string.equipment_bodyweight
    EquipmentType.OTHER -> R.string.equipment_other
}

fun EquipmentType.icon(): ImageVector = when (this) {
    EquipmentType.BARBELL -> Icons.Rounded.FitnessCenter
    EquipmentType.DUMBBELL -> Icons.Rounded.SportsGymnastics
    EquipmentType.CABLE -> Icons.Rounded.Height
    EquipmentType.MACHINE -> Icons.Rounded.PrecisionManufacturing
    EquipmentType.BODYWEIGHT -> Icons.Rounded.AccessibilityNew
    EquipmentType.OTHER -> Icons.Rounded.FitnessCenter
}
