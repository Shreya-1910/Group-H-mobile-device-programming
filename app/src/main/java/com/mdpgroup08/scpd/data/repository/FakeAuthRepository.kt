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
    private val clubName: String = "Robotics Club",
    private val uid: String? = "fake_user"
) : AuthRepository {
    override suspend fun signIn(email: String, password: String) = role
    override suspend fun registerStudent(
        email: String, password: String, name: String,
        pdpaAccepted: Boolean, studyDetails: StudyDetails
    ) {}
    override suspend fun currentUserRole() = role

    /** Like Firebase: null when nobody is signed in, or for a club account with no club linked. */
    override suspend fun currentUser(): User? = when {
        uid == null -> null
        role == UserRole.CLUB && clubId == null -> null
        else -> User(
            id = uid,
            role = role.name,
            clubId = clubId,
            name = if (role == UserRole.CLUB) clubName else "Test User",
            clubName = if (role == UserRole.CLUB) clubName else null
        )
    }

    override suspend fun currentClubId() = clubId
    override suspend fun currentUserId() = uid
    override suspend fun verifyCheckInDevice() = true
    override fun signOut() {}
}