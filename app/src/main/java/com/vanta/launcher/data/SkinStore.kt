package com.vanta.launcher.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object SkinStore {
    private const val MAX_SKIN_BYTES = 2 * 1024 * 1024

    /** Imports a Java skin texture into app-private storage and returns its absolute path. */
    fun importSkin(context: Context, accountId: Long, uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                require(total <= MAX_SKIN_BYTES) { "Skin PNG must be smaller than 2 MB." }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        } ?: throw IllegalArgumentException("The selected skin file could not be opened.")
        require(bytes.isNotEmpty()) { "The selected skin file is empty." }

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        val validDimensions = (options.outWidth == 64 && options.outHeight in setOf(32, 64)) ||
            (options.outWidth == 128 && options.outHeight in setOf(64, 128))
        require(validDimensions) {
            "Unsupported skin size: ${options.outWidth}×${options.outHeight}. Use 64×64, 64×32, 128×128, or 128×64 PNG."
        }

        val directory = File(context.filesDir, "skins").apply { mkdirs() }
        val destination = File(directory, "skin_${accountId}_${UUID.randomUUID()}.png")
        FileOutputStream(destination).use { it.write(bytes) }
        return destination.absolutePath
    }
}
