package com.example.model

data class SurahInfo(
    val number: Int,
    val nameArabic: String,
    val revelationType: String, // مكية / مدنية
    val ayahCount: Int,
    val startPage: Int,
    val startJuz: Int
)

data class JuzInfo(
    val number: Int,
    val nameArabic: String,
    val startSurahName: String,
    val startPage: Int
)

data class QuranWardProgress(
    val currentPage: Int = 1,
    val dailyGoalPages: Int = 4,
    val pagesReadToday: Int = 0,
    val streakDays: Int = 1,
    val bookmarkPage: Int = 1,
    val bookmarkSurah: String = "الفاتحة",
    val completedKhatmahs: Int = 0
) {
    val totalPages: Int = 604
    val currentJuz: Int = ((currentPage - 1) / 20 + 1).coerceIn(1, 30)
    val progressFraction: Float = (currentPage.toFloat() / totalPages).coerceIn(0f, 1f)
    val progressPercent: Int = (progressFraction * 100).toInt()
    val remainingPages: Int = (totalPages - currentPage).coerceAtLeast(0)
    val estimatedDaysToKhatmah: Int = if (dailyGoalPages > 0) {
        (remainingPages.toDouble() / dailyGoalPages).toInt().coerceAtLeast(1)
    } else 30
    val isTodayGoalCompleted: Boolean = pagesReadToday >= dailyGoalPages
}

