package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SiteInfo(
    val sitename: String? = null,
    val username: String? = null,
    val firstname: String? = null,
    val lastname: String? = null,
    val fullname: String? = null,
    val lang: String? = null,
    val userid: Int = 0,
    val userpictureurl: String? = null,
    val exception: String? = null,
    val errorcode: String? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class MoodleCourse(
    val id: Int,
    val shortname: String? = null,
    val fullname: String? = null,
    val displayname: String? = null,
    val idnumber: String? = null,
    val visible: Int? = null,
    val summary: String? = null,
    val summaryformat: Int? = null,
    val format: String? = null,
    val showgrades: Boolean? = null,
    val lang: String? = null,
    val category: Int? = null
)

@JsonClass(generateAdapter = true)
data class CalendarEventResponse(
    val events: List<MoodleCalendarEvent>
)

@JsonClass(generateAdapter = true)
data class EnrolledCoursesResponse(
    val courses: List<MoodleCourse>,
    val nextoffset: Int? = null
)

@JsonClass(generateAdapter = true)
data class MoodleCalendarEvent(
    val id: Int,
    val name: String,
    val description: String?,
    val format: Int?,
    val courseid: Int?,
    val userid: Int?,
    val repeatid: Int?,
    val moduleid: Int?,
    val instance: Int?,
    val eventtype: String?,
    val timestart: Long,
    val timeduration: Long,
    val visible: Int?,
    val sequence: Int?,
    val timemodified: Long
)

@JsonClass(generateAdapter = true)
data class MoodleError(
    val exception: String?,
    val errorcode: String?,
    val message: String?,
    val debuginfo: String?
)
