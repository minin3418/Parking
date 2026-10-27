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

    suspend fun saveToGitHub(
        token: String,
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
                val getRequest = okhttp3.Request.Builder()
                    .url("https://api.github.com/repos/minin3418/Parking/contents/database.json")
                    .header("Authorization", "Bearer $cleanToken")
                    .header("Accept", "application/vnd.github.v3+json")
                    .get()
                    .build()

                var currentSha: String? = null
                client.newCall(getRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val match = Regex("\"sha\"\\s*:\\s*\"([^\"]+)\"").find(body)
                        currentSha = match?.groupValues?.get(1)
                    }
                }

                val jsonString = moshi.adapter(ParkingData::class.java).indent("  ").toJson(data)
                val base64Content = android.util.Base64.encodeToString(
                    jsonString.toByteArray(Charsets.UTF_8),
                    android.util.Base64.NO_WRAP
                )

                val putJson = buildString {
                    append("{")
                    append("\"message\": \"").append(commitMessage).append("\",")
                    append("\"content\": \"").append(base64Content).append("\"")
                    if (!currentSha.isNullOrBlank()) {
                        append(",\"sha\": \"").append(currentSha).append("\"")
                    }
                    append("}")
                }

                val putBody = putJson.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

                val putRequest = okhttp3.Request.Builder()
                    .url("https://api.github.com/repos/minin3418/Parking/contents/database.json")
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
