package com.example.data.database

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Configuration
    @Query("SELECT * FROM moodle_config WHERE id = 1 LIMIT 1")
    fun getConfig(): Flow<MoodleConfig?>

    @Query("SELECT * FROM moodle_config WHERE id = 1 LIMIT 1")
    suspend fun getConfigDirect(): MoodleConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: MoodleConfig)

    // Courses
    @Query("SELECT * FROM course ORDER BY fullname ASC")
    fun getAllCourses(): Flow<List<Course>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: Course)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<Course>)

    @Query("DELETE FROM course WHERE id = :id")
    suspend fun deleteCourse(id: Int)

    @Query("DELETE FROM course")
    suspend fun clearAllCourses()

    // Evaluations / Exams
    @Query("SELECT * FROM evaluation ORDER BY dueDate ASC")
    fun getAllEvaluations(): Flow<List<Evaluation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvaluation(evaluation: Evaluation)

    @Query("DELETE FROM evaluation WHERE id = :id")
    suspend fun deleteEvaluation(id: Int)

    @Query("DELETE FROM evaluation")
    suspend fun clearAllEvaluations()

    // Payments
    @Query("SELECT * FROM payment ORDER BY dueDate ASC")
    fun getAllPayments(): Flow<List<Payment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment)

    @Query("DELETE FROM payment WHERE id = :id")
    suspend fun deletePayment(id: Int)

    @Query("DELETE FROM payment")
    suspend fun clearAllPayments()

    // Academic Changes
    @Query("SELECT * FROM academic_change ORDER BY changeDate DESC")
    fun getAllAcademicChanges(): Flow<List<AcademicChange>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAcademicChange(academicChange: AcademicChange)

    @Query("DELETE FROM academic_change WHERE id = :id")
    suspend fun deleteAcademicChange(id: Int)

    @Query("DELETE FROM academic_change")
    suspend fun clearAllAcademicChanges()

    // Notification Logs
    @Query("SELECT * FROM notification_log ORDER BY timestamp DESC")
    fun getAllNotificationLogs(): Flow<List<NotificationLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificationLog(log: NotificationLog)

    @Query("DELETE FROM notification_log WHERE id = :id")
    suspend fun deleteNotificationLog(id: Int)

    @Query("DELETE FROM notification_log")
    suspend fun clearAllNotificationLogs()

    // Schedule Blocks
    @Query("SELECT * FROM schedule_block ORDER BY dayOfWeek, startTime ASC")
    fun getAllScheduleBlocks(): Flow<List<ScheduleBlock>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleBlock(scheduleBlock: ScheduleBlock)

    @Query("DELETE FROM schedule_block WHERE id = :id")
    suspend fun deleteScheduleBlock(id: Long)

    @Query("DELETE FROM schedule_block")
    suspend fun clearAllScheduleBlocks()

    // Calendar Events
    @Query("SELECT * FROM calendar_event ORDER BY dateMillis ASC")
    fun getAllCalendarEvents(): Flow<List<CalendarEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalendarEvent(calendarEvent: CalendarEvent)

    @Query("DELETE FROM calendar_event WHERE id = :id")
    suspend fun deleteCalendarEvent(id: Int)

    @Query("DELETE FROM calendar_event")
    suspend fun clearAllCalendarEvents()
}
