package me.ash.reader.ui.page.home.feeds.photocard

import java.util.Calendar
import java.util.Date

object PhotocardDateHelper {
    private val BENGALI_DAYS = arrayOf(
        "রবিবার", "সোমবার", "মঙ্গলবার", "বুধবার", "বৃহস্পতিবার", "শুক্রবার", "শনিবার"
    )
    private val ENGLISH_DAYS = arrayOf(
        "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    )

    private val BENGALI_MONTHS = arrayOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )
    private val ENGLISH_MONTHS = arrayOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    fun toBengaliNumber(numberStr: String): String {
        return numberStr.map { char ->
            when (char) {
                '0' -> '০'
                '1' -> '১'
                '2' -> '২'
                '3' -> '৩'
                '4' -> '৪'
                '5' -> '৫'
                '6' -> '৬'
                '7' -> '৭'
                '8' -> '৮'
                '9' -> '৯'
                else -> char
            }
        }.joinToString("")
    }

    val FORMATS = listOf(
        "রবিবার, ১২ জুলাই ২০২৬",
        " ১২ জুলাই ২০২৬, রবিবার",
        "Sunday, 12 July 2026",
        "12 July 2026, Sunday",
        "রবিবার, ১২ জুলাই ২৬",
        "Sunday, 12 july 26",
        "১২ জুলাই ২৬, রবিবার",
        "12 july 26, sunday",
        "12 July 2026",
        "১২ জুলাই ২০২৬"
    )

    fun formatDate(date: Date, formatPattern: String): String {
        val cal = Calendar.getInstance().apply { time = date }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1-based, Sunday=1
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH) // 0-based
        val year = cal.get(Calendar.YEAR)
        val shortYear = year % 100

        val bgDay = BENGALI_DAYS[dayOfWeek - 1]
        val enDay = ENGLISH_DAYS[dayOfWeek - 1]
        val bgMonth = BENGALI_MONTHS[month]
        val enMonth = ENGLISH_MONTHS[month]

        val bgDayOfMonth = toBengaliNumber(dayOfMonth.toString())
        val bgYear = toBengaliNumber(year.toString())
        val bgShortYear = toBengaliNumber(String.format("%02d", shortYear))

        val enDayOfMonth = dayOfMonth.toString()
        val enYear = year.toString()
        val enShortYear = String.format("%02d", shortYear)

        return when (formatPattern) {
            "রবিবার, ১২ জুলাই ২০২৬" -> "$bgDay, $bgDayOfMonth $bgMonth $bgYear"
            " ১২ জুলাই ২০২৬, রবিবার" -> " $bgDayOfMonth $bgMonth $bgYear, $bgDay"
            "Sunday, 12 July 2026" -> "$enDay, $enDayOfMonth $enMonth $enYear"
            "12 July 2026, Sunday" -> "$enDayOfMonth $enMonth $enYear, $enDay"
            "রবিবার, ১২ জুলাই ২৬" -> "$bgDay, $bgDayOfMonth $bgMonth $bgShortYear"
            "Sunday, 12 july 26" -> "$enDay, $enDayOfMonth ${enMonth.lowercase()} $enShortYear"
            "১২ জুলাই ২৬, রবিবার" -> "$bgDayOfMonth $bgMonth $bgShortYear, $bgDay"
            "12 july 26, sunday" -> "$enDayOfMonth ${enMonth.lowercase()} $enShortYear, ${enDay.lowercase()}"
            "12 July 2026" -> "$enDayOfMonth $enMonth $enYear"
            "১২ জুলাই ২০২৬" -> "$bgDayOfMonth $bgMonth $bgYear"
            else -> "$enDayOfMonth $enMonth $enYear" // Default fallback
        }
    }
}
