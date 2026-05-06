package com.example.misuper.data.local

import android.content.Context
import com.google.gson.Gson
import com.example.misuper.data.model.AppData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JsonStorage(private val context: Context) {

    private val fileName = "app_data.json"
    private val gson = Gson()

    suspend fun guardar(data: AppData) = withContext(Dispatchers.IO) {
        val json = gson.toJson(data)

        context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
            it.write(json.toByteArray())
        }
    }

    suspend fun cargar(): AppData? = withContext(Dispatchers.IO) {
        return@withContext try {
            val json = context.openFileInput(fileName)
                .bufferedReader()
                .use { it.readText() }

            gson.fromJson(json, AppData::class.java)
        } catch (e: Exception) {
            null
        }
    }
}