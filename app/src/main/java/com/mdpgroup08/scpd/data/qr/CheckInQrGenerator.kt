
package com.mdpgroup08.scpd.data.qr

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import org.json.JSONObject
import java.util.UUID

data class CheckInQrCode(
    val eventId: String,
    val codeCounter: Long,
    val issuedAt: Long,
    val expiresAt: Long,
    val nonce: String,
    val signature: String,
    val payload: String
)

object CheckInQrGenerator {

    private const val DEFAULT_VALIDITY_MS = 5 * 60 * 1000L

    // Prototype only. This is NOT a cryptographic signature.
    private const val DEMO_SIGNATURE = "DEMO_NOT_SIGNED"

    /**
     * Creates the information that will be encoded in the QR code.
     */
    fun create(
        eventId: String,
        codeCounter: Long,
        validityMillis: Long = DEFAULT_VALIDITY_MS
    ): CheckInQrCode {

        require(eventId.isNotBlank()) {
            "Event ID cannot be empty"
        }

        require(codeCounter >= 0) {
            "Code counter cannot be negative"
        }

        require(validityMillis > 0) {
            "Validity duration must be positive"
        }

        val issuedAt = System.currentTimeMillis()
        val expiresAt = issuedAt + validityMillis
        val nonce = UUID.randomUUID().toString()

        val payload = JSONObject().apply {
            put("version", 1)
            put("eventId", eventId)
            put("codeCounter", codeCounter)
            put("issuedAt", issuedAt)
            put("expiresAt", expiresAt)
            put("nonce", nonce)
            put("signature", DEMO_SIGNATURE)
        }.toString()

        return CheckInQrCode(
            eventId = eventId,
            codeCounter = codeCounter,
            issuedAt = issuedAt,
            expiresAt = expiresAt,
            nonce = nonce,
            signature = DEMO_SIGNATURE,
            payload = payload
        )
    }

    /**
     * Converts the QR payload into an Android Bitmap.
     */
    fun toBitmap(
        payload: String,
        size: Int = 512
    ): Bitmap {

        require(payload.isNotBlank()) {
            "QR payload cannot be empty"
        }

        require(size in 128..2048) {
            "QR size must be between 128 and 2048 pixels"
        }

        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 2
        )

        val matrix = QRCodeWriter().encode(
            payload,
            BarcodeFormat.QR_CODE,
            size,
            size,
            hints
        )

        val pixels = IntArray(size * size)

        for (y in 0 until size) {
            for (x in 0 until size) {
                pixels[y * size + x] =
                    if (matrix[x, y]) {
                        android.graphics.Color.BLACK
                    } else {
                        android.graphics.Color.WHITE
                    }
            }
        }

        return Bitmap.createBitmap(
            size,
            size,
            Bitmap.Config.ARGB_8888
        ).apply {
            setPixels(pixels, 0, size, 0, 0, size, size)
        }
    }

    /**
     * Creates a QR code and its corresponding Bitmap together.
     */
    fun generate(
        eventId: String,
        codeCounter: Long,
        validityMillis: Long = DEFAULT_VALIDITY_MS
    ): Pair<CheckInQrCode, Bitmap> {

        val qrCode = create(
            eventId = eventId,
            codeCounter = codeCounter,
            validityMillis = validityMillis
        )

        val bitmap = toBitmap(qrCode.payload)

        return Pair(qrCode, bitmap)
    }
}
