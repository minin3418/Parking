package com.example.data.model

import java.util.Locale

object YardTemplates {

    val DEFAULT_YARD = YardItem(
        id = "karbysheva_22",
        address = "ул. Карбышева, 22 / 22А",
        name = "Двор Карбышева 22 / 22А",
        city = "Владивосток",
        spotsCount = 31,
        freeSpotsCount = 20,
        isDefault = true
    )

    val INITIAL_KNOWN_YARDS = listOf(
        DEFAULT_YARD,
        YardItem(
            id = "yard_okeansky_100",
            address = "Океанский проспект, 100",
            name = "Двор Океанский 100",
            city = "Владивосток",
            spotsCount = 20,
            freeSpotsCount = 20
        ),
        YardItem(
            id = "yard_svetlanskaya_45",
            address = "ул. Светланская, 45",
            name = "Двор Светланская 45",
            city = "Владивосток",
            spotsCount = 12,
            freeSpotsCount = 12
        ),
        YardItem(
            id = "yard_russkaya_46",
            address = "ул. Русская, 46",
            name = "Двор Русская 46",
            city = "Владивосток",
            spotsCount = 18,
            freeSpotsCount = 18
        )
    )

    fun normalizeAddressToId(address: String): String {
        val trimmed = address.trim()
        if (isMasterKarbyshevaYard(trimmed)) {
            return "karbysheva_22"
        }
        val clean = trimmed.lowercase(Locale.ROOT)
            .replace(Regex("[^a-zа-я0-9]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
        val slug = if (clean.length > 28) clean.take(28) else clean
        val hash = Math.abs(trimmed.hashCode()).toString(16).padStart(6, '0').take(6)
        return if (slug.isBlank()) "yard_$hash" else "yard_${slug}_$hash"
    }

    fun isMasterKarbyshevaYard(addressOrId: String): Boolean {
        val norm = addressOrId.trim().lowercase(Locale.ROOT)
        if (norm == "karbysheva_22") return true
        val hasKarbysheva = norm.contains("karbysheva") || norm.contains("карбышев")
        val has22 = norm.contains("22")
        val hasOtherBldg = norm.contains("24") || norm.contains("26") || norm.contains("28") ||
                           norm.contains("11") || norm.contains("15") || norm.contains("10")
        return hasKarbysheva && has22 && !hasOtherBldg
    }

    fun isKarbyshevaYard(address: String, id: String): Boolean {
        return isMasterKarbyshevaYard(address) || isMasterKarbyshevaYard(id)
    }

    fun generateTemplateData(address: String, templateType: YardTemplateType, baseKarbyshevaData: ParkingData? = null): ParkingData {
        return when (templateType) {
            YardTemplateType.COPY_KARBYSHEVA -> {
                if (baseKarbyshevaData != null && baseKarbyshevaData.spots.isNotEmpty()) {
                    // For any newly scaled yard, ensure all spots are clean (unoccupied) and blockers empty
                    val cleanSpots = baseKarbyshevaData.spots.map { s ->
                        s.copy(occupiedBy = null)
                    }
                    baseKarbyshevaData.copy(
                        spots = cleanSpots,
                        blockers = emptyList(),
                        landmarks = listOf(
                            ParkingLandmark(id = "home_main", text = address, x = 180f, y = 70f),
                            ParkingLandmark(id = "driveway", text = "Въезд во двор", x = 50f, y = 200f),
                            ParkingLandmark(id = "trash", text = "Контейнеры ТКО", x = 550f, y = 70f)
                        )
                    )
                } else {
                    generateStandard2Rows(address)
                }
            }
            YardTemplateType.STANDARD_2ROWS -> generateStandard2Rows(address)
            YardTemplateType.COMPACT -> generateCompactYard(address)
            YardTemplateType.EMPTY -> generateEmptyYard(address)
        }
    }

    private fun generateStandard2Rows(address: String): ParkingData {
        val spots = mutableListOf<ParkingSpot>()
        // Row 1 (top row, spots 1..10)
        for (i in 0 until 10) {
            val spotId = i + 1
            spots.add(
                ParkingSpot(
                    id = spotId,
                    x = 80f + i * 50f,
                    y = 130f,
                    angle = 0f
                )
            )
        }
        // Row 2 (bottom row, spots 11..20)
        for (i in 0 until 10) {
            val spotId = 11 + i
            spots.add(
                ParkingSpot(
                    id = spotId,
                    x = 80f + i * 50f,
                    y = 290f,
                    angle = 0f
                )
            )
        }

        val landmarks = listOf(
            ParkingLandmark(id = "house_1", text = address, x = 200f, y = 60f),
            ParkingLandmark(id = "driveway", text = "Въезд / Шлагбаум", x = 40f, y = 210f),
            ParkingLandmark(id = "walkway", text = "Пешеходная дорожка", x = 220f, y = 370f),
            ParkingLandmark(id = "trash", text = "ТКО", x = 600f, y = 60f)
        )

        return ParkingData(
            spots = spots,
            landmarks = landmarks,
            blockers = emptyList()
        )
    }

    private fun generateCompactYard(address: String): ParkingData {
        val spots = mutableListOf<ParkingSpot>()
        // 6 spots horizontally
        for (i in 0 until 6) {
            spots.add(
                ParkingSpot(
                    id = i + 1,
                    x = 100f + i * 52f,
                    y = 140f,
                    angle = 0f
                )
            )
        }
        // 6 spots angled (45 deg)
        for (i in 0 until 6) {
            spots.add(
                ParkingSpot(
                    id = 7 + i,
                    x = 100f + i * 54f,
                    y = 280f,
                    angle = 30f
                )
            )
        }

        val landmarks = listOf(
            ParkingLandmark(id = "house_compact", text = address, x = 160f, y = 70f),
            ParkingLandmark(id = "barrier", text = "Шлагбаум", x = 50f, y = 210f),
            ParkingLandmark(id = "greenery", text = "Газон", x = 440f, y = 140f)
        )

        return ParkingData(
            spots = spots,
            landmarks = landmarks,
            blockers = emptyList()
        )
    }

    private fun generateEmptyYard(address: String): ParkingData {
        return ParkingData(
            spots = emptyList(),
            landmarks = listOf(
                ParkingLandmark(id = "house_empty", text = address, x = 200f, y = 80f),
                ParkingLandmark(id = "driveway", text = "Въезд во двор", x = 60f, y = 200f)
            ),
            blockers = emptyList()
        )
    }
}
