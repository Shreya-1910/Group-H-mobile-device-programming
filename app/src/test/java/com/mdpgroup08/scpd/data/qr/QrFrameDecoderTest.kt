package com.mdpgroup08.scpd.data.qr

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QrFrameDecoderTest {

    private val payload = "SCPD1|rZlgkUH3SBmrV2oORL26|119713|0123456789abcdef0123456789abcdef"

    /** Draws a QR code into a grey-scale frame like a camera's, with extra padding bytes per row. */
    private fun frame(text: String, size: Int, padding: Int): Triple<ByteArray, Int, Int> {
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.MARGIN to 2))
        val rowStride = size + padding
        val bytes = ByteArray(rowStride * size) { 0x7f }
        for (y in 0 until size) for (x in 0 until size) {
            bytes[y * rowStride + x] = if (matrix[x, y]) 0x10 else 0xf0.toByte()
        }
        return Triple(bytes, rowStride, size)
    }

    @Test
    fun readsTheClubQrPayload() {
        val (bytes, stride, size) = frame(payload, 300, 0)
        assertEquals(payload, QrFrameDecoder.decode(bytes, stride, size, size))
    }

    @Test
    fun readsAFrameWithRowPadding() {
        val (bytes, stride, size) = frame(payload, 320, 64)
        assertEquals(payload, QrFrameDecoder.decode(bytes, stride, size, size))
    }

    @Test
    fun emptyFrameGivesNull() {
        val bytes = ByteArray(200 * 200) { 0x55 }
        assertNull(QrFrameDecoder.decode(bytes, 200, 200, 200))
    }

    @Test
    fun badSizesGiveNullInsteadOfCrashing() {
        assertNull(QrFrameDecoder.decode(ByteArray(10), 100, 100, 100))
        assertNull(QrFrameDecoder.decode(ByteArray(100), 5, 10, 10))
    }
}
