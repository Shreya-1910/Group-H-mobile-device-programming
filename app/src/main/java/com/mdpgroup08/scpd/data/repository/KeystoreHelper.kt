package com.mdpgroup08.scpd.data.repository

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey

/**
 * Handles hardware-backed Android Keystore key generation for R6-S3 (One phone per account).
 */
class KeystoreHelper {

    companion object {
        private const val KEY_ALIAS = "scpd_device_checkin_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    }

    /**
     * Creates an RSA KeyPair in Android Keystore if not present and returns the Base64 Public Key (R6-S3 AC1).
     * Only the public key is sent to the server.
     */
    fun getOrCreatePublicKey(): String {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE
            )
            keyPairGenerator.initialize(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
                )
                .setKeySize(2048)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                .build()
            )
            keyPairGenerator.generateKeyPair()
        }

        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.PrivateKeyEntry
        val publicKeyBytes = entry.certificate.publicKey.encoded
        return Base64.encodeToString(publicKeyBytes, Base64.NO_WRAP)
    }

    /**
     * Checks if this device holds the private key matching the registered server public key (R6-S3 EX1).
     */
    fun isDeviceRegistered(serverPublicKey: String?): Boolean {
        if (serverPublicKey.isNull_or_blank()) return false
        return try {
            val localPublicKey = getOrCreatePublicKey()
            localPublicKey == serverPublicKey
        } catch (e: Exception) {
            false
        }
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()
