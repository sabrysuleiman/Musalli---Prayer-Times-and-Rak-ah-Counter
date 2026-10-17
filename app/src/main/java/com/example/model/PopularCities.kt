package com.example.model

object PopularCities {
    val list = listOf(
        CityLocation("مكة المكرمة", "المملكة العربية السعودية", 21.4225, 39.8262, "Asia/Riyadh"),
        CityLocation("المدينة المنورة", "المملكة العربية السعودية", 24.4672, 39.6111, "Asia/Riyadh"),
        CityLocation("الرياض", "المملكة العربية السعودية", 24.7136, 46.6753, "Asia/Riyadh"),
        CityLocation("جدة", "المملكة العربية السعودية", 21.5433, 39.1728, "Asia/Riyadh"),
        CityLocation("القاهرة", "مصر", 30.0444, 31.2357, "Africa/Cairo"),
        CityLocation("الإسكندرية", "مصر", 31.2001, 29.9187, "Africa/Cairo"),
        CityLocation("دبي", "الإمارات العربية المتحدة", 25.2048, 55.2708, "Asia/Dubai"),
        CityLocation("أبوظبي", "الإمارات العربية المتحدة", 24.4539, 54.3773, "Asia/Dubai"),
        CityLocation("الدوحة", "قطر", 25.2854, 51.5310, "Asia/Qatar"),
        CityLocation("الكويت", "الكويت", 29.3759, 47.9774, "Asia/Kuwait"),
        CityLocation("المنامة", "البحرين", 26.2285, 50.5860, "Asia/Bahrain"),
        CityLocation("مسقط", "عُمان", 23.5880, 58.3829, "Asia/Muscat"),
        CityLocation("عمان", "الأردن", 31.9454, 35.9284, "Asia/Amman"),
        CityLocation("القدس الشريف", "فلسطين", 31.7683, 35.2137, "Asia/Jerusalem"),
        CityLocation("دمشق", "سوريا", 33.5138, 36.2765, "Asia/Damascus"),
        CityLocation("بيروت", "لبنان", 33.8938, 35.5018, "Asia/Beirut"),
        CityLocation("بغداد", "العراق", 33.3152, 44.3661, "Asia/Baghdad"),
        CityLocation("صنعاء", "اليمن", 15.3694, 44.1910, "Asia/Aden"),
        CityLocation("طرابلس", "ليبيا", 32.8872, 13.1913, "Africa/Tripoli"),
        CityLocation("تونس", "تونس", 36.8065, 10.1815, "Africa/Tunis"),
        CityLocation("الجزائر", "الجزائر", 36.7538, 3.0588, "Africa/Algiers"),
        CityLocation("الرباط", "المغرب", 34.0209, -6.8416, "Africa/Casablanca"),
        CityLocation("الدار البيضاء", "المغرب", 33.5731, -7.5898, "Africa/Casablanca"),
        CityLocation("الخرطوم", "السودان", 15.5007, 32.5599, "Africa/Khartoum"),
        CityLocation("إسطنبول", "تركيا", 41.0082, 28.9784, "Europe/Istanbul"),
        CityLocation("لندن", "المملكة المتحدة", 51.5074, -0.1278, "Europe/London"),
        CityLocation("باريس", "فرنسا", 48.8566, 2.3522, "Europe/Paris"),
        CityLocation("نيويورك", "الولايات المتحدة", 40.7128, -74.0060, "America/New_York")
    )

    val defaultCity = list[0] // Makkah
}
