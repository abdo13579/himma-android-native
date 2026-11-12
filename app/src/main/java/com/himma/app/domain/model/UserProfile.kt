package com.himma.app.domain.model

data class UserProfile(
    val id: Int = 1,
    val displayName: String? = null,
    val bodyWeightKg: Double? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)
