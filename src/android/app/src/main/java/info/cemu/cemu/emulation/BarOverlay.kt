package info.cemu.cemu.emulation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import info.cemu.cemu.common.settings.BarOverlaySettings
import java.io.File

private const val TAG = "BarOverlay"

fun log(msg: String) { Log.d(TAG, msg) }

/**
 * Checks if a path is a SAF URI (Storage Access Framework).
 * SAF URIs look like: /document/msf:1000075217 or content://com.android.providers.downloads.documents/document/msf:1000075217
 */
fun isSaferUri(path: String): Boolean {
    return path.startsWith("/document/msf:") ||
        path.startsWith("content://") ||
        path.startsWith("file:///storage/")
}

/**
 * Loads a bitmap from a file path or SAF URI.
 */
@Composable
fun rememberBarOverlayBitmap(imagePath: String?): Bitmap? {
    val context = LocalContext.current
    return remember(imagePath) {
        if (imagePath.isNullOrEmpty()) {
            log("Bitmap load skipped: path is null or empty")
            return@remember null
        }
        log("Loading bitmap from: $imagePath")

        try {
            // Handle SAF URIs
            if (isSaferUri(imagePath)) {
                log("  -> Detected SAF URI, opening via ContentResolver")
                val uri = Uri.parse(imagePath)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val bitmap = BitmapFactory.decodeStream(input)
                    if (bitmap != null) {
                        log("  -> Bitmap loaded from SAF URI: ${bitmap.width}x${bitmap.height}")
                    } else {
                        log("  -> Bitmap decode from SAF URI returned null")
                    }
                    bitmap
                }?.also {
                    if (it == null) {
                        log("  -> Failed to open input stream for SAF URI")
                    }
                }
            } else {
                // Handle regular file paths
                val file = File(imagePath)
                if (!file.exists()) {
                    log("  -> Bitmap file not found: $imagePath")
                    return@remember null
                }
                log("  -> File exists, size: ${file.length()} bytes")
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    log("  -> Bitmap loaded: ${bitmap.width}x${bitmap.height}")
                } else {
                    log("  -> Bitmap decode returned null")
                }
                bitmap
            }
        } catch (e: Exception) {
            log("  -> Failed to load bar overlay image: $imagePath - ${e.message}")
            null
        }
    }
}

/**
 * Calculates the top and bottom bar positions and sizes based on screen dimensions and game surface dimensions.
 *
 * The Wii U TV resolution is 720x400 (16:9). When displayed on a 4:3 screen (e.g., 640x480),
 * there will be black bars on the top and bottom (letterboxing).
 *
 * Returns a pair of (topBarInfo, bottomBarInfo), where each is null if no bar exists at that position.
 */
fun calculateBarPositions(
    screenWidth: Int,
    screenHeight: Int,
    gameWidth: Int,
    gameHeight: Int,
): Pair<BarInfo?, BarInfo?> {
    log("calculateBarPositions: screen=${screenWidth}x${screenHeight}, game=${gameWidth}x${gameHeight}")
    if (gameWidth <= 0 || gameHeight <= 0) {
        log("  -> Returning null bars: invalid game dimensions")
        return Pair(null, null)
    }

    // Calculate the centered position of the game content
    val gameX = (screenWidth - gameWidth) / 2
    val gameY = (screenHeight - gameHeight) / 2
    log("  -> Game centered at: ($gameX, $gameY)")

    var topBar: BarInfo? = null
    var bottomBar: BarInfo? = null

    // Top bar
    if (gameY > 0) {
        topBar = BarInfo(
            offset = IntOffset(0, 0),
            size = IntSize(screenWidth, gameY)
        )
        log("  -> Top bar: offset=(0,0), size=${screenWidth}x$gameY")
    } else {
        log("  -> No top bar (gameY=$gameY <= 0)")
    }

    // Bottom bar
    val bottomBarY = gameY + gameHeight
    if (bottomBarY < screenHeight) {
        bottomBar = BarInfo(
            offset = IntOffset(0, bottomBarY),
            size = IntSize(screenWidth, screenHeight - bottomBarY)
        )
        log("  -> Bottom bar: offset=(0,$bottomBarY), size=${screenWidth}x${screenHeight - bottomBarY}")
    } else {
        log("  -> No bottom bar (bottomBarY=$bottomBarY >= screenHeight=$screenHeight)")
    }

    return Pair(topBar, bottomBar)
}

/**
 * Information about a bar's position and size.
 */
data class BarInfo(
    val offset: IntOffset,
    val size: IntSize
)

/**
 * Composable that renders bar overlay images on top of the game screen.
 *
 * This is used to display decorative images on the black bars that appear
 * when the game's aspect ratio doesn't match the screen's aspect ratio.
 *
 * For Wii U, the TV resolution is 720x400 (16:9). When displayed on a 4:3 screen,
 * there will be black bars on the top and bottom (letterboxing).
 */
@Composable
fun BarOverlay(
    screenWidthDp: Int,
    screenHeightDp: Int,
    gameWidthDp: Int,
    gameHeightDp: Int,
    barOverlaySettings: BarOverlaySettings,
    modifier: Modifier = Modifier,
) {
    log("BarOverlay called: enabled=${barOverlaySettings.isBarOverlayEnabled}, " +
        "screen=${screenWidthDp}x${screenHeightDp}, game=${gameWidthDp}x${gameHeightDp}")
    log("  Settings: topPath=${barOverlaySettings.topBarImagePath}, bottomPath=${barOverlaySettings.bottomBarImagePath}")
    log("  Alpha: top=${barOverlaySettings.topBarImageAlpha}, bottom=${barOverlaySettings.bottomBarImageAlpha}")

    if (!barOverlaySettings.isBarOverlayEnabled) {
        log("  -> Bar overlay disabled, returning")
        return
    }

    // Load the bottom bar image
    val bottomBitmap = rememberBarOverlayBitmap(barOverlaySettings.bottomBarImagePath)
    log("  Bottom bitmap loaded: ${bottomBitmap != null}")

    if (bottomBitmap != null) {
        log("  -> Rendering bottom bar overlay on entire screen area")
        BarImageOverlay(
            bitmap = bottomBitmap,
            barInfo = BarInfo(
                offset = IntOffset(0, 0),
                size = IntSize(screenWidthDp, screenHeightDp)
            ),
            alpha = barOverlaySettings.bottomBarImageAlpha,
            modifier = modifier,
        )
    } else {
        log("  -> No bottom bitmap, skipping")
    }
}

/**
 * Renders a single bar overlay image at the specified position.
 */
@Composable
private fun BarImageOverlay(
    bitmap: Bitmap,
    barInfo: BarInfo,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    val density = androidx.compose.ui.platform.LocalDensity.current.density

    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    barInfo.offset.x,
                    barInfo.offset.y
                )
            }
            .size(
                width = (barInfo.size.width * density).dp,
                height = (barInfo.size.height * density).dp
            )
            .alpha(alpha)
            .background(Color.Black.copy(alpha = 0.5f)) // Semi-transparent background for debugging
    ) {
        Image(
            bitmap = imageBitmap,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}
