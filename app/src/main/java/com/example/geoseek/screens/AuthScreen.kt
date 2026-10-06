// Title screen with sign in and sign up, shown over the wandering world until a player is remembered.
package com.example.geoseek.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private enum class AuthMode { Menu, LogIn, SignUp }

@Composable
fun AuthScreen(store: AuthStore, onAuthenticated: (Player) -> Unit, modifier: Modifier = Modifier) {
    var mode by rememberSaveable { mutableStateOf(AuthMode.Menu) }
    BackHandler(enabled = mode != AuthMode.Menu) { mode = AuthMode.Menu }

    Column(
        modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(if (mode == AuthMode.Menu) 72.dp else 24.dp))
        GeoLogo(fontSize = if (mode == AuthMode.Menu) 60f else 44f)
        OutlinedText("Seek the world around you", GeoType.Heading, Modifier.padding(top = 6.dp), color = GeoColors.TextSoft)
        Spacer(Modifier.height(if (mode == AuthMode.Menu) 96.dp else 28.dp))

        AnimatedContent(
            targetState = mode,
            transitionSpec = {
                (fadeIn(tween(320, delayMillis = 120)) + slideInVertically(tween(420)) { it / 6 }) togetherWith
                    (fadeOut(tween(160)) + slideOutVertically(tween(260)) { -it / 8 })
            },
            label = "authMode",
        ) { m ->
            when (m) {
                AuthMode.Menu -> TitleMenu(onSelect = { mode = it })
                AuthMode.LogIn, AuthMode.SignUp -> AuthForm(
                    signUp = m == AuthMode.SignUp,
                    store = store,
                    onAuthenticated = onAuthenticated,
                    onSwitch = { mode = if (m == AuthMode.SignUp) AuthMode.LogIn else AuthMode.SignUp },
                    onBack = { mode = AuthMode.Menu },
                )
            }
        }
    }
}

@Composable
private fun TitleMenu(onSelect: (AuthMode) -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MenuItem("Sign In", { onSelect(AuthMode.LogIn) }, Modifier.entrance(0))
        MenuItem("Create Explorer", { onSelect(AuthMode.SignUp) }, Modifier.entrance(1))
        Spacer(Modifier.height(40.dp))
        OutlinedText(
            "Find real objects. Collect them as cards.\nLevel up your explorer.",
            GeoType.Body,
            Modifier.entrance(2),
            color = GeoColors.TextSoft,
            outlineWidth = 2.5.dp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AuthForm(
    signUp: Boolean,
    store: AuthStore,
    onAuthenticated: (Player) -> Unit,
    onSwitch: () -> Unit,
    onBack: () -> Unit,
) {
    val focus = LocalFocusManager.current
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var errorField by rememberSaveable { mutableStateOf<AuthField?>(null) }

    fun submit() {
        focus.clearFocus()
        val result = if (signUp) store.signUp(username, password, confirm) else store.logIn(username, password)
        when (result) {
            is AuthResult.Success -> onAuthenticated(result.player)
            is AuthResult.Failure -> {
                error = result.message
                errorField = result.field
            }
        }
    }

    fun edited() {
        error = null
        errorField = null
    }

    Column(Modifier.widthIn(max = 460.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Panel(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedText(if (signUp) "Create Explorer" else "Sign In", GeoType.Title, outlineWidth = 3.dp)
                GeoTextField(
                    username, { username = it; edited() }, "Explorer name",
                    error = errorField == AuthField.Username,
                    onImeAction = { focus.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) },
                )
                GeoTextField(
                    password, { password = it; edited() }, "Password",
                    password = true,
                    error = errorField == AuthField.Password,
                    imeAction = if (signUp) ImeAction.Next else ImeAction.Go,
                    onImeAction = { if (signUp) focus.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) else submit() },
                )
                AnimatedVisibility(signUp, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    GeoTextField(
                        confirm, { confirm = it; edited() }, "Confirm password",
                        password = true,
                        error = errorField == AuthField.Confirm,
                        imeAction = ImeAction.Go,
                        onImeAction = { submit() },
                    )
                }
                AnimatedVisibility(error != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    OutlinedText(error.orEmpty(), GeoType.Label, color = GeoColors.Danger, outlineWidth = 2.dp)
                }
                PixelButton(
                    if (signUp) "Start Exploring" else "Enter World",
                    ::submit,
                    Modifier.fillMaxWidth(),
                    enabled = username.isNotBlank() && password.isNotEmpty() && (!signUp || confirm.isNotEmpty()),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        MenuItem(
            if (signUp) "Have an explorer? Sign in" else "New here? Create an explorer",
            onSwitch,
            style = GeoType.Heading,
        )
        Box(Modifier.padding(top = 4.dp)) {
            MenuItem("Back", onBack, icon = Icons.Back, accent = GeoColors.TextSoft, style = GeoType.Heading)
        }
    }
}
