package com.example.util

import java.security.MessageDigest

object SecurityUtils {
    private const val SALT = "QR_FIELD_FORCE_2026_SECURE_SALT"

    fun hashPassword(password: String): String {
        val input = "$password:$SALT"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(password: String, storedHash: String): Boolean {
        val hash = hashPassword(password)
        return hash.equals(storedHash, ignoreCase = true)
    }
}
