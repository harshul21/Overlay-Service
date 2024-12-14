package com.example.overlayappdev

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.annotation.RequiresApi
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
    private var isPipMode by mutableStateOf(false)

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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        stateOwner.create()

        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(stateOwner)
            setViewTreeSavedStateRegistryOwner(stateOwner)

            setContent {
                OverlayContent(
                    onClose = { stopSelf() },
                    onPipMode = { togglePipMode() },
                    isPipMode = isPipMode
                )
            }
        }

        updateOverlayLayout(isPipMode)
        stateOwner.resume()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun togglePipMode() {
        isPipMode = !isPipMode
        updateOverlayLayout(isPipMode)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateOverlayLayout(isPip: Boolean) {
        val params = WindowManager.LayoutParams(
            if (isPip) 500 else WindowManager.LayoutParams.WRAP_CONTENT,
            if (isPip) 400 else WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = if (isPip) Gravity.END or Gravity.BOTTOM else Gravity.CENTER
            x = 0
            y = 100
        }

        overlayView?.let { view ->
            try {
                windowManager?.updateViewLayout(view, params)
            } catch (e: IllegalArgumentException) {
                windowManager?.addView(view, params)
            }
        }
    }

    override fun onDestroy() {
        stateOwner.destroy()
        overlayView?.let { windowManager?.removeView(it) }
        super.onDestroy()
    }
}


