package info.cemu.cemu.emulation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Paint
import android.view.View
import info.cemu.cemu.common.settings.BarOverlaySettings
import info.cemu.cemu.common.util.BitmapLoader

/**
 * A View that renders a bar overlay image on top of the game screen.
 * This is used in PadPresentation to render the bar overlay on the external display.
 */
class BarOverlayView(
    context: Context,
    private var settings: BarOverlaySettings,
) : View(context) {
    private var bitmap: Bitmap? = null
    private var imagePath: String? = null

    init {
        // Use hardware layer caching so the view is only drawn once
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        // Initialize with the provided settings
        setSettings(settings)
    }

    fun setSettings(newSettings: BarOverlaySettings) {
        settings = newSettings
        if (!settings.isBarOverlayEnabled) {
            visibility = GONE
            return
        }

        visibility = VISIBLE

        // Load the bottom bar image
        val newPath = settings.bottomBarImagePath
        if (newPath != imagePath) {
            imagePath = newPath
            bitmap = loadBitmap(newPath)
        }
        invalidate()
    }

    private fun loadBitmap(path: String?): Bitmap? {
        return BitmapLoader.loadFromPath(context, path)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val localBitmap = bitmap
        if (localBitmap == null) return

        // Calculate hole size based on aspect ratio difference
        // The game content is 16:9, fit it to full width of the screen
        val gameAspectRatio = 16f / 9f
        val gameHeightAtFullWidth = (width / gameAspectRatio).toInt()
        val totalBarHeight = height - gameHeightAtFullWidth
        val barHeight = totalBarHeight / 2 // Top and bottom bars
        
        // Draw the full bitmap stretched to fill the entire view
        val destRect = android.graphics.RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawBitmap(localBitmap, null, destRect, null)
        
        // Cut a rectangular hole in the middle to see gamepad content underneath
        // Shrink by 1px on top/bottom edges to avoid thin black lines from anti-aliasing
        val holeLeft = 0
        val holeTop = barHeight + 1
        val holeRight = width
        val holeBottom = height - barHeight - 1
        
        // Use PorterDuffXfermode to cut the hole
        val paint = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
        canvas.drawRect(holeLeft.toFloat(), holeTop.toFloat(), holeRight.toFloat(), holeBottom.toFloat(), paint)
        paint.xfermode = null // Reset for future draws
    }
}
