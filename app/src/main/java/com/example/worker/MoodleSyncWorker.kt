package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result
import com.example.data.database.AppDatabase
import com.example.data.repository.MoodleRepository

class MoodleSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = MoodleRepository(database.appDao(), applicationContext)

        return try {
            Log.d("MoodleSyncWorker", "Iniciando sincronización automática en segundo plano...")
            repository.syncWithMoodle()
            Log.d("MoodleSyncWorker", "Sincronización en segundo plano completada con éxito.")
            Result.success()
        } catch (e: Exception) {
            Log.e("MoodleSyncWorker", "Error en sincronización automática: ${e.message}")
            if (e.message?.contains("Unable to resolve host") == true) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
