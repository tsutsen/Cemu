package info.cemu.cemu.emulation

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.view.Display
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import android.view.WindowManager
import info.cemu.cemu.common.settings.BarOverlaySettings

class PadPresentation(
    context: Context,
    display: Display,
    private val rotateLeft: Boolean,
    private val holderCallback: SurfaceHolder.Callback,
    private val touchListener: CanvasOnTouchListener,
    private val barOverlaySettings: BarOverlaySettings,
) : Presentation(context, display) {
    private var barOverlayView: BarOverlayView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window?.addFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        )

        val mode = display.mode
        val (surfaceWidth, surfaceHeight) = computeSurfaceSize(
            width = mode.physicalWidth,
            height = mode.physicalHeight,
            rotateLeft = rotateLeft,
        )

        // Create a FrameLayout to hold both the SurfaceView and the bar overlay
        val frameLayout = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
        }

        val surfaceView = SurfaceView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
            holder.setFixedSize(surfaceWidth, surfaceHeight)
            holder.addCallback(holderCallback)
            setOnTouchListener(touchListener)
        }

        // Create the bar overlay view
        barOverlayView = BarOverlayView(context, barOverlaySettings).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
        }

        // Add both views to the frame layout
        frameLayout.addView(surfaceView)
        frameLayout.addView(barOverlayView)

        setContentView(frameLayout)
    }

    /**
     * Update the bar overlay settings when they change.
     */
    fun updateBarOverlaySettings(settings: BarOverlaySettings) {
        android.util.Log.d("BarOverlay", "PadPresentation.updateBarOverlaySettings: barOverlayView=${barOverlayView != null}, settings=${settings.isBarOverlayEnabled}")
        if (barOverlayView != null) {
            android.util.Log.d("BarOverlay", "Calling barOverlayView.setSettings...")
            barOverlayView!!.setSettings(settings)
        } else {
            android.util.Log.e("BarOverlay", "barOverlayView is null!")
        }
    }

    private fun computeSurfaceSize(width: Int, height: Int, rotateLeft: Boolean): Pair<Int, Int> {
        var surfaceWidth = width
        var surfaceHeight = height

        if (surfaceWidth < surfaceHeight) {
            val tmp = surfaceWidth
            surfaceWidth = surfaceHeight
            surfaceHeight = tmp
        }

        if (rotateLeft) {
            val tmp = surfaceWidth
            surfaceWidth = surfaceHeight
            surfaceHeight = tmp
        }

        return surfaceWidth to surfaceHeight
    }
}
