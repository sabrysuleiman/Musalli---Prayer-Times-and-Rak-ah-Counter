package com.example.model

enum class CountingMode(val titleArabic: String, val descArabic: String) {
    SAJDAH_ASSIST(
        "حساب بالسجدات (الأكثر دقة لمن ينسى)",
        "المس الشاشة عند كل سجدة (سجدة 1، ثم سجدة 2) لتكتمل الركعة تلقائياً مع تذكير بالتشهد"
    ),
    RAKAH_DIRECT(
        "حساب مباشر بالركعات",
        "المس الشاشة لمسة واحدة عند إتمام كل ركعة"
    )
}

data class RakahStepSnapshot(
    val currentRakah: Int,
    val currentSajdah: Int,
    val note: String
)

data class RakahSessionState(
    val selectedPrayer: PrayerType = PrayerType.DHUHR,
    val targetRakahs: Int = 4,
    val currentRakah: Int = 1,
    val currentSajdah: Int = 0, // 0: standing/rukoo, 1: first sajdah, 2: second sajdah
    val isSessionActive: Boolean = false,
    val isSessionCompleted: Boolean = false,
    val countingMode: CountingMode = CountingMode.SAJDAH_ASSIST,
    val guidanceMessage: String = "انقر 'ابدأ الصلاة' للبدء",
    val promptTashahhud: Boolean = false,
    val historyStack: List<RakahStepSnapshot> = emptyList()
)
