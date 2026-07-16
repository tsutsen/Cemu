package info.cemu.cemu.emulation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.util.Log
import android.view.View
import info.cemu.cemu.common.settings.BarOverlaySettings
import java.io.File

private const val TAG = "BarOverlayView"

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
        visibility = GONE
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
        if (path.isNullOrEmpty()) return null
        Log.d(TAG, "Loading bar overlay bitmap from: $path")
        return try {
            val file = File(path)
            if (!file.exists()) {
                Log.w(TAG, "Bar overlay image not found: $path")
                return null
            }
            Log.d(TAG, "File exists, size: ${file.length()} bytes")
            val bmp = BitmapFactory.decodeFile(file.absolutePath)
            if (bmp != null) {
                Log.d(TAG, "Bitmap loaded: ${bmp.width}x${bmp.height}")
            } else {
                Log.w(TAG, "Bitmap decode returned null")
            }
            bmp
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load bar overlay image: $path - ${e.message}")
            null
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val localBitmap = bitmap
        Log.d(TAG, "onDraw: visibility=$visibility, bitmap=${localBitmap != null}, alpha=${settings.bottomBarImageAlpha}, viewSize=${width}x${height}")
        if (localBitmap == null) return

        val alpha = (settings.bottomBarImageAlpha * 255).toInt().coerceIn(0, 255)
        Log.d(TAG, "onDraw: drawing bitmap with alpha=$alpha, bitmapSize=${localBitmap.width}x${localBitmap.height}")
        
        // Clear the canvas first to ensure transparency
        canvas.drawColor(Color.TRANSPARENT)
        
        // Draw the bitmap with the specified alpha using Paint
        val paint = android.graphics.Paint().apply {
            this.alpha = alpha
        }
        canvas.drawBitmap(localBitmap, 0f, 0f, paint)
    }
}
