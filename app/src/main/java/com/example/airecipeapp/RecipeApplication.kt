package com.example.airecipeapp

import android.app.Application
import com.example.airecipeapp.data.local.database.AppDatabase

class RecipeApplication : Application() {
    
    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }
    
    override fun onCreate() {
        super.onCreate()
    }
}
