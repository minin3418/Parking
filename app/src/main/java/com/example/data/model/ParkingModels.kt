package com.example.data.model

import com.squareup.moshi.Json

data class ParkingData(
    @Json(name = "spots") val spots: List<ParkingSpot> = emptyList(),
    @Json(name = "landmarks") val landmarks: List<ParkingLandmark> = emptyList(),
    @Json(name = "blockers") val blockers: List<ParkingBlocker> = emptyList(),
    @Json(name = "surveys") val surveys: List<SurveyItem>? = null
)

data class ParkingSpot(
    @Json(name = "id") val id: Int,
    @Json(name = "x") val x: Float,
    @Json(name = "y") val y: Float,
    @Json(name = "angle") val angle: Float? = 0f,
    @Json(name = "occupiedBy") val occupiedBy: Occupant? = null
) {
    val isFree: Boolean get() = occupiedBy == null
}

data class Occupant(
    @Json(name = "carNumber") val carNumber: String = "",
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "apt") val apt: String? = null,
    @Json(name = "parkedAt") val parkedAt: String? = null,
    @Json(name = "leaveTime") val leaveTime: String? = null
)

data class ParkingLandmark(
    @Json(name = "id") val id: String = "",
    @Json(name = "text") var text: String = "",
    @Json(name = "x") val x: Float = 0f,
    @Json(name = "y") val y: Float = 0f
)

data class ParkingBlocker(
    @Json(name = "id") val id: String = "",
    @Json(name = "carNumber") val carNumber: String = "",
    @Json(name = "phone") val phone: String = "",
    @Json(name = "apt") val apt: String? = null,
    @Json(name = "leaveTime") val leaveTime: String? = null,
    @Json(name = "blockedSpots") val blockedSpots: List<Int> = emptyList(),
    @Json(name = "parkedAt") val parkedAt: String? = null
)

data class SurveyItem(
    @Json(name = "id") val id: Long = 0L,
    @Json(name = "answer") val answer: String = "",
    @Json(name = "comment") val comment: String? = null,
    @Json(name = "date") val date: String? = null
)

// Request payloads matching backend server.js
data class Row1ParkRequest(
    @Json(name = "spotId") val spotId: Int,
    @Json(name = "carNumber") val carNumber: String,
    @Json(name = "phone") val phone: String? = "",
    @Json(name = "apt") val apt: String? = "",
    @Json(name = "leaveTime") val leaveTime: String? = ""
)

data class Row1LeaveRequest(
    @Json(name = "spotId") val spotId: Int,
    @Json(name = "lastDigits") val lastDigits: String = "",
    @Json(name = "pin") val pin: String = ""
)

data class Row2ParkRequest(
    @Json(name = "carNumber") val carNumber: String,
    @Json(name = "phone") val phone: String,
    @Json(name = "blockedSpots") val blockedSpots: List<Int>,
    @Json(name = "leaveTime") val leaveTime: String = ""
)

data class Row2LeaveRequest(
    @Json(name = "blockerId") val blockerId: String,
    @Json(name = "lastDigits") val lastDigits: String = "",
    @Json(name = "pin") val pin: String = ""
)

data class SurveyRequest(
    @Json(name = "answer") val answer: String,
    @Json(name = "comment") val comment: String? = ""
)

data class AdminLoginRequest(
    @Json(name = "pin") val pin: String
)

data class AdminPinRequest(
    @Json(name = "pin") val pin: String
)

data class SaveMapConfigRequest(
    @Json(name = "pin") val pin: String,
    @Json(name = "landmarks") val landmarks: List<ParkingLandmark>,
    @Json(name = "spots") val spots: List<ParkingSpot>,
    @Json(name = "blockers") val blockers: List<ParkingBlocker>? = emptyList()
)

data class GenericResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "message") val message: String? = null
)

data class YardItem(
    @Json(name = "id") val id: String,
    @Json(name = "address") val address: String,
    @Json(name = "name") val name: String = "",
    @Json(name = "city") val city: String = "Владивосток",
    @Json(name = "spotsCount") val spotsCount: Int = 0,
    @Json(name = "freeSpotsCount") val freeSpotsCount: Int = 0,
    @Json(name = "lastVisited") val lastVisited: Long = System.currentTimeMillis(),
    @Json(name = "isDefault") val isDefault: Boolean = false
)

// Entry of the shared address list (yards/index.json in the GitHub repo)
data class SharedYardEntry(
    @Json(name = "id") val id: String,
    @Json(name = "address") val address: String,
    @Json(name = "createdAt") val createdAt: Long? = null
)

enum class YardTemplateType(val title: String, val description: String, val spotsCount: Int) {
    STANDARD_2ROWS("Типовой двор (2 ряда)", "20 мест, 2 параллельных ряда, въезд и дом", 20),
    COMPACT("Компактный двор", "12 мест, угловая расстановка, дом и шлагбаум", 12),
    COPY_KARBYSHEVA("Схема Карбышева 22", "31 место, 2 ряда (полная схема прототипа)", 31),
    EMPTY("Чистый двор (для разметки)", "Без мест, с ориентиром въезда (для админа)", 0)
}

