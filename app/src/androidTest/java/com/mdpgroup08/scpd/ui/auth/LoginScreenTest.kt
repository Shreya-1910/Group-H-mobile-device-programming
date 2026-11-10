package com.mdpgroup08.scpd.ui.auth

/** UI tests for the login layout. Run on the Pixel 2 API 31 emulator. */
class LoginScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    /** Shows LoginContent with working text fields, but no Firebase. */
    private fun showLogin(initial: LoginUiState = LoginUiState(isCheckingSession = false)) {
        composeRule.setContent {
            var state by remember { mutableStateOf(initial) }
            LoginContent(
                state = state,
                onEmailChange = { state = state.copy(email = it) },
                onPasswordChange = { state = state.copy(password = it) },
                onLogin = {}
            )
        }
    }

    @Test
    fun loginButton_isDisabled_whenFieldsAreEmpty() {
        showLogin()
        composeRule.onNodeWithTag(LoginTags.BUTTON).assertIsNotEnabled()
    }

    @Test
    fun loginButton_isEnabled_afterTypingEmailAndPassword() {
        showLogin()
        composeRule.onNodeWithTag(LoginTags.EMAIL).performTextInput("student1@test.com")
        composeRule.onNodeWithTag(LoginTags.PASSWORD).performTextInput("Test1234!")
        composeRule.onNodeWithTag(LoginTags.BUTTON).assertIsEnabled()
    }

    @Test
    fun errorMessage_isShown_whenStateHasError() {
        showLogin(LoginUiState(isCheckingSession = false, errorMessage = "The email or password is incorrect"))
        composeRule.onNodeWithTag(LoginTags.ERROR).assertIsDisplayed()
    }
}

