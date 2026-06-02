package com.example.data.repository

import android.content.Context
import android.util.Log
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.data.api.*
import com.example.data.database.AppDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class MoodleRepository(
    private val appDao: AppDao,
    private val context: Context
) {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://placeholder.invalid/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val moodleService = retrofit.create(MoodleApiService::class.java)

    val config: Flow<MoodleConfig?> = appDao.getConfig()
    val courses: Flow<List<Course>> = appDao.getAllCourses()
    val evaluations: Flow<List<Evaluation>> = appDao.getAllEvaluations()
    val payments: Flow<List<Payment>> = appDao.getAllPayments()
    val academicChanges: Flow<List<AcademicChange>> = appDao.getAllAcademicChanges()
    val notificationLogs: Flow<List<NotificationLog>> = appDao.getAllNotificationLogs()
    val scheduleBlocks: Flow<List<ScheduleBlock>> = appDao.getAllScheduleBlocks()
    val calendarEvents: Flow<List<CalendarEvent>> = appDao.getAllCalendarEvents()

    suspend fun saveConfig(config: MoodleConfig) {
        appDao.insertConfig(config)
    }

    suspend fun insertEvaluation(evaluation: Evaluation) {
        appDao.insertEvaluation(evaluation)
    }

    suspend fun deleteEvaluation(id: Int) {
        appDao.deleteEvaluation(id)
    }

    suspend fun insertPayment(payment: Payment) {
        appDao.insertPayment(payment)
    }

    suspend fun deletePayment(id: Int) {
        appDao.deletePayment(id)
    }

    suspend fun insertAcademicChange(academicChange: AcademicChange) {
        appDao.insertAcademicChange(academicChange)
    }

    suspend fun deleteAcademicChange(id: Int) {
        appDao.deleteAcademicChange(id)
    }

    suspend fun insertNotificationLog(log: NotificationLog) {
        appDao.insertNotificationLog(log)
    }

    suspend fun clearNotificationLogs() {
        appDao.clearAllNotificationLogs()
    }

    suspend fun deleteNotificationLog(id: Int) {
        appDao.deleteNotificationLog(id)
    }

    // Courses
    suspend fun insertCourse(course: Course) {
        appDao.insertCourse(course)
    }

    suspend fun deleteCourse(id: Int) {
        appDao.deleteCourse(id)
    }

    // Schedule Blocks
    suspend fun insertScheduleBlock(scheduleBlock: ScheduleBlock) {
        appDao.insertScheduleBlock(scheduleBlock)
    }

    suspend fun deleteScheduleBlock(id: Long) {
        appDao.deleteScheduleBlock(id)
    }

    // Calendar Events
    suspend fun insertCalendarEvent(calendarEvent: CalendarEvent) {
        appDao.insertCalendarEvent(calendarEvent)
    }

    suspend fun deleteCalendarEvent(id: Long) {
        appDao.deleteCalendarEvent(id.toInt())
    }

    private suspend fun triggerLocalNotification(title: String, message: String, type: String = "GENERAL") {
        appDao.insertNotificationLog(NotificationLog(
            title = title,
            message = message,
            type = type
        ))

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "moodle_alerts_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alertas de Moodle Cloud",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de cambios en cursos y tareas"
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

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    suspend fun loginWithMoodle(url: String, user: String, pass: String): String {
        val sanitizedUrl = sanitizeMoodleUrl(url)
        val tokenUrl = "$sanitizedUrl/login/token.php"
        
        try {
            val response = moodleService.getToken(url = tokenUrl, user = user, pass = pass)
            
            if (response.token != null) {
                appDao.clearAllCourses()
                appDao.clearAllEvaluations()
                appDao.clearAllPayments()
                appDao.clearAllScheduleBlocks()
                appDao.clearAllNotificationLogs()
                appDao.clearAllCalendarEvents()

                val currentConfig = appDao.getConfigDirect() ?: MoodleConfig()
                val updatedConfig = currentConfig.copy(
                    moodleUrl = sanitizedUrl,
                    wsToken = response.token,
                    isLoggedIn = true,
                    studentEmail = "$user@univirtual2026.moodlecloud.com",
                    studentName = user
                )
                appDao.insertConfig(updatedConfig)
                syncWithMoodle()
                return response.token
            } else {
                val errorMsg = response.error ?: "Credenciales incorrectas."
                throw Exception(errorMsg)
            }
        } catch (e: Exception) {
            Log.e("MoodleRepository", "Login failed", e)
            throw e
        }
    }

    suspend fun testMoodleConnection(url: String, token: String): SiteInfo {
        val sanitizedUrl = sanitizeMoodleUrl(url)
        val fullUrl = "$sanitizedUrl/webservice/rest/server.php"
        try {
            val response = moodleService.getSiteInfo(url = fullUrl, token = token)
            if (response.exception != null || response.errorcode != null) {
                throw Exception(response.message ?: "Error de Moodle")
            }
            return response
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun syncWithMoodle() {
        val currentConfig = appDao.getConfigDirect() ?: return
        if (currentConfig.moodleUrl.isBlank() || currentConfig.wsToken.isBlank()) return

        val sanitizedUrl = sanitizeMoodleUrl(currentConfig.moodleUrl)
        val fullUrl = "$sanitizedUrl/webservice/rest/server.php"

        try {
            val siteInfo = moodleService.getSiteInfo(url = fullUrl, token = currentConfig.wsToken)
            
            val updatedConfig = currentConfig.copy(
                studentName = siteInfo.fullname ?: "${siteInfo.firstname} ${siteInfo.lastname}".trim(),
                studentEmail = siteInfo.username?.let { "$it@univirtual2026.moodlecloud.com" } ?: currentConfig.studentEmail
            )
            appDao.insertConfig(updatedConfig)

            val mCourses = mutableListOf<MoodleCourse>()
            try {
                mCourses.addAll(moodleService.getUserCourses(fullUrl, currentConfig.wsToken, siteInfo.userid))
            } catch (e: Exception) { Log.e("MoodleRepository", "Error sync cursos", e) }

            if (mCourses.isNotEmpty()) {
                val uniqueCourses = mCourses.filter { it.id > 0 }.distinctBy { it.id }
                val courseIds = uniqueCourses.map { it.id }
                
                appDao.insertCourses(uniqueCourses.map { mc ->
                    Course(
                        id = mc.id,
                        fullname = mc.fullname ?: mc.displayname ?: "Curso ${mc.id}",
                        shortname = mc.shortname ?: "C-${mc.id}"
                    )
                })

                // 1. Sincronizar Tareas (Assignments)
                try {
                    val assignmentResponse = moodleService.getAssignments(fullUrl, currentConfig.wsToken)
                    for (aCourse in assignmentResponse.courses) {
                        for (assign in aCourse.assignments) {
                            processActivity(
                                id = assign.cmid,
                                name = assign.name,
                                courseId = assign.course,
                                courseName = aCourse.fullname,
                                dueDateMillis = assign.duedate * 1000L,
                                description = ""
                            )
                        }
                    }
                } catch (e: Exception) { Log.e("MoodleRepository", "Error sync assignments", e) }

                // 2. Sincronizar Cuestionarios (Quizzes)
                try {
                    val quizResponse = moodleService.getQuizzes(fullUrl, currentConfig.wsToken, courseIds)
                    for (quiz in quizResponse.quizzes) {
                        val courseName = uniqueCourses.find { it.id == quiz.course }?.fullname ?: "Curso"
                        processActivity(
                            id = quiz.id, // Nota: Quiz no siempre tiene CMID fácil de obtener aquí, usamos id
                            name = quiz.name,
                            courseId = quiz.course,
                            courseName = courseName,
                            dueDateMillis = quiz.timeclose * 1000L,
                            description = ""
                        )
                    }
                } catch (e: Exception) { Log.e("MoodleRepository", "Error sync quizzes", e) }

                // 3. Scan genérico de contenidos (se mantiene por si hay otros tipos)
                for (course in uniqueCourses) {
                    try {
                        val sections = moodleService.getCourseContents(fullUrl, currentConfig.wsToken, course.id)
                        for (section in sections) {
                            for (module in section.modules) {
                                val dueDate = module.dates.find { it.label.contains("Cierre") || it.label.contains("vence") }?.timestamp
                                if (dueDate != null) {
                                    val existing = appDao.getAllEvaluationsDirect().find { it.id == module.id }
                                    val newEval = Evaluation(
                                        id = module.id,
                                        title = module.name,
                                        courseId = course.id,
                                        courseName = course.fullname ?: "Curso",
                                        dueDate = dueDate * 1000L,
                                        description = module.description ?: ""
                                    )

                                    if (existing == null) {
                                        triggerLocalNotification("Nueva Actividad", "${course.shortname}: ${module.name}", "ALERT")
                                        appDao.insertEvaluation(newEval)
                                    } else if (existing.dueDate != newEval.dueDate) {
                                        triggerLocalNotification("Fecha Cambiada", "La actividad '${module.name}' del curso ${course.shortname} ha sido reprogramada para el ${java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date(newEval.dueDate))}", "ACADEMIC")
                                        appDao.insertEvaluation(newEval)
                                        
                                        // Registrar cambio académico formalmente si es necesario
                                        appDao.insertAcademicChange(AcademicChange(
                                            courseId = course.id,
                                            courseName = course.fullname ?: "Curso",
                                            oldRoom = "N/A",
                                            newRoom = "N/A",
                                            oldSchedule = java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date(existing.dueDate)),
                                            newSchedule = java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date(newEval.dueDate)),
                                            changeDate = System.currentTimeMillis(),
                                            description = "Reprogramación de actividad: ${module.name}"
                                        ))
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) { Log.e("MoodleRepository", "Scan error ${course.id}", e) }
                }
            }

            try {
                val calendar = moodleService.getCalendarEvents(fullUrl, currentConfig.wsToken)
                val existingEvents = appDao.getAllCalendarEventsDirect()

                for (me in calendar.events) {
                    val type = mapMoodleEventType(me.eventtype, me.name)
                    val dbEvent = CalendarEvent(
                        id = me.id,
                        title = me.name,
                        dateMillis = me.timestart * 1000L,
                        type = type,
                        courseId = me.courseid ?: 0,
                        description = me.description ?: ""
                    )

                    val old = existingEvents.find { it.id == me.id }
                    if (old == null) {
                        if (type == "EXAMEN") {
                            triggerLocalNotification("Nuevo Examen Detectado", "${me.name} - Fecha: ${java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date(me.timestart * 1000L))}", "ALERT")
                        } else if (type == "PAGO") {
                            triggerLocalNotification("Nuevo Pago Pendiente", "Se ha registrado un pago para: ${me.name}", "PAYMENT")
                        } else {
                            triggerLocalNotification("Nuevo Evento", me.name, "ALERT")
                        }
                    } else if (old.dateMillis != dbEvent.dateMillis) {
                        triggerLocalNotification("Evento Modificado", "La fecha de '${me.name}' ha cambiado al ${java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date(dbEvent.dateMillis))}", "ACADEMIC")
                    }

                    if (type == "PAGO") {
                        val existingPayment = appDao.getAllPaymentsDirect().find { it.concept == me.name && it.dueDate == me.timestart * 1000L }
                        if (existingPayment == null) {
                            val payment = Payment(
                                concept = me.name,
                                amount = 0.0,
                                dueDate = me.timestart * 1000L,
                                status = "PENDIENTE"
                            )
                            appDao.insertPayment(payment)
                            
                            val threeDaysInMillis = 3 * 24 * 60 * 60 * 1000L
                            val timeDiff = payment.dueDate - System.currentTimeMillis()
                            if (timeDiff in 0..threeDaysInMillis) {
                                triggerLocalNotification("Recordatorio de Pago", "La cuota '${me.name}' vence pronto (${java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault()).format(java.util.Date(payment.dueDate))}).", "PAYMENT")
                            }
                        }
                    }
                    appDao.insertCalendarEvent(dbEvent)
                }
            } catch (e: Exception) { Log.e("MoodleRepository", "Calendar error", e) }

        } catch (e: Exception) {
            Log.e("MoodleRepository", "Global sync failure", e)
        }
    }

    private fun mapMoodleEventType(type: String?, name: String): String {
        val n = name.lowercase()
        val t = type?.lowercase() ?: ""
        return when {
            n.contains("pago") || n.contains("cuota") || t.contains("pay") -> "PAGO"
            n.contains("examen") || n.contains("parcial") || n.contains("final") || t.contains("exam") -> "EXAMEN"
            else -> "GENERAL"
        }
    }

    private fun sanitizeMoodleUrl(url: String): String {
        var clean = url.trim()
        if (!clean.startsWith("http")) clean = "https://$clean"
        if (clean.endsWith("/")) clean = clean.dropLast(1)
        return clean
    }

    suspend fun prepopulateIfEmpty() {
        if (appDao.getConfigDirect() == null) {
            appDao.insertConfig(MoodleConfig())
        }
    }

    private suspend fun processActivity(id: Int, name: String, courseId: Int, courseName: String, dueDateMillis: Long, description: String) {
        if (dueDateMillis <= 0) return
        
        val existing = appDao.getAllEvaluationsDirect().find { it.id == id }
        val newEval = Evaluation(
            id = id,
            title = name,
            courseId = courseId,
            courseName = courseName,
            dueDate = dueDateMillis,
            description = description
        )

        if (existing == null) {
            triggerLocalNotification("Nueva Actividad", "$courseName: $name", "ALERT")
            appDao.insertEvaluation(newEval)
        } else if (existing.dueDate != newEval.dueDate) {
            triggerLocalNotification("Fecha Cambiada", "La actividad '$name' ha sido reprogramada para el ${java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date(newEval.dueDate))}", "ACADEMIC")
            appDao.insertEvaluation(newEval)
            
            appDao.insertAcademicChange(AcademicChange(
                courseId = courseId,
                courseName = courseName,
                oldRoom = "N/A",
                newRoom = "N/A",
                oldSchedule = java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date(existing.dueDate)),
                newSchedule = java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date(newEval.dueDate)),
                changeDate = System.currentTimeMillis(),
                description = "Reprogramación de actividad: $name"
            ))
        }
    }
}
