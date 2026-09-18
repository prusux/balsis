package com.balsis.app

import com.balsis.app.service.RecentVoiceNoteCache
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class BalsisUnitTest {

    @Test
    fun testNotificationCache_matching() {
        RecentVoiceNoteCache.addNotification("Jānis Bērziņš", "Ģimenes Čats")
        val match = RecentVoiceNoteCache.findBestMatch()
        assertNotNull(match)
        assertEquals("Jānis Bērziņš", match?.senderName)
        assertEquals("Ģimenes Čats", match?.chatName)
    }

    @Test
    fun testGeminiJsonParsing() {
        val sampleJson = """
            {
              "summary": "Jānis būs mājās 18:30 ar pirkumiem.",
              "fullText": "Čau, es būšu mājās ap sešiem trīsdesmit, pa ceļam iebraukšu veikalā nopirkt pienu un maizi.",
              "context": "Jānis informē par ierašanās laiku"
            }
        """.trimIndent()

        val parsed = JSONObject(sampleJson)
        assertEquals("Jānis būs mājās 18:30 ar pirkumiem.", parsed.getString("summary"))
        assertTrue(parsed.getString("fullText").contains("sešiem trīsdesmit"))
    }
}
