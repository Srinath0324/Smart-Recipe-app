package com.example.airecipeapp.domain.usecase

import com.example.airecipeapp.data.models.Ingredient
import com.example.airecipeapp.domain.ml.RecognitionResult
import java.util.Locale

/**
 * Enhanced text parser using structured text blocks from OCR
 * Handles multiple formats and normalizes text for better accuracy
 */
class ParseIngredientsUseCase {
    
    // Common units for grocery items
    private val UNITS = setOf(
        "kg", "g", "mg", "lb", "oz",
        "l", "ml", "gal", "qt", "pt",
        "cup", "cups", "tbsp", "tsp",
        "piece", "pieces", "pc", "pcs",
        "dozen", "doz", "unit", "units",
        "bunch", "bag", "box", "can", "bottle"
    )
    
    // Regex patterns for quantity extraction
    private val QUANTITY_PATTERNS = listOf(
        // "1kg", "500g", "2.5l"
        Regex("""(\d+(?:\.\d+)?)\s*([a-zA-Z]+)"""),
        // "1 kg", "500 g", "2.5 l"
        Regex("""(\d+(?:\.\d+)?)\s+([a-zA-Z]+)"""),
        // "kg 1", "g 500" (reversed)
        Regex("""([a-zA-Z]+)\s+(\d+(?:\.\d+)?)""")
    )
    
    /**
     * Parse OCR result into structured ingredients using text blocks
     */
    fun parse(ocrResult: RecognitionResult): List<Ingredient> {
        val items = mutableListOf<Ingredient>()
        
        // Process each text block
        for (block in ocrResult.blocks) {
            // Try to parse each line in the block
            for (line in block.lines) {
                val item = parseLine(line.text)
                if (item != null) {
                    items.add(item)
                }
            }
        }
        
        // If structured parsing failed, fallback to raw text
        if (items.isEmpty() && ocrResult.text.isNotBlank()) {
            items.addAll(parseRawText(ocrResult.text))
        }
        
        return items.distinctBy { it.name.lowercase() }
    }
    
    /**
     * Parse raw text (legacy support)
     */
    fun parse(rawText: String): List<Ingredient> {
        if (rawText.isBlank()) return emptyList()
        
        return parseRawText(rawText)
    }
    
    private fun parseLine(line: String): Ingredient? {
        if (line.isBlank()) return null
        
        // Clean and normalize the line
        val cleanedLine = cleanText(line)
        if (cleanedLine.length < 2) return null
        
        // Try to extract quantity and unit
        var quantity = "1"
        var unit = "piece"
        var name = cleanedLine
        
        for (pattern in QUANTITY_PATTERNS) {
            val match = pattern.find(cleanedLine)
            if (match != null) {
                val (first, second) = match.destructured
                
                // Check if first group is number or unit
                val isFirstNumber = first.toDoubleOrNull() != null
                
                if (isFirstNumber) {
                    quantity = first
                    val possibleUnit = second.lowercase(Locale.getDefault())
                    if (UNITS.contains(possibleUnit) || possibleUnit.length <= 4) {
                        unit = possibleUnit
                        // Remove quantity and unit from name
                        name = cleanedLine.replace(match.value, "").trim()
                    }
                } else {
                    // Reversed format: "kg 1"
                    val possibleUnit = first.lowercase(Locale.getDefault())
                    if (UNITS.contains(possibleUnit) || possibleUnit.length <= 4) {
                        unit = possibleUnit
                        quantity = second
                        name = cleanedLine.replace(match.value, "").trim()
                    }
                }
                break
            }
        }
        
        // Clean up name
        name = name.replace(Regex("""[-–—:,.]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        
        if (name.isEmpty() || name.length < 2) return null
        
        // Capitalize first letter
        name = name.lowercase().replaceFirstChar { it.uppercase() }
        
        return Ingredient(
            name = name,
            quantity = quantity,
            unit = unit,
            confidence = 0.8f
        )
    }
    
    private fun parseRawText(rawText: String): List<Ingredient> {
        val items = mutableListOf<Ingredient>()
        val lines = rawText.split("\n")
        
        for (line in lines) {
            val item = parseLine(line)
            if (item != null) {
                items.add(item)
            }
        }
        
        return items
    }
    
    private fun cleanText(text: String): String {
        return text
            .replace(Regex("""[^\w\s.-]"""), " ") // Remove special chars except word chars, space, dot, dash
            .replace(Regex("""\s+"""), " ") // Normalize whitespace
            .trim()
    }
}
