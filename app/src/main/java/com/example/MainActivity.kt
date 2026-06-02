package com.example

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MoodleViewModel
import com.example.worker.MoodleSyncWorker
import android.content.Intent
import com.example.service.MoodleQuickSyncService
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

  private val requestPermissionLauncher = registerForActivityResult(
      ActivityResultContracts.RequestPermission()
  ) { isGranted: Boolean ->
      if (isGranted) {
          // Permiso concedido
      }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Solicitar permiso de notificaciones en Android 13+
    askNotificationPermission()

    // Configurar Sincronización Inmediata (Cada 60 seg)
    startQuickSyncService()

    setContent {
      MyApplicationTheme {
        val viewModel: MoodleViewModel = viewModel()
        MainAppScreen(
          viewModel = viewModel,
          modifier = Modifier.fillMaxSize()
        )
      }
    }
  }

  private fun askNotificationPermission() {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
              PackageManager.PERMISSION_GRANTED
          ) {
              requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
          }
      }
  }

  private fun startQuickSyncService() {
    val serviceIntent = Intent(this, MoodleQuickSyncService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(serviceIntent)
    } else {
        startService(serviceIntent)
    }
  }

  // Se mantiene el WorkManager como backup de largo plazo, pero el Service es el primario ahora
  private fun setupBackgroundSync() {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    val syncRequest = PeriodicWorkRequestBuilder<MoodleSyncWorker>(1, TimeUnit.HOURS)
        .setConstraints(constraints)
        .build()

    WorkManager.getInstance(this).enqueueUniquePeriodicWork(
        "MoodleSyncWork",
        ExistingPeriodicWorkPolicy.KEEP,
        syncRequest
    )
  }
}
