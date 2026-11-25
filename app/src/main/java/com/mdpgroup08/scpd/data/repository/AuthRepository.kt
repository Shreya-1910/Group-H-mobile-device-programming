package com.mdpgroup08.scpd.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.mdpgroup08.scpd.data.model.AccountStatus
import com.mdpgroup08.scpd.data.model.StudyDetails
import com.mdpgroup08.scpd.data.model.User
import com.mdpgroup08.scpd.data.model.UserRole
import kotlinx.coroutines.tasks.await

/** Why a sign-in failed (R6-S1). */
enum class SignInError {
    WRONG_CREDENTIALS,
    TOO_MANY_ATTEMPTS,
    BLOCKED_15_MINS,
    NO_CONNECTION,
    NO_ROLE,
    ACCOUNT_DEACTIVATED,
    UNKNOWN
}

/** Thrown by the repository so the ViewModel knows which error happened. */
class SignInException(val error: SignInError, message: String? = null) : Exception(message ?: error.name)
class StudyDetailsMismatchException(message: String = "These details do not match") : Exception(message)

/**
 * Authentication, Registration, Role resolution, and Device security (R6).
 */
interface AuthRepository {
    /** Signs in and returns the role. Throws SignInException if it fails. */
    suspend fun signIn(email: String, password: String): UserRole

    /** Registers a new student account (R6-S1, R6-S2, R6-S3, R6-S4). */
    suspend fun registerStudent(
        email: String,
        password: String,
        name: String,
        pdpaAccepted: Boolean,
        studyDetails: StudyDetails
    )

    /** Role of the user already signed in, or null if nobody is. */
    suspend fun currentUserRole(): UserRole?

    /** Fetch full User object for current user. */
    suspend fun currentUser(): User?

    /** clubId of the signed-in club account, or null. */
    suspend fun currentClubId(): String? = null

    /** uid of the signed-in user, or null if nobody is signed in. Used for audit entries. */
    suspend fun currentUserId(): String? = null

    /** Verifies whether current device key matches student's registered key (R6-S3). */
    suspend fun verifyCheckInDevice(): Boolean

    fun signOut()
}

