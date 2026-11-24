
package com.mdpgroup08.scpd.data.validation

sealed interface CodeValidationResult {
    data object Valid : CodeValidationResult
    data object InvalidFormat : CodeValidationResult
    data object IncorrectCode : CodeValidationResult
}

object CheckInCodeValidator {

    private const val CODE_LENGTH = 6

    /**
     * Validates the format and compares the entered code
     * against the currently accepted event codes.
     *
     * The accepted codes will eventually come from the
     * event's QR/manual-code verification mechanism.
     */
    fun validate(
        enteredCode: String,
        acceptedCodes: Set<String>
    ): CodeValidationResult {

        val code = enteredCode.trim()

        // Must contain exactly six numeric digits.
        if (code.length != CODE_LENGTH || !code.all { it.isDigit() }) {
            return CodeValidationResult.InvalidFormat
        }

        // Compare against the accepted current/previous codes.
        if (code !in acceptedCodes) {
            return CodeValidationResult.IncorrectCode
        }

        return CodeValidationResult.Valid
    }
}
