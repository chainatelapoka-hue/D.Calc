package com.example.data.history

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Builds a 1-year rate history using weekly snapshots (52 points) from the
 * free, key-less fawazahmed0/currency-api on jsDelivr.
 * Each snapshot is one request; failed snapshots are skipped.
 */
object RateHistoryFetcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private const val WEEKS = 52

    /** Returns (epochMillis, rate of 1 [from] in [to]) sorted oldest first. */
    suspend fun fetchOneYear(from: String, to: String): List<Pair<Long, Double>> = withContext(Dispatchers.IO) {
        val now = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val points = coroutineScope {
            (0 until WEEKS).map { week ->
                async {
                    val cal = now.clone() as Calendar
                    cal.add(Calendar.DAY_OF_YEAR, -week * 7)
                    val dateTag = if (week == 0) "latest" else dateFormat.format(cal.time)
                    val rate = try {
                        fetchRate(dateTag, from, to)
                    } catch (_: Exception) {
                        null
                    }
                    rate?.let { cal.timeInMillis to it }
                }
            }.awaitAll().filterNotNull()
        }
        points.sortedBy { it.first }
    }

    private fun fetchRate(dateTag: String, from: String, to: String): Double? {
        val base = from.lowercase(Locale.US)
        val target = to.lowercase(Locale.US)
        val url = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@$dateTag/v1/currencies/$base.json"
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val rates = JSONObject(body).getJSONObject(base)
            return if (rates.has(target)) rates.getDouble(target) else null
        }
    }
}
