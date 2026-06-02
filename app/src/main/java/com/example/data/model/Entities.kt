package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "moodle_config")
data class MoodleConfig(
    @PrimaryKey val id: Int = 1,
    val moodleUrl: String = "https://univirtual2026.moodlecloud.com",
    val wsToken: String = "ad061f990e79c16f2e6761961f5c0386",
    val notificationsEnabled: Boolean = true,
    val alertLeadTimeMinutes: Int = 15,
    val academicAlertsEnabled: Boolean = true,
    val paymentAlertsEnabled: Boolean = true,
    val changesAlertsEnabled: Boolean = true,
    val studentName: String = "Alumno Univirtual",
    val studentEmail: String = "alumno1@univirtual2026.moodlecloud.com",
    val studentCode: String = "2026-FIIS",
    val studentCareer: String = "Ingeniería",
    val studentCycle: String = "2026-I",
    val studentPhone: String = "",
    val studentAvatarRes: String = "avatar_1",
    val isLoggedIn: Boolean = false,
    val studentPassword: String = ""
)

@Entity(tableName = "course")
data class Course(
    @PrimaryKey val id: Int,
    val fullname: String,
    val shortname: String,
    val category: String = "General",
    val defaultRoom: String = "Aula 101",
    val defaultSchedule: String = "Lunes y Miércoles 08:00 - 10:00"
)

@Entity(tableName = "evaluation")
data class Evaluation(
    @PrimaryKey val id: Int, // Usamos el ID de Moodle (module id)
    val title: String,
    val courseId: Int,
    val courseName: String,
    val dueDate: Long,
    val description: String = "",
    val isAlertConfigured: Boolean = true,
    val alertTimeMinutesBefore: Int = 15,
    val status: String = "PENDIENTE" // PENDING, COMPLETED, OVERDUE
)

@Entity(tableName = "payment")
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val concept: String,
    val amount: Double,
    val dueDate: Long,
    val status: String = "PENDIEN", // PENDIEN, PAGADO, VENCIDO
    val paymentDate: Long? = null,
    val transactionRef: String? = null
)

@Entity(tableName = "academic_change")
data class AcademicChange(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val courseId: Int,
    val courseName: String,
    val oldRoom: String,
    val newRoom: String,
    val oldSchedule: String,
    val newSchedule: String,
    val changeDate: Long,
    val isNotified: Boolean = false,
    val description: String = ""
)

@Entity(tableName = "notification_log")
data class NotificationLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val message: String,
    val type: String, // ALERT, PAYMENT, ACADEMIC
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "schedule_block")
data class ScheduleBlock(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Int,
    val courseName: String,
    val dayOfWeek: String, // "Lunes", "Martes", etc.
    val startTime: String, // "HH:mm"
    val endTime: String,
    val room: String
)

@Entity(tableName = "calendar_event")
data class CalendarEvent(
    @PrimaryKey val id: Int, // Usamos el ID de Moodle para evitar duplicados
    val title: String,
    val dateMillis: Long,
    val type: String, // "EXAMEN", "PAGO", "GENERAL"
    val courseId: Int,
    val description: String
)
