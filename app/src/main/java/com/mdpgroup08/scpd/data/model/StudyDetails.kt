package com.mdpgroup08.scpd.data.model

/**
 * Student study details for R6-S4.
 * End year calculation: startYear + degreeLength.
 */
data class StudyDetails(
    val currentYear: Int = 1,
    val degreeName: String = "",
    val degreeLength: Int = 3, // e.g. 3 or 4 years
    val startYear: Int = 2023,
    val extensionEndDate: Long? = null
) {
    /** Expected end year calculation (R6-S4 AC1). */
    val expectedEndYear: Int
        get() = startYear + degreeLength

    /**
     * Validates if study details fit (R6-S4 EX1).
     * e.g., Year 4 on a 3-year degree fails.
     */
    fun validate(): Result<Unit> {
        if (currentYear < 1) {
            return Result.failure(IllegalArgumentException("Current year must be at least 1"))
        }
        if (degreeLength < 1) {
            return Result.failure(IllegalArgumentException("Degree length must be at least 1 year"))
        }
        if (currentYear > degreeLength) {
            // EX1 requirement error message
            return Result.failure(IllegalArgumentException("These details do not match"))
        }
        if (degreeName.isBlank()) {
            return Result.failure(IllegalArgumentException("Degree name cannot be empty"))
        }
        return Result.success(Unit)
    }
}
