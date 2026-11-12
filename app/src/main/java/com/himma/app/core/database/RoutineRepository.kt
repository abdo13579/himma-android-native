package com.himma.app.core.database

import com.himma.app.core.database.dao.RoutineDao
import com.himma.app.core.database.entity.RoutineEntity
import com.himma.app.core.database.entity.RoutineExerciseEntity
import com.himma.app.domain.model.Routine
import com.himma.app.domain.model.RoutineExerciseDraft
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface RoutineRepository {
    fun observeActiveRoutines(): Flow<List<Routine>>

    fun observeRoutineById(routineId: String): Flow<Routine?>

    suspend fun getRoutineById(routineId: String): Routine?

    suspend fun saveRoutine(
        existingId: String?,
        title: String,
        notes: String,
        exercises: List<RoutineExerciseDraft>,
    ): String

    suspend fun duplicateRoutine(
        routineId: String,
        copySuffix: String,
    ): String?

    suspend fun softDeleteRoutine(routineId: String)

    suspend fun restoreRoutine(routineId: String)
}

@Singleton
class RoomRoutineRepository @Inject constructor(
    private val routineDao: RoutineDao,
) : RoutineRepository {

    override fun observeActiveRoutines(): Flow<List<Routine>> {
        return routineDao.observeActiveRoutinesWithExercises().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeRoutineById(routineId: String): Flow<Routine?> {
        return routineDao.observeRoutineWithExercises(routineId).map { it?.toDomain() }
    }

    override suspend fun getRoutineById(routineId: String): Routine? = withContext(Dispatchers.IO) {
        routineDao.getRoutineWithExercisesOnce(routineId)?.toDomain()
    }

    override suspend fun saveRoutine(
        existingId: String?,
        title: String,
        notes: String,
        exercises: List<RoutineExerciseDraft>,
    ): String = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val existing = existingId?.let { routineDao.getRoutineWithExercisesOnce(it)?.routine }
        val routineId = existing?.id ?: UUID.randomUUID().toString()
        val orderIndex = existing?.orderIndex ?: (routineDao.getMaxOrderIndex() + 1)

        val routineEntity = RoutineEntity(
            id = routineId,
            title = title.trim(),
            notes = notes.trim(),
            orderIndex = orderIndex,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            isDeleted = false,
        )

        val exerciseEntities = exercises.mapIndexed { index, rawDraft ->
            val draft = rawDraft.normalized()
            RoutineExerciseEntity(
                id = draft.itemId.ifBlank { UUID.randomUUID().toString() },
                routineId = routineId,
                exerciseId = draft.exercise.id,
                orderIndex = index,
                targetSets = draft.targetSets,
                targetRepsMin = draft.targetRepsMin,
                targetRepsMax = draft.targetRepsMax,
                targetRestSeconds = draft.targetRestSeconds,
            )
        }

        routineDao.saveRoutineWithExercises(routineEntity, exerciseEntities)
        routineId
    }

    override suspend fun duplicateRoutine(
        routineId: String,
        copySuffix: String,
    ): String? = withContext(Dispatchers.IO) {
        val original = routineDao.getRoutineWithExercisesOnce(routineId) ?: return@withContext null
        val now = System.currentTimeMillis()
        val newRoutineId = UUID.randomUUID().toString()
        val nextOrderIndex = routineDao.getMaxOrderIndex() + 1

        val copiedRoutine = RoutineEntity(
            id = newRoutineId,
            title = "${original.routine.title}$copySuffix",
            notes = original.routine.notes,
            orderIndex = nextOrderIndex,
            createdAt = now,
            updatedAt = now,
            isDeleted = false,
        )

        val copiedExercises = original.exercises
            .sortedBy { it.item.orderIndex }
            .mapIndexed { index, relation ->
                RoutineExerciseEntity(
                    id = UUID.randomUUID().toString(),
                    routineId = newRoutineId,
                    exerciseId = relation.item.exerciseId,
                    orderIndex = index,
                    targetSets = relation.item.targetSets,
                    targetRepsMin = relation.item.targetRepsMin,
                    targetRepsMax = relation.item.targetRepsMax,
                    targetRestSeconds = relation.item.targetRestSeconds,
                )
            }

        routineDao.saveRoutineWithExercises(copiedRoutine, copiedExercises)
        newRoutineId
    }

    override suspend fun softDeleteRoutine(routineId: String) {
        withContext(Dispatchers.IO) {
            routineDao.setDeleted(
                routineId = routineId,
                isDeleted = true,
                updatedAt = System.currentTimeMillis(),
            )
        }
    }

    override suspend fun restoreRoutine(routineId: String) {
        withContext(Dispatchers.IO) {
            routineDao.setDeleted(
                routineId = routineId,
                isDeleted = false,
                updatedAt = System.currentTimeMillis(),
            )
        }
    }
}
