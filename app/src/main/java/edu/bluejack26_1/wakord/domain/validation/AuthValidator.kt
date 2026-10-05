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
