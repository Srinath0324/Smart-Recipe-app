package com.example.airecipeapp.utils

object Constants {
    // Database
    const val DATABASE_NAME = "grocery_recipe_db"
    
    // Image Processing
    const val MAX_IMAGE_DIMENSION = 2048
    const val JPEG_QUALITY = 90
    
    // OCR
    const val MIN_CONFIDENCE_THRESHOLD = 0.5f
    
    // Recipe Matching
    const val MIN_MATCH_SCORE = 0.3f
    const val PERFECT_MATCH_SCORE = 0.9f
    
    // Units
    val WEIGHT_UNITS = listOf("kg", "g", "mg", "lb", "oz")
    val VOLUME_UNITS = listOf("l", "ml", "cup", "tbsp", "tsp", "gallon", "pint")
    val COUNT_UNITS = listOf("piece", "pieces", "pc", "pcs", "unit", "units", "whole")
    
    // File paths
    const val IMAGES_DIRECTORY = "grocery_images"
    const val PROCESSED_IMAGES_DIRECTORY = "processed_images"
    
    // Preferences
    const val PREFS_NAME = "app_preferences"
    const val PREF_FIRST_LAUNCH = "first_launch"
    const val PREF_ONBOARDING_COMPLETED = "onboarding_completed"
}
