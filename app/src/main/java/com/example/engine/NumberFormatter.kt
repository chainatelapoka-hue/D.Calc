package com.example.engine

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

enum class NumberFormatStyle {
    SOUTH_ASIAN,   // Lakhs, Crores, Arabs, Kharabs (1,23,45,678)
    INTERNATIONAL  // Thousands, Millions, Billions, Trillions (12,345,678)
}

data class LargeNumberBreakdown(
    val original: String,
    val internationalNotation: String,
    val southAsianNotation: String,
    val internationalWords: String,
    val southAsianWords: String,
    val scientificNotation: String
)

object NumberFormatter {

    /**
     * Formats a BigDecimal with commas according to user's selected style and max decimals.
     */
    fun formatWithCommas(
        value: BigDecimal,
        style: NumberFormatStyle = NumberFormatStyle.SOUTH_ASIAN,
        maxDecimals: Int = 2,
        trimTrailingZeros: Boolean = true
    ): String {
        val isNegative = value < BigDecimal.ZERO
        val absVal = value.abs()
        val plain = absVal.toPlainString()
        val parts = plain.split(".")
        val intPart = parts[0]
        val decPart = if (parts.size > 1) parts[1] else ""

        val formattedInt = when (style) {
            NumberFormatStyle.SOUTH_ASIAN -> formatSouthAsianInt(intPart)
            NumberFormatStyle.INTERNATIONAL -> formatInternationalInt(intPart)
        }

        val formattedDec = if (decPart.isNotEmpty() && maxDecimals > 0) {
            val truncated = if (decPart.length > maxDecimals) decPart.substring(0, maxDecimals) else decPart
            if (trimTrailingZeros) {
                val trimmed = truncated.trimEnd('0')
                if (trimmed.isEmpty()) "" else ".$trimmed"
            } else {
                ".$truncated"
            }
        } else {
            ""
        }

        val prefix = if (isNegative) "-" else ""
        return "$prefix$formattedInt$formattedDec"
    }

    private fun formatInternationalInt(intStr: String): String {
        val sb = StringBuilder()
        var count = 0
        for (i in intStr.length - 1 downTo 0) {
            sb.append(intStr[i])
            count++
            if (count % 3 == 0 && i > 0) {
                sb.append(',')
            }
        }
        return sb.reverse().toString()
    }

    private fun formatSouthAsianInt(intStr: String): String {
        if (intStr.length <= 3) return intStr
        val sb = StringBuilder()
        val lastThree = intStr.takeLast(3)
        val rest = intStr.dropLast(3)

        var count = 0
        for (i in rest.length - 1 downTo 0) {
            sb.append(rest[i])
            count++
            if (count % 2 == 0 && i > 0) {
                sb.append(',')
            }
        }
        val formattedRest = sb.reverse().toString()
        return "$formattedRest,$lastThree"
    }

    /**
     * Produces a compact badge representation (e.g., "1.50 Cr" or "15.00 M").
     */
    fun getCompactBadge(value: BigDecimal, style: NumberFormatStyle = NumberFormatStyle.SOUTH_ASIAN): String? {
        val absVal = value.abs()
        if (absVal < BigDecimal(1000)) return null

        return when (style) {
            NumberFormatStyle.SOUTH_ASIAN -> {
                // 1 Kharab = 10^11
                // 1 Arab = 10^9
                // 1 Crore = 10^7
                // 1 Lakh = 10^5
                // 1 Thousand = 10^3
                when {
                    absVal >= BigDecimal("100000000000") -> {
                        val v = absVal.divide(BigDecimal("100000000000"), 2, RoundingMode.HALF_UP)
                        "$v Kharab"
                    }
                    absVal >= BigDecimal("1000000000") -> {
                        val v = absVal.divide(BigDecimal("1000000000"), 2, RoundingMode.HALF_UP)
                        "$v Arab"
                    }
                    absVal >= BigDecimal("10000000") -> {
                        val v = absVal.divide(BigDecimal("10000000"), 2, RoundingMode.HALF_UP)
                        "$v Crore"
                    }
                    absVal >= BigDecimal("100000") -> {
                        val v = absVal.divide(BigDecimal("100000"), 2, RoundingMode.HALF_UP)
                        "$v Lakh"
                    }
                    absVal >= BigDecimal("1000") -> {
                        val v = absVal.divide(BigDecimal("1000"), 2, RoundingMode.HALF_UP)
                        "$v K"
                    }
                    else -> null
                }
            }
            NumberFormatStyle.INTERNATIONAL -> {
                when {
                    absVal >= BigDecimal("1000000000000") -> {
                        val v = absVal.divide(BigDecimal("1000000000000"), 2, RoundingMode.HALF_UP)
                        "$v Trillion"
                    }
                    absVal >= BigDecimal("1000000000") -> {
                        val v = absVal.divide(BigDecimal("1000000000"), 2, RoundingMode.HALF_UP)
                        "$v Billion"
                    }
                    absVal >= BigDecimal("1000000") -> {
                        val v = absVal.divide(BigDecimal("1000000"), 2, RoundingMode.HALF_UP)
                        "$v Million"
                    }
                    absVal >= BigDecimal("1000") -> {
                        val v = absVal.divide(BigDecimal("1000"), 2, RoundingMode.HALF_UP)
                        "$v K"
                    }
                    else -> null
                }
            }
        }
    }

