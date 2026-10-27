package com.example.data.network

import com.example.data.model.Occupant
import com.example.data.model.ParkingBlocker
import com.example.data.model.ParkingData
import com.example.data.model.ParkingLandmark
import com.example.data.model.ParkingSpot
import com.example.data.model.SharedYardEntry
import com.example.data.model.SurveyItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class CloudException(val code: Int, body: String) : Exception("Firebase ($code): $body")

/**
 * Shared yard data in Firebase Realtime Database, used through its REST API (no SDK):
 *   index/<yardId>                  - shared address list: {address, createdAt}
 *   yards/<yardId>/schema           - {spots: [...], landmarks: [...]}
 *   yards/<yardId>/occupancy/s<id>  - who parked on spot <id>
 *   yards/<yardId>/blockers/<id>    - cars in the 2nd row
 *   yards/<yardId>/surveys/v<id>    - survey votes
 * Keys are prefixed ("s5", "v1730...") so Firebase never turns these maps into arrays.
 * The web app (web/index.html) uses the same layout; access rules live in the Firebase console.
 */
object ParkingCloud {
    const val DB_URL = "https://dvor-parking-default-rtdb.asia-southeast1.firebasedatabase.app"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    // Event stream: Firebase sends a keep-alive every ~30 s, so a longer silence means a dead connection
    private val streamClient = client.newBuilder()
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val moshi = ParkingApiClient.moshi
    private val spotAdapter = moshi.adapter(ParkingSpot::class.java)
    private val landmarkAdapter = moshi.adapter(ParkingLandmark::class.java)
    private val occupantAdapter = moshi.adapter(Occupant::class.java)
    private val blockerAdapter = moshi.adapter(ParkingBlocker::class.java)
    private val surveyAdapter = moshi.adapter(SurveyItem::class.java)

    private fun url(vararg segments: String): HttpUrl {
        val builder = DB_URL.toHttpUrl().newBuilder()
        segments.forEachIndexed { i, segment ->
            builder.addPathSegment(if (i == segments.lastIndex) "$segment.json" else segment)
        }
        return builder.build()
    }

