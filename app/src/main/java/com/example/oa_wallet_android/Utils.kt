package com.example.oa_wallet_android

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.*

class Utils {
    companion object {
        fun getFileName(context: Context, uri: Uri): String? {
            var result: String? = null
            if (uri.scheme == "content") {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                try {
                    if (cursor != null && cursor.moveToFirst()) {
                        result =
                            cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                    }
                } finally {
                    cursor!!.close()
                }
            }
            if (result == null) {
                result = uri.path
                val cut = result!!.lastIndexOf('/')
                if (cut != -1) {
                    result = result.substring(cut + 1)
                }
            }
            return result
        }

        fun readDocument(context: Context, uri: Uri): String? {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val content = inputStream.bufferedReader().use(BufferedReader::readText)
                    inputStream.close()
                    return content
                }

            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return null
        }

        fun readDocument(file: File): String? {
            try {
                val inputStream = file.inputStream()
                val content = inputStream.bufferedReader().use(BufferedReader::readText)
                inputStream.close()

                return content
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return null
        }

        fun deleteDocument(file: File) {
            file.delete()
        }
    }
}