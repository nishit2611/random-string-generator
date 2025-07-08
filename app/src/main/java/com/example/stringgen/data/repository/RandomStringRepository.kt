package com.example.stringgen.data.repository

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.stringgen.data.model.RandomStringData
import com.example.stringgen.data.model.RandomStringResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class RandomStringRepository(private val context: Context) {

    private val contentResolver: ContentResolver = context.contentResolver
    private val gson = Gson()

    companion object {
        private const val AUTHORITY = "com.iav.contestdataprovider"
        private const val DATA_URI = "content://com.iav.contestdataprovider/text"
        private const val DATA_COLUMN_NAME = "data"
        private const val TIMEOUT_MILLIS = 30000L
    }

    suspend fun generateRandomString(length: Int): Result<RandomStringData> = withContext(Dispatchers.IO) {
        try {
            if (length <= 0 || length > 1000) {
                return@withContext Result.failure(Exception("String length must be between 1 and 1000"))
            }

            val result = withTimeout(TIMEOUT_MILLIS) {
                suspendCancellableCoroutine<RandomStringData> { continuation ->
                    val uri = Uri.parse(DATA_URI)
                    val queryArgs = Bundle().apply {
                        putInt(ContentResolver.QUERY_ARG_LIMIT, length)
                    }

                    val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                        override fun onChange(selfChange: Boolean, uri: Uri?) {
                            super.onChange(selfChange, uri)
                        }
                    }

                    try {
                        contentResolver.registerContentObserver(uri, false, observer)

                        val cursor: Cursor? = contentResolver.query(
                            uri,
                            arrayOf(DATA_COLUMN_NAME),
                            queryArgs,
                            null
                        )

                        if (cursor == null) {
                            Log.e("Repository", "## Cursor is null — possible content provider failure")
                            continuation.resumeWithException(Exception("No response from content provider"))
                            return@suspendCancellableCoroutine
                        }

                        if (!cursor.moveToFirst()) {
                            Log.e("Repository", "## Cursor is empty — no data returned")
                            continuation.resumeWithException(Exception("Content provider returned no data"))
                            return@suspendCancellableCoroutine
                        }

                        val dataColumnIndex = cursor.getColumnIndex(DATA_COLUMN_NAME)
                        if (dataColumnIndex != -1) {
                            val jsonData = cursor.getString(dataColumnIndex)
                            try {
                                val response = gson.fromJson(jsonData, RandomStringResponse::class.java)
                                continuation.resume(response.randomText)
                            } catch (e: Exception) {
                                continuation.resumeWithException(Exception("Invalid JSON from content provider"))
                            }
                        } else {
                            continuation.resumeWithException(Exception("Data column not found"))
                        }

                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    } finally {
                        contentResolver.unregisterContentObserver(observer)
                    }
                }
            }

            Result.success(result)
        } catch (e: TimeoutCancellationException) {
            Result.failure(Exception("Request timed out. Content provider may be slow."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
