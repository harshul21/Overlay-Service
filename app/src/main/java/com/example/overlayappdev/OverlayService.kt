package com.example.overlayappdev

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

class OverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null

    // Combined LifecycleOwner and SavedStateRegistryOwner
    private inner class OverlayStateOwner : LifecycleOwner, SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateRegistryController = SavedStateRegistryController.create(this).apply {
            performRestore(null)
        }

        override val lifecycle: Lifecycle
            get() = lifecycleRegistry

        override val savedStateRegistry: SavedStateRegistry
            get() = savedStateRegistryController.savedStateRegistry

        fun create() {
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
        }

        fun resume() {
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        }

        fun destroy() {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }
    }

    private val stateOwner = OverlayStateOwner()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Initialize the lifecycle state
        stateOwner.create()

        // Create the overlay view using Compose
        overlayView = ComposeView(this).apply {
            // Set the combined owner for both lifecycle and saved state
            setViewTreeLifecycleOwner(stateOwner)
            setViewTreeSavedStateRegistryOwner(stateOwner)

            setContent {
                OverlayContent(
                    onClose = { stopSelf() }
                )
            }
        }

        // Set up window parameters
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            x = 0
            y = 0
        }

        // Add the view to window manager
        windowManager?.addView(overlayView, params)

        // Move to RESUMED state after adding the view
        stateOwner.resume()
    }

    override fun onDestroy() {
        // Move to DESTROYED state
        stateOwner.destroy()

        overlayView?.let { windowManager?.removeView(it) }
        super.onDestroy()
    }
}


@Composable
fun OverlayContent(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .background(Color.Green, shape = RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "Hello from Overlay",
                color = Color.Black,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Button(onClick = onClose) {
                Text(text = "Close")
            }
            Button(onClick = onClose) {
                Text(text = "Pip")
            }

        }
    }
}
