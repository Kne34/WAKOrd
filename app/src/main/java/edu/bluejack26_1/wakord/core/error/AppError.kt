package edu.bluejack26_1.wakord.core.error

import androidx.annotation.StringRes
import edu.bluejack26_1.wakord.R

sealed class AppError(message: String? = null) : Exception(message) {
    object InvalidCredentials : AppError()
    object EmailAlreadyUsed : AppError()
    object Network : AppError()
    object NotLoggedIn : AppError()
    object NotFound : AppError()
    object CommentBlocked : AppError()
    class Unknown(message: String?) : AppError(message)
}

@StringRes
fun Throwable.messageRes(): Int = when (this) {
    AppError.InvalidCredentials -> R.string.error_invalid_credentials
    AppError.EmailAlreadyUsed -> R.string.error_email_used
    AppError.Network -> R.string.error_network
    AppError.NotLoggedIn -> R.string.error_not_logged_in
    AppError.NotFound -> R.string.error_not_found
    AppError.CommentBlocked -> R.string.error_comment_blocked
    else -> R.string.error_unknown
}
