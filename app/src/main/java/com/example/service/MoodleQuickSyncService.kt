package com.example.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.data.database.AppDatabase
import com.example.data.repository.MoodleRepository
import kotlinx.coroutines.*

class MoodleQuickSyncService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var syncJob: Job? = null
    private lateinit var repository: MoodleRepository

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getDatabase(applicationContext)
        repository = MoodleRepository(database.appDao(), applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification("Monitoreo en tiempo real activo")
        startForeground(NOTIFICATION_ID, notification)

        startSyncLoop()
        
        return START_STICKY
    }

    private fun startSyncLoop() {
        syncJob?.cancel()
        syncJob = serviceScope.launch {
            while (isActive) {
                try {
                    Log.d("QuickSyncService", "Ejecutando escaneo inmediato de Moodle Cloud...")
                    repository.syncWithMoodle()
                    Log.d("QuickSyncService", "Escaneo completado.")
                } catch (e: Exception) {
                    Log.e("QuickSyncService", "Error en escaneo rápido: ${e.message}")
                }
                // Esperar 60 segundos antes de la siguiente verificación
                delay(60000) 
            }
        }
    }

    private fun createNotification(contentText: String): Notification {
        val notificationIntent = Intent(this, com.example.MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Sincronización Moodle")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW) // Baja prioridad para no molestar, pero viva
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Servicio de Sincronización Rápida",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        syncJob?.cancel()
        serviceScope.cancel()
    }

    companion object {
        private const val CHANNEL_ID = "QuickSyncServiceChannel"
        private const val NOTIFICATION_ID = 999
    }
}
