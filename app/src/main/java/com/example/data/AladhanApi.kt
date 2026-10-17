package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class AladhanResponse(
    @field:Json(name = "code") val code: Int?,
    @field:Json(name = "status") val status: String?,
    @field:Json(name = "data") val data: AladhanData?
)

@JsonClass(generateAdapter = true)
data class AladhanData(
    @field:Json(name = "timings") val timings: AladhanTimings?,
    @field:Json(name = "date") val date: AladhanDateMeta?
)

@JsonClass(generateAdapter = true)
data class AladhanTimings(
    @field:Json(name = "Fajr") val fajr: String?,
    @field:Json(name = "Sunrise") val sunrise: String?,
    @field:Json(name = "Dhuhr") val dhuhr: String?,
    @field:Json(name = "Asr") val asr: String?,
    @field:Json(name = "Sunset") val sunset: String?,
    @field:Json(name = "Maghrib") val maghrib: String?,
    @field:Json(name = "Isha") val isha: String?,
    @field:Json(name = "Imsak") val imsak: String?,
    @field:Json(name = "Midnight") val midnight: String?
)

@JsonClass(generateAdapter = true)
data class AladhanDateMeta(
    @field:Json(name = "readable") val readable: String?,
    @field:Json(name = "hijri") val hijri: AladhanHijriDate?
)

@JsonClass(generateAdapter = true)
data class AladhanHijriDate(
    @field:Json(name = "date") val date: String?,
    @field:Json(name = "day") val day: String?,
    @field:Json(name = "year") val year: String?,
    @field:Json(name = "month") val month: AladhanHijriMonth?
)

@JsonClass(generateAdapter = true)
data class AladhanHijriMonth(
    @field:Json(name = "number") val number: Int?,
    @field:Json(name = "en") val en: String?,
    @field:Json(name = "ar") val ar: String?
)

interface AladhanApiService {
    @GET("v1/timings/{timestamp}")
    suspend fun getTimings(
        @Path("timestamp") timestamp: Long,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int = 4 // 4 = Umm Al-Qura, 5 = Egyptian, 3 = MWL
    ): AladhanResponse

    companion object {
        private const val BASE_URL = "https://api.aladhan.com/"

        fun create(): AladhanApiService {
            val client = OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()

            return retrofit.create(AladhanApiService::class.java)
        }
    }
}
