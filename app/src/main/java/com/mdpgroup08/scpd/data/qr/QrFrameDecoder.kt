package com.mdpgroup08.scpd.data.qr

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.ChecksumException
import com.google.zxing.DecodeHintType
import com.google.zxing.FormatException
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader

/**
 * Reads a QR code from one grey-scale camera frame (R3-S2).
 * Uses ZXing, which the app already uses to draw the club's QR, so no extra library is needed.
 * Pure Kotlin with no Android classes, so it can be unit tested.
 */
object QrFrameDecoder {

    private val hints: Map<DecodeHintType, Any> = mapOf(
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        DecodeHintType.TRY_HARDER to true
    )

    /**
     * @param luminance the camera's brightness bytes (the Y plane), at least rowStride * height long
     * @param rowStride bytes per row in [luminance] (can be more than [width] because of padding)
     * @return the text inside the QR code, or null if no QR code was found in this frame
     */
    fun decode(luminance: ByteArray, rowStride: Int, width: Int, height: Int): String? {
        if (width <= 0 || height <= 0 || rowStride < width || luminance.size < rowStride * height) return null
        val source = PlanarYUVLuminanceSource(luminance, rowStride, height, 0, 0, width, height, false)
        return try {
            QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source)), hints).text
        } catch (e: NotFoundException) {
            null
        } catch (e: ChecksumException) {
            null
        } catch (e: FormatException) {
            null
        }
    }
}
