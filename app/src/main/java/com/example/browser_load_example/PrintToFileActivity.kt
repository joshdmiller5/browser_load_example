package com.example.browser_load_example

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.IOException
import java.io.OutputStream

/**
 * Delete any file with the same name in the Downloads directory before creating a new one.
 * Otherwise, the code will create a new file with name plus a number suffix.
 */
class PrintToFileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_print_to_file)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val timingString = if (SessionHolder.timings.isEmpty()) {
            return
        } else if (SessionHolder.timings.size == 1) {
            SessionHolder.timings[0].toString()
        } else {
            SessionHolder.timings.joinToString(";")
        }
        intent.extras?.getString(EXTRA_LOG_TO_FILE_NAME)?.let {
            saveFile(this@PrintToFileActivity, it, timingString, "txt")
        }
    }

    @Throws(IOException::class)
    private fun saveFile(context: Context, fileName: String, text: String, extension: String) {
        val extVolumeUri: Uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
        )
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf("${fileName}_9.txt")
        val cursor =
            context.contentResolver.query(extVolumeUri, projection, selection, selectionArgs, null)
                ?: return

        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
        val nameColumn =
            cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
        var fileExists = false
        val fileUri: Uri? = if (cursor.moveToFirst()) {
            // File exists, get its URI
            val id = cursor.getLong(idColumn)
            val name = cursor.getString(nameColumn)

            val contentUri: Uri = ContentUris.withAppendedId(
                extVolumeUri,
                id
            )
            fileExists = true
            contentUri
        } else {
            // File does not exist, create a new one
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "${fileName}_9")
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            context.contentResolver.insert(extVolumeUri, values)
        }
        cursor?.close()

        fileUri?.let {
            val outputStream: OutputStream? =
                context.contentResolver.openOutputStream(it, "wa") // Open in append mode
            val bytes = if (fileExists) ";$text".toByteArray() else text.toByteArray()
            outputStream?.write(bytes)
            outputStream?.close()
        }
    }

    companion object {
        const val EXTRA_LOG_TO_FILE_NAME = "extra_log_to_file_name"
    }
}