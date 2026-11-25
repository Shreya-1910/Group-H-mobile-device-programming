package com.mdpgroup08.scpd.data.repository

import com.mdpgroup08.scpd.data.model.StudyDetails
import com.mdpgroup08.scpd.data.model.User
import com.mdpgroup08.scpd.data.model.UserRole

/**
 * Shared fake AuthRepository for unit tests.
 * If AuthRepository gets a new function, add it here once and every test keeps compiling.
 */
class FakeAuthRepository(
    private val role: UserRole = UserRole.STUDENT,
    private val clubId: String? = null,
    private val uid: String? = "fake_user"
) : AuthRepository {
    override suspend fun signIn(email: String, password: String) = role
    override suspend fun registerStudent(
        email: String, password: String, name: String,
        pdpaAccepted: Boolean, studyDetails: StudyDetails
    ) {}
    override suspend fun currentUserRole() = role
    override suspend fun currentUser(): User? = null
    override suspend fun currentClubId() = clubId
    override suspend fun currentUserId() = uid
    override suspend fun verifyCheckInDevice() = true
    override fun signOut() {}
}