package edu.bluejack26_1.wakord.testutil

import edu.bluejack26_1.wakord.domain.validation.AuthValidator
import edu.bluejack26_1.wakord.domain.validation.FieldError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val dispatcher: TestDispatcher = StandardTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

class FakeAuthValidator : AuthValidator {
    var fullNameError: FieldError? = null
    var emailError: FieldError? = null
    var passwordError: FieldError? = null
    var confirmError: FieldError? = null
    var genresError: FieldError? = null

    override fun fullName(value: String) = fullNameError
    override fun email(value: String) = emailError
    override fun password(value: String) = passwordError
    override fun loginPassword(value: String) = passwordError
    override fun confirmPassword(password: String, confirm: String) = confirmError
    override fun genres(selected: Set<String>) = genresError
}