    private suspend fun request(method: String, url: HttpUrl, body: String? = null): String =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .method(method, body?.toRequestBody(jsonType))
                .build()
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw CloudException(response.code, text)
                text
            }
        }

    /** Children of a JSON node that Firebase may return either as an object or as an array. */
    private fun children(node: Any?): List<Pair<String, Any>> = when (node) {
        is JSONObject -> node.keys().asSequence().mapNotNull { key ->
            node.opt(key)?.takeIf { it != JSONObject.NULL }?.let { key to it }
        }.toList()
        is JSONArray -> (0 until node.length()).mapNotNull { i ->
            node.opt(i)?.takeIf { it != JSONObject.NULL }?.let { i.toString() to it }
        }
        else -> emptyList()
    }

    /** Full yard (schema + occupancy + 2nd row + votes), or null when the yard has no schema in the cloud yet. */
    suspend fun loadYard(yardId: String): ParkingData? {
        val text = request("GET", url("yards", yardId))
        if (text.isBlank() || text == "null") return null
        val root = JSONObject(text)
        val schema = root.optJSONObject("schema") ?: return null

        val occupants = mutableMapOf<Int, Occupant>()
        children(root.opt("occupancy")).forEach { (key, value) ->
            val spotId = key.removePrefix("s").toIntOrNull() ?: return@forEach
            occupantAdapter.fromJson(value.toString())?.let { occupants[spotId] = it }
        }
        val spots = children(schema.opt("spots")).mapNotNull { (_, value) ->
            spotAdapter.fromJson(value.toString())
        }.map { it.copy(occupiedBy = occupants[it.id]) }
        val landmarks = children(schema.opt("landmarks")).mapNotNull { (_, value) ->
            landmarkAdapter.fromJson(value.toString())
        }
        val blockers = children(root.opt("blockers")).mapNotNull { (key, value) ->
            blockerAdapter.fromJson(value.toString())?.let { if (it.id.isBlank()) it.copy(id = key) else it }
        }.sortedBy { it.parkedAt ?: "" }
        val surveys = children(root.opt("surveys")).mapNotNull { (_, value) ->
            surveyAdapter.fromJson(value.toString())
        }.sortedBy { it.id }

        return ParkingData(spots = spots, landmarks = landmarks, blockers = blockers, surveys = surveys)
    }

    suspend fun loadIndex(): List<SharedYardEntry> {
        val text = request("GET", url("index"))
        if (text.isBlank() || text == "null") return emptyList()
        val root = JSONObject(text)
        return children(root).mapNotNull { (id, value) ->
            val entry = value as? JSONObject ?: return@mapNotNull null
            val address = entry.optString("address").ifBlank { return@mapNotNull null }
            SharedYardEntry(id = id, address = address, createdAt = entry.optLong("createdAt").takeIf { it > 0 })
        }.sortedBy { it.createdAt ?: 0L }
    }

    suspend fun setOccupant(yardId: String, spotId: Int, occupant: Occupant?) {
        val target = url("yards", yardId, "occupancy", "s$spotId")
        if (occupant == null) request("DELETE", target) else request("PUT", target, occupantAdapter.toJson(occupant))
    }

    suspend fun addBlocker(yardId: String, blocker: ParkingBlocker) {
        request("PUT", url("yards", yardId, "blockers", blocker.id), blockerAdapter.toJson(blocker))
    }

    suspend fun removeBlocker(yardId: String, blockerId: String) {
        request("DELETE", url("yards", yardId, "blockers", blockerId))
    }

    suspend fun addSurvey(yardId: String, item: SurveyItem) {
        request("PUT", url("yards", yardId, "surveys", "v${item.id}"), surveyAdapter.toJson(item))
    }

    /** Spot positions and landmarks only; who is parked where is stored separately. */
    suspend fun saveSchema(yardId: String, data: ParkingData) {
        val schema = JSONObject()
            .put("spots", JSONArray(data.spots.map { JSONObject(spotAdapter.toJson(it.copy(occupiedBy = null))) }))
            .put("landmarks", JSONArray(data.landmarks.map { JSONObject(landmarkAdapter.toJson(it)) }))
        request("PUT", url("yards", yardId, "schema"), schema.toString())
    }

    /**
     * Publishes a yard created on this device: its starter schema (unless someone already
     * published one) and its entry in the shared address list (entries are write-once).
     */
    suspend fun publishYard(entry: SharedYardEntry, data: ParkingData) {
        val existing = request("GET", url("yards", entry.id, "schema"))
        if (existing.isBlank() || existing == "null") saveSchema(entry.id, data)
        val body = JSONObject().put("address", entry.address).put("createdAt", entry.createdAt ?: System.currentTimeMillis())
        try {
            request("PUT", url("index", entry.id), body.toString())
        } catch (e: CloudException) {
            // 401: the entry already exists - rules make index entries write-once
            if (e.code != 401) throw e
        }
    }

    /**
     * Emits whenever anything under yards/<yardId> changes, using Firebase's server-sent events.
     * The first event arrives right after connecting. Reconnects until the collector is cancelled.
     */
    fun changes(yardId: String): Flow<Unit> = callbackFlow {
        var call: Call? = null
        val reader = launch(Dispatchers.IO) {
            var backoffMs = 1_000L
            while (isActive) {
                try {
                    val request = Request.Builder()
                        .url(url("yards", yardId))
                        .header("Accept", "text/event-stream")
                        .build()
                    val current = streamClient.newCall(request)
                    call = current
                    current.execute().use { response ->
                        if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                        val source = response.body?.source() ?: throw IOException("Empty stream")
                        backoffMs = 1_000L
                        while (isActive) {
                            val line = source.readUtf8Line() ?: break
                            if (!line.startsWith("event:")) continue
                            when (line.substringAfter(':').trim()) {
                                "put", "patch" -> trySend(Unit)
                                "cancel", "auth_revoked" -> break
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Network drop or app going offline: retry below
                }
                if (isActive) {
                    delay(backoffMs)
                    backoffMs = (backoffMs * 2).coerceAtMost(30_000L)
                }
            }
        }
        awaitClose {
            call?.cancel()
            reader.cancel()
        }
    }
}
