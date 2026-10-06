package com.example.util

import java.util.Locale

object UnitConversionHelper {

    val COMMON_UNITS = listOf(
        "pcs",
        "box",
        "mtr",
        "roll",
        "pkt",
        "dozen",
        "kg",
        "carton",
        "bundle",
        "set",
        "feet",
        "sqft"
    )

    data class ConversionPair(
        val baseUnit: String,
        val secondaryUnit: String,
        val defaultFactor: Double,
        val label: String
    )

    val PRESET_CONVERSIONS = listOf(
        ConversionPair("box", "pcs", 10.0, "1 Box = 10 Pcs"),
        ConversionPair("roll", "mtr", 50.0, "1 Roll = 50 Mtrs"),
        ConversionPair("dozen", "pcs", 12.0, "1 Dozen = 12 Pcs"),
        ConversionPair("pkt", "pcs", 100.0, "1 Packet = 100 Pcs"),
        ConversionPair("carton", "box", 20.0, "1 Carton = 20 Boxes"),
        ConversionPair("bundle", "pcs", 25.0, "1 Bundle = 25 Pcs"),
        ConversionPair("kg", "gm", 1000.0, "1 Kg = 1000 gm")
    )

    /**
     * Suggests the counterpart unit (e.g. "box" -> "pcs", "pcs" -> "box", "roll" -> "mtr")
     */
    fun getPairedUnit(unit: String): Pair<String, Double> {
        val u = unit.trim().lowercase(Locale.ROOT)
        return when (u) {
            "box", "boxes" -> Pair("pcs", 10.0)
            "pcs", "pc", "piece", "pieces" -> Pair("box", 10.0)
            "roll", "rolls" -> Pair("mtr", 50.0)
            "mtr", "meter", "meters", "m" -> Pair("roll", 50.0)
            "dozen", "dz" -> Pair("pcs", 12.0)
            "pkt", "packet", "packets" -> Pair("pcs", 100.0)
            "carton", "ctn" -> Pair("box", 20.0)
            "bundle", "bdl" -> Pair("pcs", 25.0)
            "kg" -> Pair("gm", 1000.0)
            else -> Pair("pcs", 1.0)
        }
    }

    /**
     * Converts quantity between units given a packaging ratio factor
     */
    fun convertQuantity(fromUnit: String, toUnit: String, qty: Double, factor: Double = 10.0): Double {
        val from = fromUnit.trim().lowercase(Locale.ROOT)
        val to = toUnit.trim().lowercase(Locale.ROOT)
        if (from == to || factor <= 0.0) return qty

        // Major unit to minor unit (e.g. Box -> Pcs, Roll -> Mtr, Dozen -> Pcs)
        if (isMajorToMinor(from, to)) {
            return qty * factor
        }
        // Minor unit to major unit (e.g. Pcs -> Box, Mtr -> Roll)
        if (isMajorToMinor(to, from)) {
            return qty / factor
        }
        return qty
    }

    private fun isMajorToMinor(major: String, minor: String): Boolean {
        return (major in listOf("box", "boxes") && minor in listOf("pcs", "pc", "piece", "pieces")) ||
                (major in listOf("roll", "rolls") && minor in listOf("mtr", "meter", "meters", "m")) ||
                (major in listOf("dozen", "dz") && minor in listOf("pcs", "pc", "piece", "pieces")) ||
                (major in listOf("pkt", "packet", "packets") && minor in listOf("pcs", "pc", "piece", "pieces")) ||
                (major in listOf("carton", "ctn") && minor in listOf("box", "boxes", "pcs", "pc")) ||
                (major in listOf("bundle", "bdl") && minor in listOf("pcs", "pc", "piece", "pieces")) ||
                (major in listOf("kg") && minor in listOf("gm", "grams"))
    }
}
