package com.example.thenobbery.logic

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf

class BulkImportWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val fileName = inputData.getString("KEY_URI_FILE") ?: return Result.failure()
        
        setProgress(workDataOf("PROGRESS" to 0f))
        
        val uris = UriListPersistence.loadUris(applicationContext, fileName)
        if (uris.isEmpty()) {
            UriListPersistence.deleteFile(applicationContext, fileName)
            return Result.failure()
        }
        
        val engine = BulkImportEngine(applicationContext)
        
        return try {
            engine.importFiles(uris) { current, total ->
                val progressValue = current.toFloat() / total.toFloat()
                setProgress(workDataOf("PROGRESS" to progressValue))
            }
            UriListPersistence.deleteFile(applicationContext, fileName)
            Result.success()
        } catch (e: Exception) {
            UriListPersistence.deleteFile(applicationContext, fileName)
            Result.failure()
        }
    }
}
