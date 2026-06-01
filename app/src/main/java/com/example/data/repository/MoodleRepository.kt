package com.example.data.repository

import android.content.Context
import android.util.Log
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
        appDao.deleteCalendarEvent(id)
    }

    suspend fun loginWithMoodle(url: String, user: String, pass: String): String {
        val sanitizedUrl = sanitizeMoodleUrl(url)
        val tokenUrl = "$sanitizedUrl/login/token.php"
        
        try {
            // Petición real de token a Moodle Cloud (ajustado nombres de parámetros)
            val response = moodleService.getToken(url = tokenUrl, user = user, pass = pass)
            
            if (response.token != null) {
                // Limpiar TODO lo anterior para asegurar que solo haya datos reales de esta sesión
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
                
                // Sincronizar inmediatamente datos de la nube
                syncWithMoodle()
                
                return response.token
            } else {
                val errorMsg = response.error ?: "Credenciales de Moodle incorrectas o servicio móvil desactivado."
                throw Exception(errorMsg)
            }
        } catch (e: Exception) {
            Log.e("MoodleRepository", "Fallo de autenticación en Moodle Cloud: ${e.message}")
            throw e
        }
    }

    suspend fun testMoodleConnection(url: String, token: String): SiteInfo {
        val sanitizedUrl = sanitizeMoodleUrl(url)
        val fullUrl = "$sanitizedUrl/webservice/rest/server.php"
        
        fullUrl.toHttpUrlOrNull() ?: throw IllegalArgumentException("URL de Moodle no válida")

        try {
            val response = moodleService.getSiteInfo(url = fullUrl, token = token)
            if (response.exception != null || response.errorcode != null) {
                throw Exception(response.message ?: "Error desde Moodle: ${response.errorcode}")
            }
            return response
        } catch (e: Exception) {
            Log.e("MoodleRepository", "Connection test failed", e)
            throw e
        }
    }

    suspend fun syncWithMoodle() {
        val currentConfig = appDao.getConfigDirect() ?: return
        if (currentConfig.moodleUrl.isBlank() || currentConfig.wsToken.isBlank()) {
            throw Exception("Por favor configura la URL y el Token en Ajustes.")
        }

        val sanitizedUrl = sanitizeMoodleUrl(currentConfig.moodleUrl)
        val fullUrl = "$sanitizedUrl/webservice/rest/server.php"

        try {
            val siteInfo = moodleService.getSiteInfo(url = fullUrl, token = currentConfig.wsToken)
            
            // Manejo específico del error de políticas
            if (siteInfo.errorcode == "sitepolicynotagreed") {
                throw Exception("BLOQUEO DE MOODLE: Debes entrar a la web y aceptar las 'Políticas y Acuerdos' para permitir la sincronización.")
            }

            if (siteInfo.exception != null || siteInfo.errorcode != null) {
                throw Exception(siteInfo.message ?: "Error desde Moodle: ${siteInfo.errorcode}")
            }

            // Actualizar perfil con datos reales de Moodle (H3)
            val updatedConfig = currentConfig.copy(
                studentName = siteInfo.fullname ?: "${siteInfo.firstname} ${siteInfo.lastname}".trim(),
                studentEmail = siteInfo.username?.let { "$it@univirtual2026.moodlecloud.com" } ?: currentConfig.studentEmail
            )
            appDao.insertConfig(updatedConfig)

            val mCourses = mutableListOf<MoodleCourse>()

            // Intento 1: core_enrol_get_users_courses (Tradicional)
            try {
                val courses1 = moodleService.getUserCourses(
                    url = fullUrl,
                    token = currentConfig.wsToken,
                    userId = siteInfo.userid
                )
                mCourses.addAll(courses1)
                Log.d("MoodleRepository", "Intento 1 Exitoso: ${courses1.size} cursos.")
            } catch (e: Exception) {
                Log.e("MoodleRepository", "ERROR CRÍTICO INTENTO 1: ${e.message}")
            }

            // Intento 2: core_course_get_enrolled_courses_by_timeline_classification
            // Probamos con "all" para traer pasados, presentes y FUTUROS
            try {
                val timelineResponse = moodleService.getEnrolledCoursesByTimeline(
                    url = fullUrl,
                    token = currentConfig.wsToken,
                    classification = "all" 
                )
                if (timelineResponse.courses.isNotEmpty()) {
                    mCourses.addAll(timelineResponse.courses)
                    Log.d("MoodleRepository", "Intento 2 (ALL) exitoso: ${timelineResponse.courses.size} cursos.")
                }
            } catch (e: Exception) {
                Log.e("MoodleRepository", "Intento 2 falló: ${e.message}")
            }

            // Intento 3: Cursos recientes
            try {
                val recent = moodleService.getRecentCourses(url = fullUrl, token = currentConfig.wsToken)
                mCourses.addAll(recent)
                Log.d("MoodleRepository", "Intento 3: ${recent.size} cursos encontrados.")
            } catch (e: Exception) {
                Log.e("MoodleRepository", "Intento 3 falló: ${e.message}")
            }

            if (mCourses.isNotEmpty()) {
                // Eliminar duplicados por ID y filtrar nulos
                val uniqueCourses = mCourses.filter { it.id > 0 }.distinctBy { it.id }
                val dbCourses = uniqueCourses.map { mc ->
                    Course(
                        id = mc.id,
                        fullname = mc.fullname ?: mc.displayname ?: "Curso ${mc.id}",
                        shortname = mc.shortname ?: "C-${mc.id}",
                        category = "Moodle Cloud",
                        defaultRoom = "Aula Virtual",
                        defaultSchedule = "Sincronizado"
                    )
                }
                appDao.clearAllCourses() // Limpiar para evitar basura de sesiones anteriores
                appDao.insertCourses(dbCourses)
                Log.d("MoodleRepository", "Total cursos guardados en DB: ${dbCourses.size}")
            }

            // Sync Calendar Events (H6)
            try {
                val calendarResponse = moodleService.getCalendarEvents(
                    url = fullUrl,
                    token = currentConfig.wsToken
                )
                
                if (calendarResponse.events.isNotEmpty()) {
                    val dbEvents = calendarResponse.events.map { me ->
                        CalendarEvent(
                            id = me.id,
                            title = me.name,
                            dateMillis = me.timestart * 1000L,
                            type = mapMoodleEventType(me.eventtype, me.name),
                            courseId = me.courseid ?: 0,
                            description = me.description ?: ""
                        )
                    }
                    
                    appDao.clearAllCalendarEvents() // Limpiar para reflejar estado actual de la nube
                    for (event in dbEvents) {
                        appDao.insertCalendarEvent(event)
                    }
                    Log.d("MoodleRepository", "Sincronizados ${dbEvents.size} eventos de calendario.")
                } else {
                    Log.d("MoodleRepository", "No se encontraron eventos de calendario.")
                }
            } catch (e: Exception) {
                Log.e("MoodleRepository", "Calendar sync failed: ${e.message}")
            }

            val syncLog = NotificationLog(
                title = "Sincronización Exitosa",
                message = "Se sincronizaron ${mCourses.size} cursos y eventos desde Moodle Cloud.",
                type = "ACADEMIC"
            )
            appDao.insertNotificationLog(syncLog)
        } catch (e: Exception) {
            Log.e("MoodleRepository", "Sync failed", e)
            throw e
        }
    }

    private fun mapMoodleEventType(type: String?, name: String): String {
        val lowerName = name.lowercase()
        return when {
            lowerName.contains("examen") || lowerName.contains("parcial") || lowerName.contains("final") -> "EXAMEN"
            lowerName.contains("pago") || lowerName.contains("cuota") -> "PAGO"
            type == "course" -> "ACADEMICO"
            type == "user" -> "PERSONAL"
            else -> "GENERAL"
        }
    }

    private fun sanitizeMoodleUrl(url: String): String {
        var clean = url.trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "https://$clean"
        }
        if (clean.endsWith("/")) {
            clean = clean.dropLast(1)
        }
        return clean
    }

    suspend fun prepopulateIfEmpty() {
        // FUNCIÓN DESACTIVADA: No queremos datos locales, todo debe venir de Moodle Cloud
        val currentConfig = appDao.getConfigDirect()
        if (currentConfig == null) {
            val defaultConfig = MoodleConfig()
            appDao.insertConfig(defaultConfig)
        }
        Log.d("MoodleRepository", "Prepopulate omitido: Modo 100% Cloud activo")
    }
}
