package info.cemu.cemu.common.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

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
        if (path.isNullOrEmpty()) return null

        return try {
            when {
                isSaferUri(path) -> loadFromSaferUri(context, path)
                else -> loadFromFile(context, path)
            }
        } catch (e: Exception) {
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

        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            outputFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun loadFromSaferUri(context: Context, path: String): Bitmap? {
        val uri = if (path.startsWith("/document/")) {
            val docId = path.substringAfter("/document/")
            Uri.parse("content://com.android.externalstorage.documents/document/${docId.replace(":", "%3A").replace("/", "%2F")}")
        } else {
            Uri.parse(path)
        }

        return context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input)
        }
    }

    private fun loadFromFile(context: Context, path: String): Bitmap? {
        val file = File(path)
        return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
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
