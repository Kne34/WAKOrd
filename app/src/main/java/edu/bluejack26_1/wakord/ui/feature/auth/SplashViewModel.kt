package edu.bluejack26_1.wakord.ui.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bluejack26_1.wakord.WAKordApp
import edu.bluejack26_1.wakord.domain.repository.AuthRepository

class SplashViewModel(private val authRepository: AuthRepository) : ViewModel() {
    val isLoggedIn: Boolean get() = authRepository.isLoggedIn

    companion object {
        val Factory = viewModelFactory {
            initializer {
                SplashViewModel((this[APPLICATION_KEY] as WAKordApp).container.authRepository)
            }
        }
    }
}
