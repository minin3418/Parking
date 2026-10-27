package com.example.data.network

import com.example.data.model.AdminLoginRequest
import com.example.data.model.AdminPinRequest
import com.example.data.model.GenericResponse
import com.example.data.model.ParkingData
import com.example.data.model.Row1LeaveRequest
import com.example.data.model.Row1ParkRequest
import com.example.data.model.Row2LeaveRequest
import com.example.data.model.Row2ParkRequest
import com.example.data.model.SaveMapConfigRequest
import com.example.data.model.SurveyItem
import com.example.data.model.SurveyRequest
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

import retrofit2.http.Url

interface ParkingApiService {

    @GET("database.json")
    suspend fun getMasterDatabase(): ParkingData

    @GET
    suspend fun getYardFromUrl(@Url url: String): ParkingData

    @GET("api/parking")
    suspend fun getParking(): ParkingData

    @POST("api/park")
    suspend fun parkRow1(@Body request: Row1ParkRequest): GenericResponse

    @POST("api/leave")
    suspend fun leaveRow1(@Body request: Row1LeaveRequest): GenericResponse

    @POST("api/blocker")
    suspend fun parkRow2(@Body request: Row2ParkRequest): GenericResponse

    @POST("api/blocker/leave")
    suspend fun leaveRow2(@Body request: Row2LeaveRequest): GenericResponse

    @POST("api/survey/vote")
    suspend fun submitSurvey(@Body request: SurveyRequest): GenericResponse

    @POST("api/admin/verify")
    suspend fun adminLogin(@Body request: AdminLoginRequest): GenericResponse

    @POST("api/admin/surveys")
    suspend fun getAdminSurveys(@Body request: AdminPinRequest): List<SurveyItem>

    @POST("api/admin/map")
    suspend fun saveMapConfig(@Body request: SaveMapConfigRequest): GenericResponse
}

object ParkingApiClient {
    const val GITHUB_REPO_URL = "https://github.com/minin3418/Parking"
    const val GITHUB_RAW_BASE_URL = "https://raw.githubusercontent.com/minin3418/Parking/main/"

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private var currentBaseUrl = GITHUB_RAW_BASE_URL
    private var retrofit = buildRetrofit(currentBaseUrl)
    var service: ParkingApiService = retrofit.create(ParkingApiService::class.java)
        private set

    private fun buildRetrofit(baseUrl: String): Retrofit {
        val safeBase = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(safeBase)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    fun updateBaseUrl(newUrl: String) {
        val url = if (newUrl.isNotBlank()) newUrl.trim() else GITHUB_RAW_BASE_URL
        if (url != currentBaseUrl) {
            currentBaseUrl = url
            retrofit = buildRetrofit(currentBaseUrl)
            service = retrofit.create(ParkingApiService::class.java)
        }
    }

    // Each yard has its own file in the repo, so saving one yard never overwrites another
    private fun yardContentsUrl(yardId: String): okhttp3.HttpUrl =
        okhttp3.HttpUrl.Builder()
            .scheme("https")
            .host("api.github.com")
            .addPathSegments("repos/minin3418/Parking/contents/yards")
            .addPathSegment("$yardId.json")
            .build()

    /**
     * Loads yard schema via GitHub contents API (not raw.githubusercontent.com, which is CDN-cached ~5 min).
     * Returns null when the yard has no file in the repository yet.
     */
    suspend fun loadYardFromGitHub(yardId: String): ParkingData? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val url = yardContentsUrl(yardId).newBuilder()
                .addQueryParameter("ref", "main")
                .addQueryParameter("t", System.currentTimeMillis().toString())
                .build()
            val request = okhttp3.Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.raw+json")
                .header("Cache-Control", "no-cache")
                .get()
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                if (response.code == 404) return@use null
                if (!response.isSuccessful) throw Exception("GitHub API (${response.code})")
                val body = response.body?.string().orEmpty()
                moshi.adapter(ParkingData::class.java).fromJson(body)
            }
        }
    }

    suspend fun saveToGitHub(
        token: String,
        yardId: String,
        data: ParkingData,
        commitMessage: String = "Обновление схемы двора из приложения"
    ): Result<String> {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val cleanToken = token.trim()
                if (cleanToken.isBlank()) {
                    return@withContext Result.failure(Exception("GitHub токен не указан"))
                }

                val client = okHttpClient
                val fileUrl = yardContentsUrl(yardId)
                val getRequest = okhttp3.Request.Builder()
                    .url(fileUrl.newBuilder().addQueryParameter("ref", "main").build())
                    .header("Authorization", "Bearer $cleanToken")
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("Cache-Control", "no-cache")
                    .get()
                    .build()

                var currentSha: String? = null
                client.newCall(getRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        currentSha = org.json.JSONObject(body).optString("sha").ifBlank { null }
                    }
                }

                val jsonString = moshi.adapter(ParkingData::class.java).indent("  ").toJson(data)
                val base64Content = android.util.Base64.encodeToString(
                    jsonString.toByteArray(Charsets.UTF_8),
                    android.util.Base64.NO_WRAP
                )

                val putJson = org.json.JSONObject().apply {
                    put("message", commitMessage)
                    put("content", base64Content)
                    put("branch", "main")
                    if (!currentSha.isNullOrBlank()) put("sha", currentSha)
                }.toString()

                val putBody = putJson.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

                val putRequest = okhttp3.Request.Builder()
                    .url(fileUrl)
                    .header("Authorization", "Bearer $cleanToken")
                    .header("Accept", "application/vnd.github.v3+json")
                    .put(putBody)
                    .build()

                client.newCall(putRequest).execute().use { putResponse ->
                    if (putResponse.isSuccessful) {
                        Result.success("Схема успешно сохранена на GitHub!")
                    } else {
                        val err = putResponse.body?.string() ?: "HTTP ${putResponse.code}"
                        Result.failure(Exception("GitHub API (${putResponse.code}): $err"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
