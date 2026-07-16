package info.cemu.cemu.common.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.File

private const val TAG = "BitmapLoader"

/**
 * Utility object for loading bitmaps from file paths or SAF URIs.
 * Handles both regular file paths and Storage Access Framework (SAF) URIs.
 */
object BitmapLoader {

    /**
     * Loads a bitmap from a file path or SAF URI.
     *
     * @param context Android context for accessing content resolver
     * @param path File path or SAF URI (e.g., "/document/primary:Download/image.jpg")
     * @return Bitmap if successful, null otherwise
     */
    fun loadFromPath(context: Context, path: String?): Bitmap? {
        if (path.isNullOrEmpty()) {
            Log.d(TAG, "loadFromPath: path is null or empty")
            return null
        }

        Log.d(TAG, "Loading bitmap from: $path")

        return try {
            when {
                isSaferUri(path) -> loadFromSaferUri(context, path)
                else -> loadFromFile(context, path)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load bitmap from $path: ${e.message}")
            null
        }
    }

    /**
     * Copies a file from a SAF URI to the app's internal storage.
     * Returns the path to the copied file, or null on failure.
     *
     * @param context Android context for accessing content resolver and files directory
     * @param uri SAF URI to copy from
     * @return Absolute path to the copied file, or null on failure
     */
    fun copyFromUriToInternal(context: Context, uri: Uri): String? {
        val fileName = uri.lastPathSegment?.sanitizeFileName() ?: "bar_overlay.png"
        val outputFile = File(context.filesDir, fileName)

        Log.d(TAG, "Copying file from URI to: ${outputFile.absolutePath}")

        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "File copied successfully, size: ${outputFile.length()} bytes")
            outputFile.absolutePath
        } catch (e: Exception) {
            Log.w(TAG, "Failed to copy file from URI: ${e.message}")
            null
        }
    }

    private fun loadFromSaferUri(context: Context, path: String): Bitmap? {
        Log.d(TAG, "  -> Detected SAF URI, opening via ContentResolver")
        val uri = if (path.startsWith("/document/")) {
            // Convert /document/primary:Download/image.jpg to content://com.android.externalstorage.documents/document/primary%3ADownload%2Fimage.jpg
            val docId = path.substringAfter("/document/")
            Uri.parse("content://com.android.externalstorage.documents/document/${docId.replace(":", "%3A").replace("/", "%2F")}")
        } else {
            Uri.parse(path)
        }

        Log.d(TAG, "  -> Converted URI: $uri")

        return context.contentResolver.openInputStream(uri)?.use { input ->
            val bitmap = BitmapFactory.decodeStream(input)
            if (bitmap != null) {
                Log.d(TAG, "  -> Bitmap loaded from SAF URI: ${bitmap.width}x${bitmap.height}")
            } else {
                Log.w(TAG, "  -> Bitmap decode from SAF URI returned null")
            }
            bitmap
        }?.also {
            if (it == null) {
                Log.w(TAG, "  -> Failed to open input stream for SAF URI")
            }
        }
    }

    private fun loadFromFile(context: Context, path: String): Bitmap? {
        val file = File(path)
        if (!file.exists()) {
            Log.w(TAG, "  -> Bitmap file not found: $path")
            return null
        }

        Log.d(TAG, "  -> File exists, size: ${file.length()} bytes")
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        if (bitmap != null) {
            Log.d(TAG, "  -> Bitmap loaded: ${bitmap.width}x${bitmap.height}")
        } else {
            Log.w(TAG, "  -> Bitmap decode returned null")
        }
        return bitmap
    }

    /**
     * Checks if a path is a SAF URI (Storage Access Framework).
     */
    private fun isSaferUri(path: String): Boolean {
        return path.startsWith("/document/") ||
            path.startsWith("content://") ||
            path.startsWith("file:///storage/")
    }

    /**
     * Sanitizes a filename by replacing invalid characters with underscores.
     */
    private fun String.sanitizeFileName(): String {
        return replace(Regex("[\\/:*?\"<>|]"), "_").trim()
    }
}
