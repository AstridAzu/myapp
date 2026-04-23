package com.example.myapp.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.example.myapp.BuildConfig
import com.example.myapp.data.database.DatabaseBuilder
import com.example.myapp.data.remote.ExercisesApiFactory
import com.example.myapp.data.remote.RoutinesApiFactory
import com.example.myapp.data.remote.sync.SyncApiFactory
import com.example.myapp.utils.SessionManager

class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        private val UUID_REGEX = Regex(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$"
        )

        private fun isValidUserIdForSync(userId: String): Boolean {
            return UUID_REGEX.matches(userId) || userId.toLongOrNull() != null
        }
    }

    override suspend fun doWork(): ListenableWorker.Result {
        val syncBaseUrl = BuildConfig.SYNC_API_BASE_URL.trim()
        val syncToken = BuildConfig.SYNC_API_TOKEN.trim()
        val syncUserId = SessionManager(applicationContext).getUserIdString().trim()
        
        if (BuildConfig.DEBUG) {
            Log.d(
                "ConfigCheck",
                "SYNC_API_BASE_URL=$syncBaseUrl, SYNC_API_TOKEN configured=${syncToken.isNotBlank()}, x-user-id configured=${syncUserId.isNotBlank()}"
            )
        }
        
        if (syncBaseUrl.isBlank()) {
            return ListenableWorker.Result.success()
        }

        val database = DatabaseBuilder.getDatabase(applicationContext)
        val sessionManager = SessionManager(applicationContext)
        
        // ============================================================
        // SYNC BASE EXERCISES FIRST (if needed)
        // ============================================================
        try {
            Log.d("SyncWorker", "Starting base exercises sync...")
            val exercisesApi = ExercisesApiFactory.create(syncBaseUrl)
            val baseExercisesMgr = BaseExercisesSyncManager(database, exercisesApi)
            
            val result = baseExercisesMgr.syncBaseExercises()
            result.fold(
                onSuccess = {
                    Log.d("SyncWorker", "✓ Base exercises synced successfully")
                    sessionManager.setLastBaseExercisesSyncTime(System.currentTimeMillis())
                },
                onFailure = { error ->
                    Log.w("SyncWorker", "⚠ Base exercises sync failed (non-blocking)", error)
                    // No fallar el sync completo si esto falla
                }
            )
        } catch (e: Exception) {
            Log.e("SyncWorker", "Exception in base exercises sync", e)
            // Continuar con sync normal
        }
        
        // ============================================================
        // SYNC BASE ROUTINES SECOND (if needed)
        // ============================================================
        try {
            // Siempre sincronizar routinas base (se limpian automáticamente antes de insertar)
            Log.d("SyncWorker", "Starting base routines sync...")
            val routinesApi = RoutinesApiFactory.create(syncBaseUrl)
            val baseRoutinesMgr = BaseRoutinesSyncManager(database, routinesApi)
            
            val result = baseRoutinesMgr.syncBaseRoutines()
            result.fold(
                onSuccess = {
                    Log.d("SyncWorker", "✓ Base routines synced successfully")
                    sessionManager.setLastBaseRoutinesSyncTime(System.currentTimeMillis())
                },
                onFailure = { error ->
                    Log.w("SyncWorker", "⚠ Base routines sync failed (non-blocking)", error)
                    // No fallar el sync completo si esto falla
                }
            )
        } catch (e: Exception) {
            Log.e("SyncWorker", "Exception in base routines sync", e)
            // Continuar con sync normal
        }
        
        // ============================================================
        // SYNC BASE ROUTINE-EXERCISE LINKS (if needed)
        // ============================================================
        try {
            // Siempre sincronizar links (se limpian automáticamente antes de insertar)
            Log.d("SyncWorker", "Starting base routine links sync...")
            val routinesApi = RoutinesApiFactory.create(syncBaseUrl)
            val baseRoutineLinksMgr = BaseRoutineLinksSyncManager(database, routinesApi)
            
            val result = baseRoutineLinksMgr.syncBaseRoutineLinks()
            result.fold(
                onSuccess = {
                    Log.d("SyncWorker", "✓ Base routine links synced successfully")
                    sessionManager.setLastBaseRoutinesSyncTime(System.currentTimeMillis())
                },
                onFailure = { error ->
                    Log.w("SyncWorker", "⚠ Base routine links sync failed (non-blocking)", error)
                    // No fallar el sync completo si esto falla
                }
            )
        } catch (e: Exception) {
            Log.e("SyncWorker", "Exception in base routine links sync", e)
            // Continuar con sync normal
        }
        
        // ============================================================
        // SYNC NORMAL (entidades del usuario)
        // ============================================================
        if (syncToken.isBlank() || syncUserId.isBlank()) {
            Log.w("SyncWorker", "Skipping normal sync: missing token or user id")
            return ListenableWorker.Result.success()
        }

        if (!isValidUserIdForSync(syncUserId)) {
            Log.w("SyncWorker", "Skipping normal sync: session user id is neither UUID nor numeric legacy id")
            return ListenableWorker.Result.success()
        }

        // Log credentials for diagnostic purposes (sanitized token)
        val tokenSuffix = syncToken.takeLast(8)
        Log.d("SyncWorker", "Starting normal sync with credentials:")
        Log.d("SyncWorker", "  - Authorization: Bearer ...${tokenSuffix}")
        Log.d("SyncWorker", "  - x-user-id: $syncUserId")
        Log.d("SyncWorker", "  - baseUrl: $syncBaseUrl")

        val syncApi = SyncApiFactory.create(
            baseUrl = syncBaseUrl,
            bearerToken = syncToken,
            userId = syncUserId
        )
        val sessionManagerForSync = SessionManager(applicationContext)
        val syncManager = SyncManager(
            database = database,
            syncApi = syncApi,
            sessionManager = sessionManagerForSync,
            remoteIdStrategy = LegacyRemoteIdStrategy.fromRaw(BuildConfig.SYNC_REMOTE_ID_STRATEGY)
        )

        return syncManager.syncAll().fold(
            onSuccess = { 
                Log.i("SyncWorker", "✓ Normal sync completed successfully")
                ListenableWorker.Result.success() 
            },
            onFailure = { error ->
                Log.e("SyncWorker", "✗ Sync failed: ${error.javaClass.simpleName}: ${error.message}")
                if (isNonRetryableSyncError(error)) {
                    Log.w("SyncWorker", "  → Classified as NON-RETRYABLE. Will not retry.")
                    ListenableWorker.Result.failure()
                } else {
                    Log.w("SyncWorker", "  → Classified as RETRYABLE. Will retry later.")
                    ListenableWorker.Result.retry()
                }
            }
        )
    }
}
