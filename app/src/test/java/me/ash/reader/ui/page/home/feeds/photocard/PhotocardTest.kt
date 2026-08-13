package me.ash.reader.ui.page.home.feeds.photocard

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.Date

class PhotocardTest {

    @Test
    fun testDateFormatting() {
        // Setup Date: July 12, 2026 is a Sunday (Day of week: 1, Month: 6)
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.JULY)
            set(Calendar.DAY_OF_MONTH, 12)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
        }
        val date = cal.time

        // 1. "রবিবার, ১২ জুলাই ২০২৬"
        assertEquals("রবিবার, ১২ জুলাই ২০২৬", PhotocardDateHelper.formatDate(date, "রবিবার, ১২ জুলাই ২০২৬"))

        // 2. " ১২ জুলাই ২০২৬, রবিবার"
        assertEquals(" ১২ জুলাই ২০২৬, রবিবার", PhotocardDateHelper.formatDate(date, " ১২ জুলাই ২০২৬, রবিবার"))

        // 3. "Sunday, 12 July 2026"
        assertEquals("Sunday, 12 July 2026", PhotocardDateHelper.formatDate(date, "Sunday, 12 July 2026"))

        // 4. "12 July 2026, Sunday"
        assertEquals("12 July 2026, Sunday", PhotocardDateHelper.formatDate(date, "12 July 2026, Sunday"))

        // 5. "রবিবার, ১২ জুলাই ২৬"
        assertEquals("রবিবার, ১২ জুলাই ২৬", PhotocardDateHelper.formatDate(date, "রবিবার, ১২ জুলাই ২৬"))

        // 6. "Sunday, 12 july 26"
        assertEquals("Sunday, 12 july 26", PhotocardDateHelper.formatDate(date, "Sunday, 12 july 26"))

        // 7. "১২ জুলাই ২৬, রবিবার"
        assertEquals("১২ জুলাই ২৬, রবিবার", PhotocardDateHelper.formatDate(date, "১২ জুলাই ২৬, রবিবার"))

        // 8. "12 july 26, sunday"
        assertEquals("12 july 26, sunday", PhotocardDateHelper.formatDate(date, "12 july 26, sunday"))

        // 9. "12 July 2026"
        assertEquals("12 July 2026", PhotocardDateHelper.formatDate(date, "12 July 2026"))

        // 10. "১২ জুলাই ২০২৬"
        assertEquals("১২ জুলাই ২০২৬", PhotocardDateHelper.formatDate(date, "১২ জুলাই ২০২৬"))
    }

    @Test
    fun testWordReplacementModeration() {
        val replacements = mapOf("ধর্ষক" to "ধ*র্ষক", "খুনি" to "খ*নি")
        var title = "ধর্ষক এবং খুনি ধরা পড়েছে।"

        replacements.forEach { (bad, good) ->
            title = title.replace(bad, good)
        }

        assertEquals("ধ*র্ষক এবং খ*নি ধরা পড়েছে।", title)
    }

    @Test
    fun testUrlNormalization() {
        assertEquals("example.com/feed", PhotocardManager.normalizeUrl("https://www.example.com/feed/"))
        assertEquals("example.com/feed", PhotocardManager.normalizeUrl("http://example.com/feed"))
        assertEquals("example.com/feed", PhotocardManager.normalizeUrl("  HTTPS://WWW.EXAMPLE.COM/FEED  "))
    }
}
