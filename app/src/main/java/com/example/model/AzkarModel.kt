package com.example.model

enum class AzkarCategory(val id: String, val titleArabic: String, val iconName: String) {
    MORNING("morning", "أذكار الصباح", "wb_sunny"),
    EVENING("evening", "أذكار المساء", "nights_stay"),
    POST_PRAYER("post_prayer", "أذكار بعد الصلاة", "mosque"),
    SLEEP("sleep", "أذكار النوم", "bedtime"),
    GENERAL("general", "تسبيح وأدعية جامعة", "auto_awesome")
}

data class DhikrItem(
    val id: String,
    val category: AzkarCategory,
    val textArabic: String,
    val virtueArabic: String,
    val targetCount: Int
)

object AzkarRepository {
    val items = listOf(
        // أذكار الصباح
        DhikrItem(
            id = "m_1",
            category = AzkarCategory.MORNING,
            textArabic = "أَصْبَحْنَا وَأَصْبَحَ المُلْكُ لِلَّهِ، وَالحَمْدُ لِلَّهِ، لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ المُلْكُ وَلَهُ الحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ.",
            virtueArabic = "رواه مسلم • من قاله حين يصبح أُعطي خير ما في هذا اليوم",
            targetCount = 1
        ),
        DhikrItem(
            id = "m_2",
            category = AzkarCategory.MORNING,
            textArabic = "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ بِذَنْبِي فَاغْفِرْ لِي فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ.",
            virtueArabic = "سيد الاستغفار • من قالها موقناً بها حين يصبح فمات دخل الجنة",
            targetCount = 1
        ),
        DhikrItem(
            id = "m_3",
            category = AzkarCategory.MORNING,
            textArabic = "بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ العَلِيمُ.",
            virtueArabic = "لم يضره شيء حتى يمسي",
            targetCount = 3
        ),
        DhikrItem(
            id = "m_4",
            category = AzkarCategory.MORNING,
            textArabic = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ: عَدَدَ خَلْقِهِ، وَرِضَا نَفْسِهِ، وَزِنَةَ عَرْشِهِ، وَمِدَادَ كَلِمَاتِهِ.",
            virtueArabic = "تعدل ساعات طويلة من الذكر والتسبيح",
            targetCount = 3
        ),
        DhikrItem(
            id = "m_5",
            category = AzkarCategory.MORNING,
            textArabic = "يَا حَيُّ يَا قَيُّومُ بِرَحْمَتِكَ أَسْتَغِيثُ، أَصْلِحْ لِي شَأْنِي كُلَّهُ، وَلَا تَكِلْنِي إِلَى نَفْسِي طَرْفَةَ عَيْنٍ.",
            virtueArabic = "دعاء النجاة وتفويض الأمر لله تعالى",
            targetCount = 1
        ),
        DhikrItem(
            id = "m_6",
            category = AzkarCategory.MORNING,
            textArabic = "قُلْ هُوَ اللَّهُ أَحَدٌ، وَقُلْ أَعُوذُ بِرَبِّ الفَلَقِ، وَقُلْ أَعُوذُ بِرَبِّ النَّاسِ.",
            virtueArabic = "تكفيك من كل شيء",
            targetCount = 3
        ),

        // أذكار المساء
        DhikrItem(
            id = "e_1",
            category = AzkarCategory.EVENING,
            textArabic = "أَمْسَيْنَا وَأَمْسَى المُلْكُ لِلَّهِ، وَالحَمْدُ لِلَّهِ، لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ المُلْكُ وَلَهُ الحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ.",
            virtueArabic = "رواه مسلم • حماية وبركة المساء",
            targetCount = 1
        ),
        DhikrItem(
            id = "e_2",
            category = AzkarCategory.EVENING,
            textArabic = "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ.",
            virtueArabic = "لم يضره شيء حتى يصبح",
            targetCount = 3
        ),
        DhikrItem(
            id = "e_3",
            category = AzkarCategory.EVENING,
            textArabic = "اللَّهُمَّ بِكَ أَمْسَيْنَا، وَبِكَ أَصْبَحْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ المَصِيرُ.",
            virtueArabic = "من أذكار المساء النبوية",
            targetCount = 1
        ),
        DhikrItem(
            id = "e_4",
            category = AzkarCategory.EVENING,
            textArabic = "قُلْ هُوَ اللَّهُ أَحَدٌ، وَالمُعَوِّذَتَيْنِ.",
            virtueArabic = "تكفيك من كل سوء وشر",
            targetCount = 3
        ),

        // أذكار بعد الصلاة
        DhikrItem(
            id = "p_1",
            category = AzkarCategory.POST_PRAYER,
            textArabic = "أَسْتَغْفِرُ اللهَ، أَسْتَغْفِرُ اللهَ، أَسْتَغْفِرُ اللهَ.",
            virtueArabic = "يفتتح بها دبر كل صلاة مكتوبة",
            targetCount = 3
        ),
        DhikrItem(
            id = "p_2",
            category = AzkarCategory.POST_PRAYER,
            textArabic = "اللَّهُمَّ أَنْتَ السَّلَامُ وَمِنْكَ السَّلَامُ، تَبَارَكْتَ يَا ذَا الجَلَالِ وَالإِكْرَامِ.",
            virtueArabic = "سنة المصطفى ﷺ عقب الصلاة",
            targetCount = 1
        ),
        DhikrItem(
            id = "p_3",
            category = AzkarCategory.POST_PRAYER,
            textArabic = "سُبْحَانَ اللهِ.",
            virtueArabic = "تسبيح دبر كل صلاة",
            targetCount = 33
        ),
        DhikrItem(
            id = "p_4",
            category = AzkarCategory.POST_PRAYER,
            textArabic = "الحَمْدُ لِلَّهِ.",
            virtueArabic = "تحميد دبر كل صلاة",
            targetCount = 33
        ),
        DhikrItem(
            id = "p_5",
            category = AzkarCategory.POST_PRAYER,
            textArabic = "اللهُ أَكْبَرُ.",
            virtueArabic = "تكبير دبر كل صلاة",
            targetCount = 33
        ),
        DhikrItem(
            id = "p_6",
            category = AzkarCategory.POST_PRAYER,
            textArabic = "لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ المُلْكُ وَلَهُ الحَمْدُ، وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ.",
            virtueArabic = "تمام المائة • غُفرت خطاياه وإن كانت مثل زبد البحر",
            targetCount = 1
        ),
        DhikrItem(
            id = "p_7",
            category = AzkarCategory.POST_PRAYER,
            textArabic = "آية الكرسي: «اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ...»",
            virtueArabic = "من قرأها دبر كل صلاة لم يمنعه من دخول الجنة إلا أن يموت",
            targetCount = 1
        ),

        // أذكار النوم
        DhikrItem(
            id = "s_1",
            category = AzkarCategory.SLEEP,
            textArabic = "بِاسْمِكَ رَبِّي وَضَعْتُ جَنْبِي وَبِكَ أَرْفَعُهُ، إِنْ أَمْسَكْتَ نَفْسِي فَارْحَمْهَا، وَإِنْ أَرْسَلْتَهَا فَاحْفَظْهَا بِمَا تَحْفَظُ بِهِ عِبَادَكَ الصَّالِحِينَ.",
            virtueArabic = "دعاء النوم المحفوظ",
            targetCount = 1
        ),
        DhikrItem(
            id = "s_2",
            category = AzkarCategory.SLEEP,
            textArabic = "اللَّهُمَّ قِنِي عَذَابَكَ يَوْمَ تَبْعَثُ عِبَادَكَ.",
            virtueArabic = "كان ﷺ يضع يده تحت خده الأيمن ويقولها ثلاثاً",
            targetCount = 3
        ),

        // تسبيح وأدعية جامعة
        DhikrItem(
            id = "g_1",
            category = AzkarCategory.GENERAL,
            textArabic = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، سُبْحَانَ اللَّهِ العَظِيمِ.",
            virtueArabic = "كلمتان خفيفتان على اللسان، ثقيلتان في الميزان، حبيبتان إلى الرحمن",
            targetCount = 100
        ),
        DhikrItem(
            id = "g_2",
            category = AzkarCategory.GENERAL,
            textArabic = "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ العَلِيِّ العَظِيمِ.",
            virtueArabic = "كنز من كنوز الجنة وباب من أبواب الفرج",
            targetCount = 33
        ),
        DhikrItem(
            id = "g_3",
            category = AzkarCategory.GENERAL,
            textArabic = "اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدٍ وَعَلَى آلِهِ وَصَحْبِهِ أَجْمَعِينَ.",
            virtueArabic = "من صلى عليّ صلاة صلى الله عليه بها عشراً",
            targetCount = 10
        ),
        DhikrItem(
            id = "g_4",
            category = AzkarCategory.GENERAL,
            textArabic = "أَسْتَغْفِرُ اللَّهَ العَظِيمَ الَّذِي لَا إِلَهَ إِلَّا هُوَ الحَيَّ القَيُّومَ وَأَتُوبُ إِلَيْهِ.",
            virtueArabic = "غُفرت ذنوبه وإن كان فر من الزحف",
            targetCount = 33
        )
    )
}
