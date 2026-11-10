package com.mdpgroup08.scpd.viewmodel

/** A fake AuthRepository: no Firebase, so tests are fast and repeatable. */
private class FakeAuthRepository(
    var roleToReturn: UserRole? = null,
    var errorToThrow: SignInError? = null,
    var alreadySignedInAs: UserRole? = null
) : AuthRepository {
    var signInCalls = 0

    override suspend fun signIn(email: String, password: String): UserRole {
        signInCalls++
        errorToThrow?.let { throw SignInException(it) }
        return roleToReturn ?: throw SignInException(SignInError.NO_ROLE)
    }

    override suspend fun currentUserRole(): UserRole? = alreadySignedInAs
    override fun signOut() { alreadySignedInAs = null }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    // ViewModels use the Android main thread, which doesn't exist in unit tests,
    // so we swap in a test thread before each test and remove it after.
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `button is disabled until both fields are filled`() {
        val vm = LoginViewModel(FakeAuthRepository())
        assertFalse(vm.uiState.value.canSubmit)

        vm.onEmailChange("student1@test.com")
        assertFalse(vm.uiState.value.canSubmit)

        vm.onPasswordChange("Test1234!")
        assertTrue(vm.uiState.value.canSubmit)
    }

    @Test
    fun `wrong password shows the incorrect credentials message`() = runTest {
        val vm = LoginViewModel(FakeAuthRepository(errorToThrow = SignInError.WRONG_CREDENTIALS))
        vm.onEmailChange("student1@test.com")
        vm.onPasswordChange("wrong")
        vm.signIn()

        val state = vm.uiState.value
        assertEquals("The email or password is incorrect", state.errorMessage)
        assertNull(state.signedInRole)
        assertFalse(state.isLoading)
    }

    @Test
    fun `student sign in returns the STUDENT role`() = runTest {
        val vm = LoginViewModel(FakeAuthRepository(roleToReturn = UserRole.STUDENT))
        vm.onEmailChange("student1@test.com")
        vm.onPasswordChange("Test1234!")
        vm.signIn()

        assertEquals(UserRole.STUDENT, vm.uiState.value.signedInRole)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `club sign in returns the CLUB role`() = runTest {
        val vm = LoginViewModel(FakeAuthRepository(roleToReturn = UserRole.CLUB))
        vm.onEmailChange("club1@test.com")
        vm.onPasswordChange("Test1234!")
        vm.signIn()

        assertEquals(UserRole.CLUB, vm.uiState.value.signedInRole)
    }

    @Test
    fun `account without a role is not let in`() = runTest {
        val vm = LoginViewModel(FakeAuthRepository(errorToThrow = SignInError.NO_ROLE))
        vm.onEmailChange("someone@test.com")
        vm.onPasswordChange("Test1234!")
        vm.signIn()

        assertNull(vm.uiState.value.signedInRole)
        assertEquals(LoginViewModel.messageFor(SignInError.NO_ROLE), vm.uiState.value.errorMessage)
    }

    @Test
    fun `no connection shows the connection message`() = runTest {
        val vm = LoginViewModel(FakeAuthRepository(errorToThrow = SignInError.NO_CONNECTION))
        vm.onEmailChange("student1@test.com")
        vm.onPasswordChange("Test1234!")
        vm.signIn()

        assertEquals(LoginViewModel.messageFor(SignInError.NO_CONNECTION), vm.uiState.value.errorMessage)
    }

    @Test
    fun `sign in is not attempted with empty fields`() = runTest {
        val repo = FakeAuthRepository(roleToReturn = UserRole.STUDENT)
        val vm = LoginViewModel(repo)
        vm.signIn()

        assertEquals(0, repo.signInCalls)
    }

    @Test
    fun `already signed in user skips the login screen`() = runTest {
        val vm = LoginViewModel(FakeAuthRepository(alreadySignedInAs = UserRole.CLUB))

        assertFalse(vm.uiState.value.isCheckingSession)
        assertEquals(UserRole.CLUB, vm.uiState.value.signedInRole)
    }

    @Test
    fun `typing clears a previous error`() = runTest {
        val vm = LoginViewModel(FakeAuthRepository(errorToThrow = SignInError.WRONG_CREDENTIALS))
        vm.onEmailChange("student1@test.com")
        vm.onPasswordChange("wrong")
        vm.signIn()
        vm.onPasswordChange("wrong2")

        assertNull(vm.uiState.value.errorMessage)
    }
}