object QuranMetadata {
    val surahs: List<SurahInfo> = listOf(
        SurahInfo(1, "الفاتحة", "مكية", 7, 1, 1),
        SurahInfo(2, "البقرة", "مدنية", 286, 2, 1),
        SurahInfo(3, "آل عمران", "مدنية", 200, 50, 3),
        SurahInfo(4, "النساء", "مدنية", 176, 77, 4),
        SurahInfo(5, "المائدة", "مدنية", 120, 106, 6),
        SurahInfo(6, "الأنعام", "مكية", 165, 128, 7),
        SurahInfo(7, "الأعراف", "مكية", 206, 151, 8),
        SurahInfo(8, "الأنفال", "مدنية", 75, 177, 9),
        SurahInfo(9, "التوبة", "مدنية", 129, 187, 10),
        SurahInfo(10, "يونس", "مكية", 109, 208, 11),
        SurahInfo(11, "هود", "مكية", 123, 221, 11),
        SurahInfo(12, "يوسف", "مكية", 111, 235, 12),
        SurahInfo(13, "الرعد", "مدنية", 43, 249, 13),
        SurahInfo(14, "إبراهيم", "مكية", 52, 255, 13),
        SurahInfo(15, "الحجر", "مكية", 99, 262, 14),
        SurahInfo(16, "النحل", "مكية", 128, 267, 14),
        SurahInfo(17, "الإسراء", "مكية", 111, 282, 15),
        SurahInfo(18, "الكهف", "مكية", 110, 293, 15),
        SurahInfo(19, "مريم", "مكية", 98, 305, 16),
        SurahInfo(20, "طه", "مكية", 135, 312, 16),
        SurahInfo(21, "الأنبياء", "مكية", 112, 322, 17),
        SurahInfo(22, "الحج", "مدنية", 78, 332, 17),
        SurahInfo(23, "المؤمنون", "مكية", 118, 342, 18),
        SurahInfo(24, "النور", "مدنية", 64, 350, 18),
        SurahInfo(25, "الفرقان", "مكية", 77, 359, 18),
        SurahInfo(26, "الشعراء", "مكية", 227, 367, 19),
        SurahInfo(27, "النمل", "مكية", 93, 377, 19),
        SurahInfo(28, "القصص", "مكية", 88, 385, 20),
        SurahInfo(29, "العنكبوت", "مكية", 69, 396, 20),
        SurahInfo(30, "الروم", "مكية", 60, 404, 21),
        SurahInfo(31, "لقمان", "مكية", 34, 411, 21),
        SurahInfo(32, "السجدة", "مكية", 30, 415, 21),
        SurahInfo(33, "الأحزاب", "مدنية", 73, 418, 21),
        SurahInfo(34, "سبأ", "مكية", 54, 428, 22),
        SurahInfo(35, "فاطر", "مكية", 45, 434, 22),
        SurahInfo(36, "يس", "مكية", 83, 440, 22),
        SurahInfo(37, "الصافات", "مكية", 182, 446, 23),
        SurahInfo(38, "ص", "مكية", 88, 453, 23),
        SurahInfo(39, "الزمر", "مكية", 75, 458, 23),
        SurahInfo(40, "غافر", "مكية", 85, 467, 24),
        SurahInfo(41, "فصلت", "مكية", 54, 477, 24),
        SurahInfo(42, "الشورى", "مكية", 53, 483, 25),
        SurahInfo(43, "الزخرف", "مكية", 89, 489, 25),
        SurahInfo(44, "الدخان", "مكية", 59, 496, 25),
        SurahInfo(45, "الجاثية", "مكية", 37, 499, 25),
        SurahInfo(46, "الأحقاف", "مكية", 35, 502, 26),
        SurahInfo(47, "محمد", "مدنية", 38, 507, 26),
        SurahInfo(48, "الفتح", "مدنية", 29, 511, 26),
        SurahInfo(49, "الحجرات", "مدنية", 18, 515, 26),
        SurahInfo(50, "ق", "مكية", 45, 518, 26),
        SurahInfo(51, "الذاريات", "مكية", 60, 520, 26),
        SurahInfo(52, "الطور", "مكية", 49, 523, 27),
        SurahInfo(53, "النجم", "مكية", 62, 526, 27),
        SurahInfo(54, "القمر", "مكية", 55, 528, 27),
        SurahInfo(55, "الرحمن", "مدنية", 78, 531, 27),
        SurahInfo(56, "الواقعة", "مكية", 96, 534, 27),
        SurahInfo(57, "الحديد", "مدنية", 29, 537, 27),
        SurahInfo(58, "المجادلة", "مدنية", 22, 542, 28),
        SurahInfo(59, "الحشر", "مدنية", 24, 545, 28),
        SurahInfo(60, "الممتحنة", "مدنية", 13, 549, 28),
        SurahInfo(61, "الصف", "مدنية", 14, 551, 28),
        SurahInfo(62, "الجمعة", "مدنية", 11, 553, 28),
        SurahInfo(63, "المنافقون", "مدنية", 11, 554, 28),
        SurahInfo(64, "التغابن", "مدنية", 18, 556, 28),
        SurahInfo(65, "الطلاق", "مدنية", 12, 558, 28),
        SurahInfo(66, "التحريم", "مدنية", 12, 560, 28),
        SurahInfo(67, "الملك", "مكية", 30, 562, 29),
        SurahInfo(68, "القلم", "مكية", 52, 564, 29),
        SurahInfo(69, "الحاقة", "مكية", 52, 566, 29),
        SurahInfo(70, "المعارج", "مكية", 44, 568, 29),
        SurahInfo(71, "نوح", "مكية", 28, 570, 29),
        SurahInfo(72, "الجن", "مكية", 28, 572, 29),
        SurahInfo(73, "المزمل", "مكية", 20, 574, 29),
        SurahInfo(74, "المدثر", "مكية", 56, 575, 29),
        SurahInfo(75, "القيامة", "مكية", 40, 577, 29),
        SurahInfo(76, "الإنسان", "مدنية", 31, 578, 29),
        SurahInfo(77, "المرسلات", "مكية", 50, 580, 29),
        SurahInfo(78, "النبأ", "مكية", 40, 582, 30),
        SurahInfo(79, "النازعات", "مكية", 46, 583, 30),
        SurahInfo(80, "عبس", "مكية", 42, 585, 30),
        SurahInfo(81, "التكوير", "مكية", 29, 586, 30),
        SurahInfo(82, "الانفطار", "مكية", 19, 587, 30),
        SurahInfo(83, "المطففين", "مكية", 36, 587, 30),
        SurahInfo(84, "الانشقاق", "مكية", 25, 589, 30),
        SurahInfo(85, "البروج", "مكية", 22, 590, 30),
        SurahInfo(86, "الطارق", "مكية", 17, 591, 30),
        SurahInfo(87, "الأعلى", "مكية", 19, 591, 30),
        SurahInfo(88, "الغاشية", "مكية", 26, 592, 30),
        SurahInfo(89, "الفجر", "مكية", 30, 593, 30),
        SurahInfo(90, "البلد", "مكية", 20, 594, 30),
        SurahInfo(91, "الشمس", "مكية", 15, 595, 30),
        SurahInfo(92, "الليل", "مكية", 21, 595, 30),
        SurahInfo(93, "الضحى", "مكية", 11, 596, 30),
        SurahInfo(94, "الشرح", "مكية", 8, 596, 30),
        SurahInfo(95, "التين", "مكية", 8, 597, 30),
        SurahInfo(96, "العلق", "مكية", 19, 597, 30),
        SurahInfo(97, "القدر", "مكية", 5, 598, 30),
        SurahInfo(98, "البينة", "مدنية", 8, 598, 30),
        SurahInfo(99, "الزلزلة", "مدنية", 8, 599, 30),
        SurahInfo(100, "العاديات", "مكية", 11, 599, 30),
        SurahInfo(101, "القارعة", "مكية", 11, 600, 30),
        SurahInfo(102, "التكاثر", "مكية", 8, 600, 30),
        SurahInfo(103, "العصر", "مكية", 3, 601, 30),
        SurahInfo(104, "الهمزة", "مكية", 9, 601, 30),
        SurahInfo(105, "الفيل", "مكية", 5, 601, 30),
        SurahInfo(106, "قريش", "مكية", 4, 602, 30),
        SurahInfo(107, "الماعون", "مكية", 7, 602, 30),
        SurahInfo(108, "الكوثر", "مكية", 3, 602, 30),
        SurahInfo(109, "الكافرون", "مكية", 6, 603, 30),
        SurahInfo(110, "النصر", "مدنية", 3, 603, 30),
        SurahInfo(111, "المسد", "مكية", 5, 603, 30),
        SurahInfo(112, "الإخلاص", "مكية", 4, 604, 30),
        SurahInfo(113, "الفلق", "مكية", 5, 604, 30),
        SurahInfo(114, "الناس", "مكية", 6, 604, 30)
    )

    fun getSurahForPage(page: Int): SurahInfo {
        return surahs.lastOrNull { it.startPage <= page } ?: surahs.first()
    }
}
