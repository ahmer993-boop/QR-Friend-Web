package com.example.util

import android.content.Context
import android.net.Uri
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

/**
 * Lightweight, zero-dependency reader for Excel (.xlsx) and CSV/TSV files.
 * Uses standard Android XML Pull Parser and ZipInputStream.
 */
object XlsxReader {

    /**
     * Reads a Uri from Context. If it starts with the ZIP magic number (PK), parses as XLSX.
     * Otherwise, reads as UTF-8 plain text (CSV/TSV).
     */
    fun readUriToCsvText(context: Context, uri: Uri): String {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return ""
            val bytes = inputStream.use { it.readBytes() }
            if (bytes.size >= 4 &&
                bytes[0] == 0x50.toByte() &&
                bytes[1] == 0x4B.toByte() &&
                bytes[2] == 0x03.toByte() &&
                bytes[3] == 0x04.toByte()
            ) {
                // It is a ZIP / XLSX file!
                parseXlsxBytesToCsv(bytes)
            } else {
                // Plain text / CSV
                String(bytes, Charsets.UTF_8)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun parseXlsxBytesToCsv(bytes: ByteArray): String {
        return try {
            val sharedStrings = mutableListOf<String>()
            var sheetBytes: ByteArray? = null

            ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name.lowercase()
                    if (name == "xl/sharedstrings.xml") {
                        val ssBytes = zis.readBytes()
                        sharedStrings.addAll(parseSharedStrings(ssBytes))
                    } else if (name.startsWith("xl/worksheets/sheet") && name.endsWith(".xml") && sheetBytes == null) {
                        sheetBytes = zis.readBytes()
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            if (sheetBytes == null) return ""
            parseSheetXmlToCsv(sheetBytes!!, sharedStrings)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private fun parseSharedStrings(xmlBytes: ByteArray): List<String> {
        val list = mutableListOf<String>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")

        var eventType = parser.eventType
        var currentString = StringBuilder()
        var insideSi = false
        var insideT = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "si" -> {
                            insideSi = true
                            currentString = StringBuilder()
                        }
                        "t" -> insideT = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideSi && insideT) {
                        currentString.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "si" -> {
                            insideSi = false
                            list.add(currentString.toString())
                        }
                        "t" -> insideT = false
                    }
                }
            }
            eventType = parser.next()
        }
        return list
    }

    private fun parseSheetXmlToCsv(sheetXmlBytes: ByteArray, sharedStrings: List<String>): String {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(ByteArrayInputStream(sheetXmlBytes), "UTF-8")

        val resultCsv = StringBuilder()
        var eventType = parser.eventType

        var currentRowCells = mutableMapOf<Int, String>()
        var currentCellRef = ""
        var currentCellType = ""
        var currentCellValue = StringBuilder()
        var insideV = false
        var insideInlineT = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> {
                            currentRowCells = mutableMapOf()
                        }
                        "c" -> {
                            currentCellRef = parser.getAttributeValue(null, "r") ?: ""
                            currentCellType = parser.getAttributeValue(null, "t") ?: ""
                            currentCellValue = StringBuilder()
                        }
                        "v" -> insideV = true
                        "t" -> if (currentCellType == "inlineStr") insideInlineT = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideV || insideInlineT) {
                        currentCellValue.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "v" -> insideV = false
                        "t" -> insideInlineT = false
                        "c" -> {
                            val rawVal = currentCellValue.toString().trim()
                            val finalVal = when (currentCellType) {
                                "s" -> {
                                    val idx = rawVal.toIntOrNull() ?: -1
                                    if (idx in sharedStrings.indices) sharedStrings[idx] else rawVal
                                }
                                "b" -> if (rawVal == "1") "TRUE" else "FALSE"
                                else -> rawVal
                            }
                            val colIdx = colNameToIndex(currentCellRef)
                            currentRowCells[colIdx] = finalVal
                        }
                        "row" -> {
                            if (currentRowCells.isNotEmpty()) {
                                val maxCol = currentRowCells.keys.maxOrNull() ?: -1
                                val rowValues = (0..maxCol).map { col ->
                                    val cell = currentRowCells[col] ?: ""
                                    escapeCsvCell(cell)
                                }
                                resultCsv.append(rowValues.joinToString(",")).append("\n")
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return resultCsv.toString()
    }

    private fun colNameToIndex(cellRef: String): Int {
        var col = 0
        for (ch in cellRef) {
            if (ch in 'A'..'Z') {
                col = col * 26 + (ch - 'A' + 1)
            } else if (ch in 'a'..'z') {
                col = col * 26 + (ch.uppercaseChar() - 'A' + 1)
            } else {
                break
            }
        }
        return (col - 1).coerceAtLeast(0)
    }

    private fun escapeCsvCell(text: String): String {
        return if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            "\"" + text.replace("\"", "\"\"") + "\""
        } else {
            text
        }
    }
}
