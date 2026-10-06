package com.example.util

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.zip.ZipInputStream

object XlsxParser {
    fun parseItems(inputStream: java.io.InputStream): List<TempItem> {
        val sharedStrings = mutableListOf<String>()
        val files = mutableMapOf<String, ByteArray>()
        
        try {
            val zip = ZipInputStream(inputStream)
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "xl/sharedStrings.xml" || entry.name == "xl/worksheets/sheet1.xml") {
                    files[entry.name] = zip.readBytes()
                }
                entry = zip.nextEntry
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
        
        val sharedStringsBytes = files["xl/sharedStrings.xml"]
        if (sharedStringsBytes != null) {
            try {
                sharedStrings.addAll(parseSharedStrings(sharedStringsBytes.inputStream()))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        val sheetBytes = files["xl/worksheets/sheet1.xml"]
        if (sheetBytes != null) {
            try {
                return parseSheet(sheetBytes.inputStream(), sharedStrings)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        return emptyList()
    }

    private fun parseSharedStrings(inputStream: InputStream): List<String> {
        val list = mutableListOf<String>()
        val parser = Xml.newPullParser()
        parser.setInput(inputStream, "UTF-8")
        var eventType = parser.eventType
        var currentString = StringBuilder()
        var insideT = false
        
        while (eventType != XmlPullParser.END_DOCUMENT) {
            val name = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (name == "t") {
                        insideT = true
                        currentString = StringBuilder()
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideT) {
                        currentString.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (name == "t") {
                        insideT = false
                        list.add(currentString.toString())
                    }
                }
            }
            eventType = parser.next()
        }
        return list
    }

    private fun parseSheet(inputStream: InputStream, sharedStrings: List<String>): List<TempItem> {
        val list = mutableListOf<TempItem>()
        val parser = Xml.newPullParser()
        parser.setInput(inputStream, "UTF-8")
        var eventType = parser.eventType
        
        var currentRow = mutableMapOf<Int, String>()
        var currentCellRef = ""
        var currentCellType = ""
        var insideV = false
        var currentVal = StringBuilder()
        
        while (eventType != XmlPullParser.END_DOCUMENT) {
            val name = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (name == "row") {
                        currentRow = mutableMapOf()
                    } else if (name == "c") {
                        currentCellRef = parser.getAttributeValue(null, "r") ?: ""
                        currentCellType = parser.getAttributeValue(null, "t") ?: ""
                    } else if (name == "v") {
                        insideV = true
                        currentVal = StringBuilder()
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideV) {
                        currentVal.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (name == "v") {
                        insideV = false
                        val rawVal = currentVal.toString()
                        val value = if (currentCellType == "s") {
                            val idx = rawVal.toIntOrNull()
                            if (idx != null && idx >= 0 && idx < sharedStrings.size) sharedStrings[idx] else rawVal
                        } else {
                            rawVal
                        }
                        val colIdx = excelColToIndex(currentCellRef)
                        currentRow[colIdx] = value
                    } else if (name == "row") {
                        val item = mapRowToTempItem(currentRow)
                        if (item != null) {
                            list.add(item)
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return list
    }

    private fun excelColToIndex(ref: String): Int {
        val colLetters = ref.filter { it.isLetter() }
        var index = 0
        for (c in colLetters) {
            index = index * 26 + (c - 'A' + 1)
        }
        return index - 1
    }

    private fun mapRowToTempItem(row: Map<Int, String>): TempItem? {
        val name = row[2] ?: ""
        if (name.isBlank() || name.equals("Item Name", ignoreCase = true)) return null
        
        return TempItem(
            code = row[0] ?: "",
            barcode = row[1] ?: "",
            name = name,
            cost = row[3]?.toDoubleOrNull() ?: 0.0,
            price = row[4]?.toDoubleOrNull() ?: 0.0,
            type = row[5] ?: "",
            brand = row[6] ?: "",
            size = row[7] ?: "",
            unit = row[8]?.ifBlank { "pcs" } ?: "pcs",
            mrp = row[9]?.toDoubleOrNull(),
            qty = row[10]?.toDoubleOrNull() ?: 0.0,
            aliases = row[11] ?: ""
        )
    }
}

data class TempItem(
    val code: String,
    val barcode: String,
    val name: String,
    val cost: Double,
    val price: Double,
    val type: String,
    val brand: String,
    val size: String,
    val unit: String,
    val mrp: Double?,
    val qty: Double,
    val aliases: String
)
