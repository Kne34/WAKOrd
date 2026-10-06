package edu.bluejack26_1.wakord.domain.validation

enum class FieldError {
    REQUIRED,
    INVALID_EMAIL,
    PASSWORD_TOO_SHORT,
    PASSWORD_MISMATCH,
    TOO_FEW_GENRES,
}

interface AuthValidator {
    fun fullName(value: String): FieldError?
    fun email(value: String): FieldError?
    fun password(value: String): FieldError?
    fun loginPassword(value: String): FieldError?
    fun confirmPassword(password: String, confirm: String): FieldError?
    fun genres(selected: Set<String>): FieldError?
}

class DefaultAuthValidator : AuthValidator {

    override fun fullName(value: String): FieldError? =
        if (value.isBlank()) FieldError.REQUIRED else null

    override fun email(value: String): FieldError? = when {
        value.isBlank() -> FieldError.REQUIRED
        !EMAIL_REGEX.matches(value.trim()) -> FieldError.INVALID_EMAIL
        else -> null
    }

    override fun password(value: String): FieldError? = when {
        value.isEmpty() -> FieldError.REQUIRED
        value.length < MIN_PASSWORD_LENGTH -> FieldError.PASSWORD_TOO_SHORT
        else -> null
    }

    override fun loginPassword(value: String): FieldError? =
        if (value.isEmpty()) FieldError.REQUIRED else null

    override fun confirmPassword(password: String, confirm: String): FieldError? = when {
        confirm.isEmpty() -> FieldError.REQUIRED
        confirm != password -> FieldError.PASSWORD_MISMATCH
        else -> null
    }

    override fun genres(selected: Set<String>): FieldError? =
        if (selected.size < MIN_GENRES) FieldError.TOO_FEW_GENRES else null

    companion object {
        const val MIN_PASSWORD_LENGTH = 8
        const val MIN_GENRES = 3
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
