package com.mdpgroup08.scpd.data.local

enum class CheckInStatus {
    PENDING,
    SYNCING,
    VERIFIED,
    REJECTED,
    NEEDS_REVIEW,
    COULDNT_SYNC
}

enum class CheckInMethod {
    QR,
    CODE,
    MANUAL
}