    /**
     * Complete breakdown for the Large Number Converter screen.
     */
    fun getLargeNumberBreakdown(value: BigDecimal): LargeNumberBreakdown {
        val intVal = value.toBigInteger()
        val formattedInt = formatWithCommas(value, NumberFormatStyle.INTERNATIONAL, 4)
        val formattedSA = formatWithCommas(value, NumberFormatStyle.SOUTH_ASIAN, 4)

        // Scientific notation
        val symbols = DecimalFormatSymbols(Locale.US)
        val sciFormat = DecimalFormat("0.######E0", symbols)
        val sciStr = try {
            sciFormat.format(value.toDouble())
        } catch (_: Exception) {
            value.toEngineeringString()
        }

        val intWords = toInternationalWords(intVal)
        val saWords = toSouthAsianWords(intVal)

        return LargeNumberBreakdown(
            original = value.toPlainString(),
            internationalNotation = formattedInt,
            southAsianNotation = formattedSA,
            internationalWords = intWords,
            southAsianWords = saWords,
            scientificNotation = sciStr
        )
    }

    private val belowTwenty = arrayOf(
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
        "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    )
    private val tens = arrayOf("", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety")

    private fun helperHundreds(n: Long): String {
        var num = n
        var str = ""
        if (num >= 100) {
            str += belowTwenty[(num / 100).toInt()] + " Hundred "
            num %= 100
        }
        if (num >= 20) {
            str += tens[(num / 10).toInt()] + " "
            num %= 10
        }
        if (num > 0) {
            str += belowTwenty[num.toInt()] + " "
        }
        return str.trim()
    }

    fun toInternationalWords(num: java.math.BigInteger): String {
        if (num == java.math.BigInteger.ZERO) return "Zero"
        var n = num.abs()
        val units = arrayOf("", "Thousand", "Million", "Billion", "Trillion", "Quadrillion")
        var unitIndex = 0
        var words = ""

        val thousand = java.math.BigInteger.valueOf(1000)
        while (n > java.math.BigInteger.ZERO && unitIndex < units.size) {
            val rem = n.remainder(thousand).toLong()
            if (rem > 0) {
                val chunk = helperHundreds(rem)
                val unit = units[unitIndex]
                words = if (unit.isEmpty()) "$chunk $words" else "$chunk $unit $words"
            }
            n = n.divide(thousand)
            unitIndex++
        }
        val prefix = if (num < java.math.BigInteger.ZERO) "Negative " else ""
        return (prefix + words).trim()
    }

    fun toSouthAsianWords(num: java.math.BigInteger): String {
        if (num == java.math.BigInteger.ZERO) return "Zero"
        var n = num.abs()
        val prefix = if (num < java.math.BigInteger.ZERO) "Negative " else ""

        // South Asian grouping:
        // Last 3 digits (Hundreds)
        // Next 2 digits (Thousand)
        // Next 2 digits (Lakh)
        // Next 2 digits (Crore)
        // Next 2 digits (Arab)
        // Next 2 digits (Kharab)
        val units = arrayOf(
            Pair(java.math.BigInteger.valueOf(1000), ""),
            Pair(java.math.BigInteger.valueOf(100), "Thousand"),
            Pair(java.math.BigInteger.valueOf(100), "Lakh"),
            Pair(java.math.BigInteger.valueOf(100), "Crore"),
            Pair(java.math.BigInteger.valueOf(100), "Arab"),
            Pair(java.math.BigInteger.valueOf(100), "Kharab")
        )

        var words = ""
        for (i in units.indices) {
            if (n == java.math.BigInteger.ZERO) break
            val divisor = units[i].first
            val unitName = units[i].second
            val rem = n.remainder(divisor).toLong()
            if (rem > 0) {
                val chunk = helperHundreds(rem)
                words = if (unitName.isEmpty()) "$chunk $words" else "$chunk $unitName $words"
            }
            n = n.divide(divisor)
        }

        // If still remaining (more than 100 Kharab, express as Crores of Crores or higher)
        if (n > java.math.BigInteger.ZERO) {
            val chunk = helperHundreds(n.toLong())
            words = "$chunk Nil $words"
        }

        return (prefix + words).trim()
    }
}
