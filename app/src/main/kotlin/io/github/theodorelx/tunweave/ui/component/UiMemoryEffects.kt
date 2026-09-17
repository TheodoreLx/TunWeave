package io.github.theodorelx.tunweave.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Releases screen-only data while the activity is hidden, then asks the screen
 * to load it again when the user returns. VPN state remains untouched.
 */
@Composable
fun ReleaseUiDataWhenBackgrounded(
    onRelease: () -> Unit,
    onResume: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentRelease = rememberUpdatedState(onRelease)
    val currentResume = rememberUpdatedState(onResume)

    DisposableEffect(lifecycleOwner) {
        var releasedForBackground = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    currentRelease.value()
                    releasedForBackground = true
                }
                Lifecycle.Event.ON_START -> {
                    if (releasedForBackground) {
                        currentResume.value()
                        releasedForBackground = false
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            currentRelease.value()
        }
    }
}
