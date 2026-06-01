package com.example.data.api

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface MoodleApiService {

    @GET
    suspend fun getSiteInfo(
        @Url url: String,
        @Query("wstoken") token: String,
        @Query("wsfunction") function: String = "core_webservice_get_site_info",
        @Query("moodlewsrestformat") format: String = "json"
    ): SiteInfo

    @GET
    suspend fun getUserCourses(
        @Url url: String,
        @Query("wstoken") token: String,
        @Query("userid") userId: Int,
        @Query("wsfunction") function: String = "core_enrol_get_users_courses",
        @Query("moodlewsrestformat") format: String = "json"
    ): List<MoodleCourse>

    @GET
    suspend fun getRecentCourses(
        @Url url: String,
        @Query("wstoken") token: String,
        @Query("wsfunction") function: String = "core_course_get_recent_courses",
        @Query("moodlewsrestformat") format: String = "json"
    ): List<MoodleCourse>

    @GET
    suspend fun getEnrolledCoursesByTimeline(
        @Url url: String,
        @Query("wstoken") token: String,
        @Query("wsfunction") function: String = "core_course_get_enrolled_courses_by_timeline_classification",
        @Query("classification") classification: String = "all",
        @Query("moodlewsrestformat") format: String = "json"
    ): EnrolledCoursesResponse

    @GET
    suspend fun getCalendarEvents(
        @Url url: String,
        @Query("wstoken") token: String,
        @Query("wsfunction") function: String = "core_calendar_get_calendar_events",
        @Query("moodlewsrestformat") format: String = "json"
    ): CalendarEventResponse

    @GET
    suspend fun getToken(
        @Url url: String,
        @Query("username") user: String,
        @Query("password") pass: String,
        @Query("service") service: String = "moodle_mobile_app"
    ): MoodleTokenResponse
}

@JsonClass(generateAdapter = true)
data class MoodleTokenResponse(
    val token: String? = null,
    val error: String? = null
)
