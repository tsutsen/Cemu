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
        // Initialize with the provided settings
        setSettings(settings)
    }

    fun setSettings(newSettings: BarOverlaySettings) {
        Log.d(TAG, "*** BarOverlayView.setSettings CALLED *** enabled=${newSettings.isBarOverlayEnabled}, path=${newSettings.bottomBarImagePath}")
        settings = newSettings
        if (!settings.isBarOverlayEnabled) {
            visibility = GONE
            Log.d(TAG, "setSettings: disabled, hiding view")
            return
        }

        visibility = VISIBLE
        Log.d(TAG, "setSettings: enabled, showing view")

        // Load the bottom bar image
        val newPath = settings.bottomBarImagePath
        Log.d(TAG, "setSettings: newPath=$newPath, imagePath=$imagePath")
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
            // Check if it's a SAF URI
            if (path.startsWith("/document/") || path.startsWith("content://")) {
                Log.d(TAG, "Detected SAF URI, trying to resolve via ContentResolver")
                // SAF URI - try to extract the file path or use ContentResolver
                val context = context
                // Convert /document/primary:path to content://com.android.externalstorage.documents/document/primary%3ADownload%3Agradient%283%29.jpeg
                val uri = if (path.startsWith("/document/")) {
                    // Convert /document/primary:Download/gradient(3).jpeg to content://com.android.externalstorage.documents/document/primary%3ADownload%3Agradient%283%29.jpeg
                    val docId = path.substringAfter("/document/")
                    android.net.Uri.parse("content://com.android.externalstorage.documents/document/${docId.replace(":", "%3A").replace("/", "%2F")}")
                } else {
                    android.net.Uri.parse(path)
                }
                Log.d(TAG, "Converted URI: $uri")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val bmp = BitmapFactory.decodeStream(input)
                    if (bmp != null) {
                        Log.d(TAG, "Bitmap loaded from SAF URI: ${bmp.width}x${bmp.height}")
                    } else {
                        Log.w(TAG, "Bitmap decode from SAF URI returned null")
                    }
                    bmp
                }?.also {
                    if (it == null) {
                        Log.w(TAG, "Failed to open input stream for SAF URI")
                    }
                }
            } else {
                // Regular file path
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
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load bar overlay image: $path - ${e.message}")
            null
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val localBitmap = bitmap
        if (localBitmap == null) return

        // Draw the bitmap stretched to fill the entire view
        val destRect = android.graphics.RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawBitmap(localBitmap, null, destRect, null)
        
        // Calculate hole size based on aspect ratio difference
        // The game content is 16:9, fit it to full width of the screen
        val gameAspectRatio = 16f / 9f
        val gameHeightAtFullWidth = (width / gameAspectRatio).toInt()
        val totalBarHeight = height - gameHeightAtFullWidth
        val barHeight = totalBarHeight / 2 // Top and bottom bars
        
        // Cut a rectangular hole in the middle to see gamepad content underneath
        // Shrink by 1 pixel on top and bottom only to avoid thin black lines from anti-aliasing
        val holeLeft = 0
        val holeTop = barHeight + 1
        val holeRight = width
        val holeBottom = height - barHeight - 1
        
        // Use PorterDuffXfermode to cut the hole
        val paint = android.graphics.Paint().apply {
            xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
        }
        canvas.drawRect(holeLeft.toFloat(), holeTop.toFloat(), holeRight.toFloat(), holeBottom.toFloat(), paint)
        paint.xfermode = null // Reset for future draws
    }
}
