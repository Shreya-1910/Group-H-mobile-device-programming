package com.mdpgroup08.scpd.data.repository

import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.mdpgroup08.scpd.data.model.UserRole
import kotlinx.coroutines.tasks.await

/** Why a sign-in failed. The ViewModel turns each one into a message for the user. */

enum class SignInError {
    WRONG_CREDENTIALS,
    TOO_MANY_ATTEMPTS,
    NO_CONNECTION,
    NO_ROLE,
    UNKNOWN
}

/** Thrown by the repository so the ViewModel knows which error happened. */
class SignInException(val error: SignInError) : Exception(error.name)

/**
 * Signing in and out, and finding the user's role (R6).
 * An interface, so tests can use a fake instead of Firebase.
 */

interface AuthRepository {
    /** Signs in and returns the role. Throws SignInException if it fails. */
    suspend fun signIn(email: String, password: String): UserRole

    /** Role of the user already signed in, or null if nobody is. */
    suspend fun currentUserRole(): UserRole?

    fun signOut()
}

class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override suspend fun signIn(email: String, password: String): UserRole {
        // 1. Check the email and password with Firebase Authentication
        try {
            auth.signInWithEmailAndPassword(email.trim(), password).await()
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            throw SignInException(SignInError.WRONG_CREDENTIALS)
        } catch (e: FirebaseAuthInvalidUserException) {
            // Same message as a wrong password, so nobody can find out which emails exist
            throw SignInException(SignInError.WRONG_CREDENTIALS)
        } catch (e: FirebaseTooManyRequestsException) {
            throw SignInException(SignInError.TOO_MANY_ATTEMPTS)
        } catch (e: FirebaseNetworkException) {
            throw SignInException(SignInError.NO_CONNECTION)
        }

        // 2. Read their role from Firestore
        val role = try {
            loadRole()
        } catch (e: FirebaseNetworkException) {
            auth.signOut()
            throw SignInException(SignInError.NO_CONNECTION)
        } catch (e: FirebaseFirestoreException) {
            auth.signOut()
            throw SignInException(
                if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) SignInError.NO_CONNECTION
                else SignInError.UNKNOWN
            )
        }

        // 3. No valid role means no access, so sign them back out
        if (role == null) {
            auth.signOut()
            throw SignInException(SignInError.NO_ROLE)
        }
        return role
    }

    override suspend fun currentUserRole(): UserRole? {
        if (auth.currentUser == null) return null
        return try {
            loadRole()
        } catch (e: Exception) {
            null
        }
    }

    override fun signOut() = auth.signOut()

    /** Reads users/{uid}.role from Firestore. */
    private suspend fun loadRole(): UserRole? {
        val uid = auth.currentUser?.uid ?: return null
        val snapshot = firestore.collection("users").document(uid).get().await()
        return UserRole.fromString(snapshot.getString("role"))
    }
}