package com.example.data.model

data class CurrencyItem(
    val code: String,
    val name: String,
    val flagEmoji: String,
    val symbol: String
)

object CurrencyCatalog {
    val ALL_CURRENCIES = listOf(
        CurrencyItem("BDT", "Bangladeshi Taka", "🇧🇩", "৳"),
        CurrencyItem("CNY", "Chinese Yuan (RMB)", "🇨🇳", "¥"),
        CurrencyItem("USD", "US Dollar", "🇺🇸", "$"),
        CurrencyItem("EUR", "Euro", "🇪🇺", "€"),
        CurrencyItem("GBP", "British Pound", "🇬🇧", "£"),
        CurrencyItem("JPY", "Japanese Yen", "🇯🇵", "¥"),
        CurrencyItem("INR", "Indian Rupee", "🇮🇳", "₹"),
        CurrencyItem("AED", "UAE Dirham", "🇦🇪", "د.إ"),
        CurrencyItem("SAR", "Saudi Riyal", "🇸🇦", "﷼"),
        CurrencyItem("SGD", "Singapore Dollar", "🇸🇬", "S$"),
        CurrencyItem("MYR", "Malaysian Ringgit", "🇲🇾", "RM"),
        CurrencyItem("THB", "Thai Baht", "🇹🇭", "฿"),
        CurrencyItem("KRW", "South Korean Won", "🇰🇷", "₩"),
        CurrencyItem("HKD", "Hong Kong Dollar", "🇭🇰", "HK$"),
        CurrencyItem("AUD", "Australian Dollar", "🇦🇺", "A$"),
        CurrencyItem("CAD", "Canadian Dollar", "🇨🇦", "C$"),
        CurrencyItem("CHF", "Swiss Franc", "🇨🇭", "CHF"),
        CurrencyItem("PKR", "Pakistani Rupee", "🇵🇰", "₨"),
        CurrencyItem("TRY", "Turkish Lira", "🇹🇷", "₺"),
        CurrencyItem("RUB", "Russian Ruble", "🇷🇺", "₽"),
        CurrencyItem("KWD", "Kuwaiti Dinar", "🇰🇼", "KD"),
        CurrencyItem("QAR", "Qatari Riyal", "🇶🇦", "QR"),
        CurrencyItem("OMR", "Omani Rial", "🇴🇲", "RO"),
        CurrencyItem("NZD", "New Zealand Dollar", "🇳🇿", "NZ$"),
        CurrencyItem("IDR", "Indonesian Rupiah", "🇮🇩", "Rp"),
        CurrencyItem("VND", "Vietnamese Dong", "🇻🇳", "₫"),
        CurrencyItem("PHP", "Philippine Peso", "🇵🇭", "₱"),
        CurrencyItem("BRL", "Brazilian Real", "🇧🇷", "R$"),
        CurrencyItem("ZAR", "South African Rand", "🇿🇦", "R")
    )

    private val mapByCode = ALL_CURRENCIES.associateBy { it.code }

    fun find(code: String): CurrencyItem {
        return mapByCode[code] ?: CurrencyItem(code, code, "🌐", code)
    }

    // Default baseline rates against USD (used only when offline before any cache)
    val BASELINE_RATES_VS_USD = mapOf(
        "USD" to 1.0,
        "BDT" to 122.50,
        "CNY" to 7.25,
        "EUR" to 0.92,
        "GBP" to 0.78,
        "JPY" to 155.0,
        "INR" to 85.0,
        "AED" to 3.67,
        "SAR" to 3.75,
        "SGD" to 1.34,
        "MYR" to 4.45,
        "THB" to 34.50,
        "KRW" to 1380.0,
        "HKD" to 7.78,
        "AUD" to 1.52,
        "CAD" to 1.39,
        "CHF" to 0.88,
        "PKR" to 278.0,
        "TRY" to 34.5,
        "RUB" to 98.0,
        "KWD" to 0.31,
        "QAR" to 3.64,
        "OMR" to 0.38,
        "NZD" to 1.68,
        "IDR" to 15800.0,
        "VND" to 25200.0,
        "PHP" to 58.5,
        "BRL" to 5.75,
        "ZAR" to 18.0
    )
}
