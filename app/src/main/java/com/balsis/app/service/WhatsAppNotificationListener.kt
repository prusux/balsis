package com.balsis.app.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

data class CachedVoiceNoteNotification(
    val timestamp: Long,
    val senderName: String,
    val chatName: String
)

object RecentVoiceNoteCache {
    private const val MAX_SIZE = 15
    private const val MAX_AGE_MS = 10 * 60 * 1000L // 10 minutes window

    private val cache = ArrayDeque<CachedVoiceNoteNotification>()

    @Synchronized
    fun addNotification(sender: String, chat: String) {
        val now = System.currentTimeMillis()
        cache.addFirst(CachedVoiceNoteNotification(now, sender, chat))
        while (cache.size > MAX_SIZE) {
            cache.removeLast()
        }
    }

    @Synchronized
    fun findBestMatch(targetTimestamp: Long = System.currentTimeMillis()): CachedVoiceNoteNotification? {
        val now = System.currentTimeMillis()
        // Find the most recent notification within the age window
        return cache.firstOrNull { (now - it.timestamp) < MAX_AGE_MS }
    }
}

class WhatsAppNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName
        if (pkg != "com.whatsapp" && pkg != "com.whatsapp.w4b") return

        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

        // Check if notification relates to an audio note / voice message
        // WhatsApp notification texts:
        // English: "🎤 Voice message (0:12)"
        // Latvian: "🎤 Balss ziņa (0:12)"
        val isVoiceNote = text.contains("🎤") || 
                          text.contains("Voice message", ignoreCase = true) ||
                          text.contains("Balss ziņa", ignoreCase = true) ||
                          text.contains("Audio", ignoreCase = true)

        if (isVoiceNote) {
            val sender = if (title.isNotBlank()) title else "Nezināms sūtītājs"
            val chat = if (subText.isNotBlank()) subText else sender
            Log.d("BalsisNotif", "Captured WhatsApp voice note: sender=$sender, chat=$chat")
            RecentVoiceNoteCache.addNotification(sender, chat)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
