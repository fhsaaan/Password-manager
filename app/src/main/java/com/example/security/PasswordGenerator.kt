package com.example.security

import java.security.SecureRandom

object PasswordGenerator {

    private val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    private val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private val DIGITS = "0123456789"
    private val SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?"

    fun generate(
        length: Int = 16,
        includeUppercase: Boolean = true,
        includeLowercase: Boolean = true,
        includeDigits: Boolean = true,
        includeSymbols: Boolean = true
    ): String {
        val charPool = StringBuilder()
        val forcedChars = StringBuilder()
        val random = SecureRandom()

        if (includeLowercase) {
            charPool.append(LOWERCASE)
            forcedChars.append(LOWERCASE[random.nextInt(LOWERCASE.length)])
        }
        if (includeUppercase) {
            charPool.append(UPPERCASE)
            forcedChars.append(UPPERCASE[random.nextInt(UPPERCASE.length)])
        }
        if (includeDigits) {
            charPool.append(DIGITS)
            forcedChars.append(DIGITS[random.nextInt(DIGITS.length)])
        }
        if (includeSymbols) {
            charPool.append(SYMBOLS)
            forcedChars.append(SYMBOLS[random.nextInt(SYMBOLS.length)])
        }

        if (charPool.isEmpty()) {
            return ""
        }

        val password = StringBuilder()
        // Start with the characters forced to guarantee at least one from each selected pool
        password.append(forcedChars)

        val remainingLength = length - forcedChars.length
        for (i in 0 until remainingLength) {
            password.append(charPool[random.nextInt(charPool.length)])
        }

        // Shuffle the characters
        val list = password.toList().shuffled(random)
        return list.joinToString("")
    }

    fun evaluateStrength(password: String): PasswordStrength {
        if (password.isEmpty()) return PasswordStrength.EMPTY
        if (password.length < 6) return PasswordStrength.WEAK

        var score = 0
        if (password.length >= 8) score++
        if (password.length >= 12) score++
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++

        return when {
            score <= 3 -> PasswordStrength.WEAK
            score == 4 || score == 5 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.STRONG
        }
    }
}

enum class PasswordStrength(val label: String, val score: Int) {
    EMPTY("No Password", 0),
    WEAK("Weak Profile", 1),
    MEDIUM("Medium Security", 2),
    STRONG("Highly Secure", 3)
}
