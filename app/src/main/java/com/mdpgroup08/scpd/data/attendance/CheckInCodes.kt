package com.mdpgroup08.scpd.data.attendance

import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Rotating, signed check-in codes (R3-S1, R3-S9, R4-S23).
 *
 * Every 15 s the club device moves to a new "window counter" (time / 15 s).
 * For each counter it signs "eventId:counter" with the event's secret (HMAC-SHA256).
 * From that one signature we make:
 *   - the QR payload  "SCPD1|eventId|counter|signature"
 *   - the 6-digit code for students who can't scan (R3-S3)
 * Students never receive the secret, so they can't create valid codes (R3-S9).
 *
 * Pure Kotlin with no Android classes, so it can be unit tested and reused by the server.
 */
object CheckInCodes {

    /** A new code every 15 seconds (R4-S23 AC1). */
    const val ROTATION_MS = 15_000L

    /** Current and previous code are accepted, so a code is valid for up to 30 s (R3-S7). */
    const val ACCEPTED_COUNTERS = 2

    private const val PREFIX = "SCPD1"
    private const val SIGNATURE_BYTES = 16
    private const val SECRET_BYTES = 32

    /** The window counter for a moment in time. */
    fun counterAt(timeMillis: Long): Long = timeMillis / ROTATION_MS

    /** When a counter's 15 s window starts. */
    fun counterStart(counter: Long): Long = counter * ROTATION_MS

    /** Seconds until the next code (for the countdown on the club screen). */
    fun secondsUntilNext(timeMillis: Long): Int =
        ((ROTATION_MS - timeMillis % ROTATION_MS + 999) / 1000).toInt()

    /** A new random per-event secret, Base64 encoded. Readable only by the owning club (R3-S9 AC2). */
    fun newSecret(random: SecureRandom = SecureRandom()): String {
        val bytes = ByteArray(SECRET_BYTES).also(random::nextBytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    private fun hmac(secret: String, eventId: String, counter: Long): ByteArray {
        val key = Base64.getDecoder().decode(secret)
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal("$eventId:$counter".toByteArray(Charsets.UTF_8))
    }

    /** The signature part of the QR code (hex). */
    fun signature(secret: String, eventId: String, counter: Long): String =
        hmac(secret, eventId, counter).take(SIGNATURE_BYTES)
            .joinToString("") { "%02x".format(it) }

    /** The 6-digit fallback code, same rules as the QR (R3-S3 AC1). Uses HOTP-style truncation. */
    fun sixDigitCode(secret: String, eventId: String, counter: Long): String {
        val h = hmac(secret, eventId, counter)
        val offset = h.last().toInt() and 0x0f
        val value = ByteBuffer.wrap(h, offset, 4).int and 0x7fffffff
        return "%06d".format(value % 1_000_000)
    }

    /** The text inside the QR code. */
    fun qrPayload(secret: String, eventId: String, counter: Long): String =
        "$PREFIX|$eventId|$counter|${signature(secret, eventId, counter)}"

    /** What a scanned QR contains, or null if it isn't an s-CPD event code (R4-S6 EX1). */
    data class ScannedQr(val eventId: String, val counter: Long, val signature: String)

    fun parseQr(text: String): ScannedQr? {
        val parts = text.trim().split("|")
        if (parts.size != 4 || parts[0] != PREFIX) return null
        val counter = parts[2].toLongOrNull() ?: return null
        if (parts[1].isBlank() || counter < 0) return null
        if (!parts[3].matches(Regex("[0-9a-f]{${SIGNATURE_BYTES * 2}}"))) return null
        return ScannedQr(parts[1], counter, parts[3])
    }

    /** True if the typed text is exactly 6 digits (R4-S6 EX2). */
    fun isSixDigitFormat(text: String): Boolean = text.length == 6 && text.all(Char::isDigit)
}
