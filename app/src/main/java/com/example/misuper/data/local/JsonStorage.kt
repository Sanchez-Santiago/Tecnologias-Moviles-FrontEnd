package com.example.misuper.data.local

import android.content.Context
import com.google.gson.Gson
import com.example.misuper.data.model.AppData

class JsonStorage(private val context: Context) {

    private val fileName = "app_data.json"
    private val gson = Gson()

    fun guardar(data: AppData) {
        val json = gson.toJson(data)

        context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
            it.write(json.toByteArray())
        }
    }

    fun cargar(): AppData? {
        return try {
            val json = context.openFileInput(fileName)
                .bufferedReader()
                .use { it.readText() }

            gson.fromJson(json, AppData::class.java)
        } catch (e: Exception) {
            null
        }
    }
}