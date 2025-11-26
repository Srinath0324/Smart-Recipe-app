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
        
        // Initialize default values
        var quantity = "1"
        var unit = "piece"
        var name = cleanedLine
        
        // Check for dash-separated format: "Item - Quantity+Unit"
        // Handle various dash types: - – —
        val dashPattern = Regex("""(.+?)\s*[-–—]\s*(.+)""")
        val dashMatch = dashPattern.find(cleanedLine)
        
        if (dashMatch != null) {
            // Split by dash: left side is item name, right side is quantity+unit
            val (itemPart, quantityPart) = dashMatch.destructured
            name = itemPart.trim()
            
            // Extract quantity and unit from the right side
            val extracted = extractQuantityAndUnit(quantityPart.trim())
            if (extracted != null) {
                quantity = extracted.first
                unit = extracted.second
            }
        } else {
            // No dash found, try to parse the entire line
            val extracted = extractQuantityAndUnit(cleanedLine)
            if (extracted != null) {
                quantity = extracted.first
                unit = extracted.second
                // Remove the quantity and unit from the name
                name = cleanedLine
                    .replace(Regex("""${Regex.escape(quantity)}\s*${Regex.escape(unit)}""", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("""${Regex.escape(unit)}\s*${Regex.escape(quantity)}""", RegexOption.IGNORE_CASE), "")
                    .trim()
            }
        }
        
        // Clean up name - remove any remaining special characters
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
    
    /**
     * Extract quantity and unit from a string
     * Returns Pair(quantity, unit) or null if not found
     */
    private fun extractQuantityAndUnit(text: String): Pair<String, String>? {
        val normalizedText = text.trim()
        
        for (pattern in QUANTITY_PATTERNS) {
            val match = pattern.find(normalizedText)
            if (match != null) {
                val (first, second) = match.destructured
                
                // Check if first group is number or unit
                val isFirstNumber = first.toDoubleOrNull() != null
                
                if (isFirstNumber) {
                    val possibleUnit = second.lowercase(Locale.getDefault())
                    if (UNITS.contains(possibleUnit) || possibleUnit.length <= 4) {
                        return Pair(first, possibleUnit)
                    }
                } else {
                    // Reversed format: "kg 1"
                    val possibleUnit = first.lowercase(Locale.getDefault())
                    if (UNITS.contains(possibleUnit) || possibleUnit.length <= 4) {
                        return Pair(second, possibleUnit)
                    }
                }
            }
        }
        
        return null
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
            // Normalize whitespace first
            .replace(Regex("""\s+"""), " ")
            // Fix common OCR errors in numbers
            .replace(Regex("""\bI\s*([kKgGmMlL])\b"""), "1$1") // "Ikg" -> "1kg"
            .replace(Regex("""\bO\s*([kKgGmMlL])\b"""), "0$1") // "Okg" -> "0kg"
            // Preserve dashes (important for "Item - Quantity" format)
            // Remove other special chars except word chars, space, dot, dash
            .replace(Regex("""[^\w\s.\-–—]"""), " ")
            // Normalize multiple spaces again
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}
