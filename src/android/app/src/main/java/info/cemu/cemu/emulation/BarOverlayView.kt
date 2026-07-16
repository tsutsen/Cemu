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
        Log.d(TAG, "onDraw: visibility=$visibility, bitmap=${localBitmap != null}, viewSize=${width}x${height}")
        if (localBitmap == null) return

        // Draw the full bitmap first
        Log.d(TAG, "onDraw: drawing bitmap, bitmapSize=${localBitmap.width}x${localBitmap.height}")
        canvas.drawBitmap(localBitmap, 0f, 0f, null)
        
        // Calculate hole size based on aspect ratio difference
        // The game content is 16:9, fit it to full width of the screen
        val gameAspectRatio = 16f / 9f
        val gameHeightAtFullWidth = (width / gameAspectRatio).toInt()
        val totalBarHeight = height - gameHeightAtFullWidth
        val barHeight = totalBarHeight / 2 // Top and bottom bars
        
        Log.d(TAG, "onDraw: screen=${width}x${height}, gameHeightAtFullWidth=$gameHeightAtFullWidth, barHeight=$barHeight")
        
        // Cut a rectangular hole in the middle to see gamepad content underneath
        // Shrink by 1 pixel on each side to avoid thin black lines from anti-aliasing
        val holeLeft = 1
        val holeTop = barHeight + 1
        val holeRight = width - 1
        val holeBottom = height - barHeight - 1
        
        Log.d(TAG, "onDraw: cutting hole at ($holeLeft,$holeTop)-($holeRight,$holeBottom)")
        
        // Use PorterDuffXfermode to cut the hole
        val paint = android.graphics.Paint().apply {
            xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
        }
        canvas.drawRect(holeLeft.toFloat(), holeTop.toFloat(), holeRight.toFloat(), holeBottom.toFloat(), paint)
        paint.xfermode = null // Reset for future draws
    }
}
