// FILE: FileHelper.kt

package com.yourcompany.partygameapp.util

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileHelper @Inject constructor(
    private val application: Application
) {
    /**
     * Given a URI from the file picker, this function reads the actual file name.
     */
    fun getFileNameFromUri(uri: Uri): String? {
        var fileName: String? = null
        val cursor = application.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = it.getString(nameIndex)
                }
            }
        }
        return fileName
    }

    /**
     * Takes a list of strings and writes each one as a new line to the file
     * represented by the given URI.
     */
    fun saveContentToUri(uri: Uri, content: List<String>) {
        application.contentResolver.openFileDescriptor(uri, "w")?.use { parcelFileDescriptor ->
            FileOutputStream(parcelFileDescriptor.fileDescriptor).use { fileOutputStream ->
                content.forEach { line ->
                    fileOutputStream.write((line + "\n").toByteArray())
                }
            }
        }
    }
}