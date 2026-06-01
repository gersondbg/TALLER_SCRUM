package com.example.ui.viewmodel

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.example.data.repository.MoodleRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SyncState {
    object Idle : SyncState
    object Syncing : SyncState
    object Success : SyncState
    data class Error(val message: String) : SyncState
}

sealed interface TestConnectionState {
    object Idle : TestConnectionState
    object Testing : TestConnectionState
    data class Success(val fullname: String, val sitename: String) : TestConnectionState
    data class Error(val message: String) : TestConnectionState
}

class MoodleViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = MoodleRepository(database.appDao(), application)

    val config: StateFlow<MoodleConfig?> = repository.config
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val courses: StateFlow<List<Course>> = repository.courses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val evaluations: StateFlow<List<Evaluation>> = repository.evaluations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payments: StateFlow<List<Payment>> = repository.payments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val academicChanges: StateFlow<List<AcademicChange>> = repository.academicChanges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notificationLogs: StateFlow<List<NotificationLog>> = repository.notificationLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scheduleBlocks: StateFlow<List<ScheduleBlock>> = repository.scheduleBlocks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val calendarEvents: StateFlow<List<CalendarEvent>> = repository.calendarEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _testState = MutableStateFlow<TestConnectionState>(TestConnectionState.Idle)
    val testState: StateFlow<TestConnectionState> = _testState.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    // To display visual popups in app
    private val _inAppNotification = MutableStateFlow<NotificationLog?>(null)
    val inAppNotification: StateFlow<NotificationLog?> = _inAppNotification.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                // Configuración inicial por defecto si no existe
                if (repository.config.stateIn(viewModelScope).value == null) {
                    repository.saveConfig(MoodleConfig())
                }
            } catch (e: Exception) {
                Log.e("MoodleViewModel", "Init failed", e)
            }
        }
    }

    fun saveConfig(
        moodleUrl: String,
        wsToken: String,
        notificationsEnabled: Boolean,
        alertLeadTime: Int,
        academicAlerts: Boolean,
        paymentAlerts: Boolean,
        changesAlerts: Boolean,
        studentName: String,
        studentEmail: String
    ) {
        viewModelScope.launch {
            val current = config.value ?: MoodleConfig()
            val newConf = current.copy(
                moodleUrl = moodleUrl,
                wsToken = wsToken,
                notificationsEnabled = notificationsEnabled,
                alertLeadTimeMinutes = alertLeadTime,
                academicAlertsEnabled = academicAlerts,
                paymentAlertsEnabled = paymentAlerts,
                changesAlertsEnabled = changesAlerts,
                studentName = studentName,
                studentEmail = studentEmail
            )
            repository.saveConfig(newConf)
            _toastEvent.emit("Ajustes guardados correctamente")
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            try {
                repository.syncWithMoodle()
                // Usar la propiedad 'courses' del ViewModel que es StateFlow
                val coursesCount = courses.value.size
                if (coursesCount > 0) {
                    _syncState.value = SyncState.Success
                    _toastEvent.emit("¡Sincronización exitosa! $coursesCount cursos encontrados.")
                } else {
                    _syncState.value = SyncState.Idle
                    _toastEvent.emit("Conexión exitosa, pero no se encontraron cursos matriculados.")
                }
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Error desconocido en sync")
                _toastEvent.emit("Sync Error: ${e.message}")
            }
        }
    }

    fun resetSyncState() {
        _syncState.value = SyncState.Idle
    }

    fun testConnection(moodleUrl: String, wsToken: String) {
        viewModelScope.launch {
            _testState.value = TestConnectionState.Testing
            try {
                val response = repository.testMoodleConnection(moodleUrl, wsToken)
                _testState.value = TestConnectionState.Success(
                    fullname = response.fullname ?: response.username ?: "Estudiante",
                    sitename = response.sitename ?: "Moodle Cloud"
                )
                _toastEvent.emit("¡Conexión Exitosa con ${response.sitename}!")
            } catch (e: Exception) {
                _testState.value = TestConnectionState.Error(e.message ?: "Fallo de conexión")
                _toastEvent.emit("Error de conexión: ${e.message}")
            }
        }
    }

    fun resetTestState() {
        _testState.value = TestConnectionState.Idle
    }

    fun dismissInAppNotification() {
        _inAppNotification.value = null
    }

    // H4 Functions
    fun addCourse(
        fullname: String,
        shortname: String,
        category: String,
        defaultRoom: String,
        defaultSchedule: String
    ) {
        viewModelScope.launch {
            val id = (10000..99999).random() // Simulate Moodle ID
            val course = Course(
                id = id,
                fullname = fullname,
                shortname = shortname,
                category = category,
                defaultRoom = defaultRoom,
                defaultSchedule = defaultSchedule
            )
            repository.insertCourse(course)
            _toastEvent.emit("Curso registrado")

            repository.insertNotificationLog(NotificationLog(
                title = "Actividad: Curso Guardado",
                message = "Registraste el curso '${fullname}' (${shortname}).",
                type = "USER_ACTION"
            ))
        }
    }

    fun updateCourse(
        id: Int,
        fullname: String,
        shortname: String,
        category: String,
        defaultRoom: String,
        defaultSchedule: String
    ) {
        viewModelScope.launch {
            val course = Course(
                id = id,
                fullname = fullname,
                shortname = shortname,
                category = category,
                defaultRoom = defaultRoom,
                defaultSchedule = defaultSchedule
            )
            repository.insertCourse(course)
            _toastEvent.emit("Curso actualizado")

            repository.insertNotificationLog(NotificationLog(
                title = "Actividad: Curso Modificado",
                message = "Actualizaste la información del curso '${fullname}'.",
                type = "USER_ACTION"
            ))
        }
    }

    fun deleteCourse(id: Int) {
        viewModelScope.launch {
            val courseName = courses.value.find { it.id == id }?.fullname ?: "Curso"
            repository.deleteCourse(id)
            _toastEvent.emit("Curso eliminado")

            repository.insertNotificationLog(NotificationLog(
                title = "Actividad: Curso Eliminado",
                message = "Eliminaste el curso '${courseName}'.",
                type = "USER_ACTION"
            ))
        }
    }

    // H5 Functions
    fun addScheduleBlock(
        courseId: Int,
        courseName: String,
        dayOfWeek: String,
        startTime: String,
        endTime: String,
        room: String
    ) {
        viewModelScope.launch {
            val block = ScheduleBlock(
                courseId = courseId,
                courseName = courseName,
                dayOfWeek = dayOfWeek,
                startTime = startTime,
                endTime = endTime,
                room = room
            )
            repository.insertScheduleBlock(block)
            _toastEvent.emit("Bloque de horario registrado")

            repository.insertNotificationLog(NotificationLog(
                title = "Actividad: Horario Registrado",
                message = "Agregaste bloque de horario para '${courseName}' en '${dayOfWeek}' de ${startTime} a ${endTime}.",
                type = "USER_ACTION"
            ))
        }
    }

    fun updateScheduleBlock(
        id: Long,
        courseId: Int,
        courseName: String,
        dayOfWeek: String,
        startTime: String,
        endTime: String,
        room: String
    ) {
        viewModelScope.launch {
            val block = ScheduleBlock(
                id = id,
                courseId = courseId,
                courseName = courseName,
                dayOfWeek = dayOfWeek,
                startTime = startTime,
                endTime = endTime,
                room = room
            )
            repository.insertScheduleBlock(block)
            _toastEvent.emit("Bloque de horario actualizado")

            repository.insertNotificationLog(NotificationLog(
                title = "Actividad: Horario Actualizado",
                message = "Modificaste el bloque de horario para '${courseName}'.",
                type = "USER_ACTION"
            ))
        }
    }

    fun deleteScheduleBlock(id: Long) {
        viewModelScope.launch {
            val block = scheduleBlocks.value.find { it.id == id }
            val courseLabel = block?.courseName ?: "Asignatura"
            val dayLabel = block?.dayOfWeek ?: "Día"
            repository.deleteScheduleBlock(id)
            _toastEvent.emit("Bloque de horario eliminado")

            repository.insertNotificationLog(NotificationLog(
                title = "Actividad: Horario Eliminado",
                message = "Eliminaste bloque de horario de '${courseLabel}' del día ${dayLabel}.",
                type = "USER_ACTION"
            ))
        }
    }

    // H6 Functions
    fun addCalendarEvent(
        title: String,
        dateMillis: Long,
        type: String,
        courseId: Int,
        description: String
    ) {
        viewModelScope.launch {
            val event = CalendarEvent(
                title = title,
                dateMillis = dateMillis,
                type = type,
                courseId = courseId,
                description = description
            )
            repository.insertCalendarEvent(event)
            _toastEvent.emit("Evento de calendario registrado")

            repository.insertNotificationLog(NotificationLog(
                title = "Actividad: Calendario Registrado",
                message = "Agregaste un evento '${title}' (${type}) al calendario.",
                type = "USER_ACTION"
            ))
        }
    }

    fun deleteCalendarEvent(id: Long) {
        viewModelScope.launch {
            repository.deleteCalendarEvent(id)
            _toastEvent.emit("Evento de calendario eliminado")

            repository.insertNotificationLog(NotificationLog(
                title = "Actividad: Evento Eliminado",
                message = "Eliminaste un evento del calendario.",
                type = "USER_ACTION"
            ))
        }
    }

    // H7 Functions
    fun addEvaluation(
        title: String,
        courseId: Int,
        courseName: String,
        dueDate: Long,
        description: String,
        isAlertConfigured: Boolean,
        alertTimeMinutesBefore: Int
    ) {
        viewModelScope.launch {
            val eval = Evaluation(
                title = title,
                courseId = courseId,
                courseName = courseName,
                dueDate = dueDate,
                description = description,
                isAlertConfigured = isAlertConfigured,
                alertTimeMinutesBefore = alertTimeMinutesBefore,
                status = "PENDIENTE"
            )
            repository.insertEvaluation(eval)
            _toastEvent.emit("Evaluación/Examen registrado")

            repository.insertNotificationLog(NotificationLog(
                title = "Actividad: Evaluación Creada",
                message = "Registraste el examen/evaluación '${title}' para ${courseName}.",
                type = "USER_ACTION"
            ))

            if (isAlertConfigured) {
                val log = NotificationLog(
                    title = "Notificación Programada (H7)",
                    message = "Alerta programada para examen '$title' con un tiempo de $alertTimeMinutesBefore minutos.",
                    type = "ALERT"
                )
                repository.insertNotificationLog(log)
            }
        }
    }

    fun deleteEvaluation(id: Int) {
        viewModelScope.launch {
            val eval = evaluations.value.find { it.id == id }
            val titleLabel = eval?.title ?: "Evaluación"
            repository.deleteEvaluation(id)
            _toastEvent.emit("Examen eliminado")

            repository.insertNotificationLog(NotificationLog(
                title = "Actividad: Evaluación Eliminada",
                message = "Eliminaste el registro de '${titleLabel}'.",
                type = "USER_ACTION"
            ))
        }
    }

    // H8 Functions
    fun addPayment(concept: String, amount: Double, dueDate: Long) {
        viewModelScope.launch {
            val pay = Payment(
                concept = concept,
                amount = amount,
                dueDate = dueDate,
                status = "PENDIENTE"
            )
            repository.insertPayment(pay)
            _toastEvent.emit("Cargo de pago registrado")

            repository.insertNotificationLog(NotificationLog(
                title = "Actividad: Cargo Registrado",
                message = "Registraste una obligación de pago/matrícula: '${concept}' por s/. ${amount}.",
                type = "USER_ACTION"
            ))
        }
    }

    fun payPayment(id: Int, ref: String) {
        viewModelScope.launch {
            val paymentsList = payments.value
            val payment = paymentsList.find { it.id == id }
            if (payment != null) {
                val updated = payment.copy(
                    status = "PAGADO",
                    paymentDate = System.currentTimeMillis(),
                    transactionRef = ref
                )
                repository.insertPayment(updated)
                _toastEvent.emit("Pago procesado exitosamente")

                val log = NotificationLog(
                    title = "Pago de Matrícula Registrado",
                    message = "Transacción aprobada para: ${payment.concept} por s/. ${payment.amount}",
                    type = "PAYMENT"
                )
                repository.insertNotificationLog(log)

                repository.insertNotificationLog(NotificationLog(
                    title = "Actividad: Pago Realizado",
                    message = "Simulaste el pago exitoso de '${payment.concept}' por s/. ${payment.amount}.",
                    type = "USER_ACTION"
                ))
            }
        }
    }

    fun deletePayment(id: Int) {
        viewModelScope.launch {
            val payment = payments.value.find { it.id == id }
            val conceptLabel = payment?.concept ?: "Pago"
            repository.deletePayment(id)
            _toastEvent.emit("Pago eliminado")

            repository.insertNotificationLog(NotificationLog(
                title = "Actividad: Cargo Eliminado",
                message = "Eliminaste el registro de '${conceptLabel}'.",
                type = "USER_ACTION"
            ))
        }
    }

    // H9 Functions
    fun addAcademicChange(
        courseId: Int,
        courseName: String,
        oldRoom: String,
        newRoom: String,
        oldSched: String,
        newSched: String,
        description: String
    ) {
        viewModelScope.launch {
            val change = AcademicChange(
                courseId = courseId,
                courseName = courseName,
                oldRoom = oldRoom,
                newRoom = newRoom,
                oldSchedule = oldSched,
                newSchedule = newSched,
                changeDate = System.currentTimeMillis(),
                isNotified = true,
                description = description
            )
            repository.insertAcademicChange(change)
            _toastEvent.emit("Cambio de clase/aula registrado")

            val log = NotificationLog(
                title = "Cambio Aula/Horario: $courseName",
                message = "Clase de $oldRoom en $oldSched reubicada a $newRoom ($newSched). Motivo: $description",
                type = "ACADEMIC"
            )
            repository.insertNotificationLog(log)

            repository.insertNotificationLog(NotificationLog(
                title = "Actividad: Cambio Reportado",
                message = "Reportaste un cambio académico clínico/físico para '${courseName}'.",
                type = "USER_ACTION"
            ))

            triggerSystemNotification(
                title = "Notificación Académica (H9)",
                message = "Clase de $courseName reubicada a $newRoom."
            )

            _inAppNotification.value = log
        }
    }

    fun deleteAcademicChange(id: Int) {
        viewModelScope.launch {
            val change = academicChanges.value.find { it.id == id }
            val courseLabel = change?.courseName ?: "Curso"
            repository.deleteAcademicChange(id)
            _toastEvent.emit("Cambio académico eliminado")

            repository.insertNotificationLog(NotificationLog(
                title = "Actividad: Cambio Eliminado",
                message = "Eliminaste el registro de cambio para '${courseLabel}'.",
                type = "USER_ACTION"
            ))
        }
    }

    // H10 Push Recepción testing
    fun testAlertReception(category: String) {
        viewModelScope.launch {
            val (title, msg, type) = when (category) {
                "ALERT" -> Triple(
                    "¡Alerta de Evaluación Próxima! (H7)",
                    "Examen Parcial I (Plataformas) vence en 48 horas. ¡Repasa conectores Moodle!",
                    "ALERT"
                )
                "PAYMENT" -> Triple(
                    "¡Alerta de Pago Pendiente! (H8)",
                    "Tu cuota Matrícula Ciclo Especial por s/. 350.00 vence pronto.",
                    "PAYMENT"
                )
                else -> Triple(
                    "¡Cambio de Aula Urgente! (H9)",
                    "Desarrollo de Aplicaciones Móviles se traslada de Aula 405 al Auditorio Principal.",
                    "ACADEMIC"
                )
            }

            val log = NotificationLog(
                title = title,
                message = msg,
                type = type
            )
            repository.insertNotificationLog(log)
            _toastEvent.emit("Notificación recibida e ingresada al historial (H10)")

            triggerSystemNotification(title, msg)
            _inAppNotification.value = log
        }
    }

    fun clearNotificationLogs() {
        viewModelScope.launch {
            repository.clearNotificationLogs()
            _toastEvent.emit("Historial limpiado")
        }
    }

    fun deleteNotificationLog(id: Int) {
        viewModelScope.launch {
            repository.deleteNotificationLog(id)
        }
    }

    private fun triggerSystemNotification(title: String, message: String) {
        val context = getApplication<Application>().applicationContext
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "moodle_companion_alerts"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alertas Moodle",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones Moodle Cloud Companion"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            val configEnabled = config.value?.notificationsEnabled ?: true
            if (configEnabled) {
                notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
            }
        } catch (e: SecurityException) {
            Log.e("MoodleViewModel", "Post Notification permissions details", e)
        }
    }

    // H3 Profile Saving
    fun saveStudentProfile(
        name: String,
        email: String,
        code: String,
        career: String,
        cycle: String,
        phone: String,
        avatar: String
    ) {
        viewModelScope.launch {
            val current = config.value ?: MoodleConfig()
            val updated = current.copy(
                studentName = name,
                studentEmail = email,
                studentCode = code,
                studentCareer = career,
                studentCycle = cycle,
                studentPhone = phone,
                studentAvatarRes = avatar
            )
            repository.saveConfig(updated)
            _toastEvent.emit("Perfil estudiantil actualizado exitosamente")
        }
    }

    // H2 Login Student - INTEGRACIÓN REAL MOODLE CLOUD
    fun loginStudent(emailOrUser: String, passwordOrToken: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (emailOrUser.isBlank() || passwordOrToken.isBlank()) {
                onResult(false, "Usuario y contraseña requeridos")
                return@launch
            }

            // URL fija de la plataforma del proyecto
            val moodleUrl = "https://univirtual2026.moodlecloud.com"

            try {
                // Intento de autenticación real vía WebService (token.php)
                repository.loginWithMoodle(moodleUrl, emailOrUser.trim(), passwordOrToken.trim())
                
                onResult(true, "Conectado a Moodle Cloud con éxito")
                _toastEvent.emit("Sincronizando datos de Univirtual 2026...")

                val welcomeLog = NotificationLog(
                    title = "Acceso Real Moodle",
                    message = "Bienvenido alumno1. Datos sincronizados desde la nube.",
                    type = "ACADEMIC"
                )
                repository.insertNotificationLog(welcomeLog)
                _inAppNotification.value = welcomeLog
                
            } catch (e: Exception) {
                // Sin fallbacks locales: si Moodle lo rechaza, la app también.
                onResult(false, "Error Moodle: ${e.message}")
                _toastEvent.emit("Fallo de autenticación en Moodle Cloud")
            }
        }
    }

    // H2 Register Student
    fun registerStudent(
        name: String,
        email: String,
        code: String,
        career: String,
        cycle: String,
        phone: String,
        password: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            // Validations
            if (name.isBlank() || email.isBlank() || code.isBlank() || career.isBlank() || password.isBlank()) {
                onResult(false, "Por favor complete los campos obligatorios")
                return@launch
            }
            if (!email.contains("@") || !email.contains(".")) {
                onResult(false, "El formato del correo electrónico no es válido")
                return@launch
            }
            if (password.length < 4) {
                onResult(false, "La contraseña debe tener mínimo 4 caracteres")
                return@launch
            }

            // Save configuration as registered & logged in!
            val current = config.value ?: MoodleConfig()
            val newConf = current.copy(
                studentName = name,
                studentEmail = email,
                studentCode = code,
                studentCareer = career,
                studentCycle = if (cycle.isNotBlank()) cycle else "1er Ciclo",
                studentPhone = if (phone.isNotBlank()) phone else "+51 987 654 432",
                studentPassword = password,
                isLoggedIn = true // Login directly after registration!
            )
            repository.saveConfig(newConf)
            _toastEvent.emit("¡Registro completo!")
            onResult(true, "Registro exitoso")

            val welcomeLog = NotificationLog(
                title = "Estudiante Registrado",
                message = "Perfil de $name creado y conectado a Moodle Cloud.",
                type = "ACADEMIC"
            )
            repository.insertNotificationLog(welcomeLog)
            _inAppNotification.value = welcomeLog
        }
    }

    // H2 Recover Password
    fun recoverPassword(email: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (email.isBlank() || !email.contains("@")) {
                onResult(false, "Ingrese un correo electrónico válido")
                return@launch
            }

            val current = config.value ?: MoodleConfig()
            if (email.trim().equals(current.studentEmail.trim(), ignoreCase = true)) {
                onResult(true, "Clave recuperada. Tu contraseña es: ${current.studentPassword}")
                _toastEvent.emit("Contraseña recuperada")
            } else {
                onResult(true, "Si el correo está registrado en Moodle, recibirá las credenciales en su buzón simulado.")
                _toastEvent.emit("Simulación de recuperación enviada")
            }
        }
    }

    // Logout
    fun logoutStudent() {
        viewModelScope.launch {
            val current = config.value ?: return@launch
            val updated = current.copy(isLoggedIn = false)
            repository.saveConfig(updated)
            _toastEvent.emit("Sesión cerrada")
        }
    }

    // Select custom avatar
    fun updateStudentAvatar(avatarRes: String) {
        viewModelScope.launch {
            val current = config.value ?: return@launch
            val updated = current.copy(studentAvatarRes = avatarRes)
            repository.saveConfig(updated)
            _toastEvent.emit("Avatar actualizado")
        }
    }
}