class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val keystoreHelper: KeystoreHelper = KeystoreHelper(),
    private val deviceHelper: DeviceHelper? = null
) : AuthRepository {

    companion object {
        // R6-S1 AC2: 5 wrong passwords block account for 15 minutes (900,000 ms)
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val BLOCK_DURATION_MS = 15 * 60 * 1000L

        private val failedAttemptsMap = mutableMapOf<String, Int>()
        private val blockedUntilMap = mutableMapOf<String, Long>()
    }

    override suspend fun signIn(email: String, password: String): UserRole {
        val trimmedEmail = email.trim().lowercase()

        // 1. Check if blocked due to 5 wrong attempts (R6-S1 AC2)
        val blockedUntil = blockedUntilMap[trimmedEmail] ?: 0L
        if (System.currentTimeMillis() < blockedUntil) {
            throw SignInException(SignInError.BLOCKED_15_MINS)
        }

        // 2. Check email and password with Firebase Auth
        try {
            auth.signInWithEmailAndPassword(trimmedEmail, password).await()
            // Reset failed attempts on success
            failedAttemptsMap.remove(trimmedEmail)
            blockedUntilMap.remove(trimmedEmail)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            handleFailedAttempt(trimmedEmail)
            throw SignInException(SignInError.WRONG_CREDENTIALS) // R6-S1 EX1 generic error
        } catch (e: FirebaseAuthInvalidUserException) {
            handleFailedAttempt(trimmedEmail)
            if (e.errorCode == "ERROR_USER_DISABLED") {
                throw SignInException(SignInError.ACCOUNT_DEACTIVATED)
            }
            throw SignInException(SignInError.WRONG_CREDENTIALS) // R6-S1 EX1 generic error
        } catch (e: FirebaseTooManyRequestsException) {
            throw SignInException(SignInError.TOO_MANY_ATTEMPTS)
        } catch (e: FirebaseNetworkException) {
            throw SignInException(SignInError.NO_CONNECTION)
        }

        // 3. Read role and account status from Firestore
        val user = currentUser()
        if (user == null) {
            auth.signOut()
            throw SignInException(SignInError.NO_ROLE)
        }

        if (user.accountStatus == AccountStatus.DEACTIVATED.name) {
            auth.signOut()
            throw SignInException(SignInError.ACCOUNT_DEACTIVATED)
        }

        val role = UserRole.fromString(user.role)
        if (role == null) {
            auth.signOut()
            throw SignInException(SignInError.NO_ROLE)
        }

        return role
    }

    private fun handleFailedAttempt(email: String) {
        val attempts = (failedAttemptsMap[email] ?: 0) + 1
        failedAttemptsMap[email] = attempts
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            blockedUntilMap[email] = System.currentTimeMillis() + BLOCK_DURATION_MS
        }
    }

    override suspend fun registerStudent(
        email: String,
        password: String,
        name: String,
        pdpaAccepted: Boolean,
        studyDetails: StudyDetails
    ) {
        // R6-S1 AC1: Must accept PDPA privacy notice
        if (!pdpaAccepted) {
            throw IllegalArgumentException("You must accept the privacy notice (PDPA)")
        }

        // R6-S4 EX1: Validate study details (e.g. Year 4 on 3-year degree fails with "These details do not match")
        val validation = studyDetails.validate()
        if (validation.isFailure) {
            throw StudyDetailsMismatchException(validation.exceptionOrNull()?.message ?: "These details do not match")
        }

        val trimmedEmail = email.trim().lowercase()

        // R6-S13 AC1 & EX2: Check if device is registered to another account
        val deviceId = deviceHelper?.getDeviceId()
        if (deviceId != null) {
            val existing = firestore.collection("users").whereEqualTo("deviceId", deviceId).get().await()
            if (!existing.isEmpty) {
                throw IllegalStateException("Phone already registered. Contact s-CPD office if you share a phone.")
            }
        }

        // Create Firebase Auth user
        val authResult = auth.createUserWithEmailAndPassword(trimmedEmail, password).await()
        val uid = authResult.user?.uid ?: throw SignInException(SignInError.UNKNOWN)

        // R6-S1 AC1: Send email verification
        authResult.user?.sendEmailVerification()

        // R6-S3 AC1: Create Keystore key and send public key
        val publicKey = try {
            keystoreHelper.getOrCreatePublicKey()
        } catch (e: Exception) {
            null
        }

        // R6-S2 EX1: "role" parameter from request is ignored; forcibly assigned STUDENT.
        val userDoc = User(
            id = uid,
            name = name.trim(),
            email = trimmedEmail,
            role = UserRole.STUDENT.name, // Ignore external role field (R6-S2 EX1)
            pdpaAccepted = true,
            accountStatus = AccountStatus.PENDING_VERIFICATION.name,
            publicKey = publicKey,
            deviceId = deviceId,
            currentYear = studyDetails.currentYear,
            degreeName = studyDetails.degreeName.trim(),
            degreeLength = studyDetails.degreeLength,
            startYear = studyDetails.startYear,
            expectedEndYear = studyDetails.expectedEndYear
        )

        firestore.collection("users").document(uid).set(userDoc).await()
    }

    override suspend fun currentUserRole(): UserRole? {
        val user = currentUser() ?: return null
        return UserRole.fromString(user.role)
    }

    override suspend fun currentUser(): User? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val snapshot = firestore.collection("users").document(uid).get().await()
            snapshot.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun currentClubId(): String? {
        val user = currentUser() ?: return null
        return user.clubId
    }

    override suspend fun currentUserId(): String? = auth.currentUser?.uid

    override suspend fun verifyCheckInDevice(): Boolean {
        val user = currentUser() ?: return false
        return keystoreHelper.isDeviceRegistered(user.publicKey)
    }

    override fun signOut() = auth.signOut()
}
