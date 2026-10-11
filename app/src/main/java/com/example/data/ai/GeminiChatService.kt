package com.example.data.ai

import com.example.BuildConfig
import com.example.data.model.BusinessProfile
import com.example.data.model.Party
import com.example.data.model.TransactionEntry
import com.example.data.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class GeminiChatService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun askBusinessAdvisor(
        userMessage: String,
        profile: BusinessProfile,
        parties: List<Party>,
        transactions: List<TransactionEntry>,
        history: List<ChatMessage>
    ): String = withContext(Dispatchers.IO) {
        val currency = profile.currencySymbol.ifBlank { "₹" }
        val totalGet = parties.filter { it.balance > 0 }.sumOf { it.balance }
        val totalGive = parties.filter { it.balance < 0 }.sumOf { kotlin.math.abs(it.balance) }
        val cashIn = transactions.filter { it.type == TransactionType.CASH_IN }.sumOf { it.amount }
        val cashOut = transactions.filter { it.type == TransactionType.CASH_OUT }.sumOf { it.amount }
        val topDebtors = parties.filter { it.balance > 0 }
            .sortedByDescending { it.balance }
            .take(5)
            .joinToString(", ") { "${it.name} ($currency${formatNum(it.balance)})" }
        val topCreditors = parties.filter { it.balance < 0 }
            .sortedBy { it.balance }
            .take(5)
            .joinToString(", ") { "${it.name} ($currency${formatNum(kotlin.math.abs(it.balance))})" }

        val systemContext = """
            You are Kirio AI Munshi, an expert Indian business ledger & financial advisor embedded inside the Kirio Khata app.
            Current Business Snapshot for "${profile.businessName}":
            - Total Parties: ${parties.size} (${parties.count { it.type.name == "CUSTOMER" }} Customers, ${parties.count { it.type.name == "SUPPLIER" }} Suppliers)
            - Total You Will Get (Receivables): $currency${formatNum(totalGet)}
            - Total You Will Give (Payables): $currency${formatNum(totalGive)}
            - Net Ledger Position: $currency${formatNum(totalGet - totalGive)}
            - Cashbook Inflow: $currency${formatNum(cashIn)} | Outflow: $currency${formatNum(cashOut)} | Net Cash: $currency${formatNum(cashIn - cashOut)}
            - Top Pending Receivables: ${topDebtors.ifBlank { "None" }}
            - Top Pending Payables: ${topCreditors.ifBlank { "None" }}
            
            Keep answers concise, practical, encouraging, and directly tailored to small business bookkeeping, collection reminders, cashflow optimization, and GST/invoice tips.
        """.trimIndent()

        val apiKey = runCatching { BuildConfig.GEMINI_API_KEY }.getOrDefault("")
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "your_api_key_here") {
            return@withContext generateLocalSmartResponse(
                userMessage = userMessage,
                currency = currency,
                businessName = profile.businessName,
                parties = parties,
                transactions = transactions,
                totalGet = totalGet,
                totalGive = totalGive,
                cashIn = cashIn,
                cashOut = cashOut
            )
        }

        try {
            val contentsArray = JSONArray()
            history.takeLast(6).forEach { msg ->
                val partObj = JSONObject().put("text", msg.text)
                val partsArr = JSONArray().put(partObj)
                val role = if (msg.isFromUser) "user" else "model"
                contentsArray.put(JSONObject().put("role", role).put("parts", partsArr))
            }
            contentsArray.put(
                JSONObject()
                    .put("role", "user")
                    .put("parts", JSONArray().put(JSONObject().put("text", userMessage)))
            )

            val payload = JSONObject().apply {
                put(
                    "systemInstruction",
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", systemContext))
                    )
                )
                put("contents", contentsArray)
                put(
                    "generationConfig",
                    JSONObject().put("temperature", 0.6)
                )
            }

            val url =
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext generateLocalSmartResponse(
                        userMessage = userMessage,
                        currency = currency,
                        businessName = profile.businessName,
                        parties = parties,
                        transactions = transactions,
                        totalGet = totalGet,
                        totalGive = totalGive,
                        cashIn = cashIn,
                        cashOut = cashOut
                    )
                }
                val json = JSONObject(bodyStr)
                val text = json.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")
                if (!text.isNullOrBlank()) {
                    text.trim()
                } else {
                    generateLocalSmartResponse(
                        userMessage = userMessage,
                        currency = currency,
                        businessName = profile.businessName,
                        parties = parties,
                        transactions = transactions,
                        totalGet = totalGet,
                        totalGive = totalGive,
                        cashIn = cashIn,
                        cashOut = cashOut
                    )
                }
            }
        } catch (e: Exception) {
            generateLocalSmartResponse(
                userMessage = userMessage,
                currency = currency,
                businessName = profile.businessName,
                parties = parties,
                transactions = transactions,
                totalGet = totalGet,
                totalGive = totalGive,
                cashIn = cashIn,
                cashOut = cashOut
            )
        }
    }

    private fun generateLocalSmartResponse(
        userMessage: String,
        currency: String,
        businessName: String,
        parties: List<Party>,
        transactions: List<TransactionEntry>,
        totalGet: Double,
        totalGive: Double,
        cashIn: Double,
        cashOut: Double
    ): String {
        val q = userMessage.lowercase(Locale.getDefault())
        val debtors = parties.filter { it.balance > 0 }.sortedByDescending { it.balance }
        val creditors = parties.filter { it.balance < 0 }.sortedBy { it.balance }

        return when {
            q.contains("owe") || q.contains("pending") || q.contains("collect") || q.contains("get") || q.contains("baaki") -> {
                if (debtors.isEmpty()) {
                    "Great news for **$businessName**! You currently have **0 pending receivables**. All customer dues are settled."
                } else {
                    val list = debtors.take(5).joinToString("\n") {
                        "• **${it.name}**: $currency${formatNum(it.balance)}" +
                            if (it.phone.isNotBlank()) " (${it.phone})" else ""
                    }
                    "Here are your top pending collections (**Total You Will Get: $currency${formatNum(totalGet)}**):\n\n$list\n\n💡 *Tip: Tap on any customer and press the WhatsApp Reminder button to send a polite payment link with your UPI QR.*"
                }
            }
            q.contains("pay") || q.contains("supplier") || q.contains("give") -> {
                if (creditors.isEmpty()) {
                    "You have **no pending supplier dues** right now (**You Will Give: ${currency}0**)."
                } else {
                    val list = creditors.take(5).joinToString("\n") {
                        "• **${it.name}**: $currency${formatNum(kotlin.math.abs(it.balance))}"
                    }
                    "Your upcoming payables (**Total You Will Give: $currency${formatNum(totalGive)}**):\n\n$list"
                }
            }
            q.contains("cash") || q.contains("today") || q.contains("profit") || q.contains("summary") || q.contains("report") -> {
                val netCash = cashIn - cashOut
                val netLedger = totalGet - totalGive
                """
                📊 **Ledger & Cashflow Summary for $businessName**
                
                • **You Will Get (Receivables):** $currency${formatNum(totalGet)} (${debtors.size} parties)
                • **You Will Give (Payables):** $currency${formatNum(totalGive)} (${creditors.size} parties)
                • **Net Khata Balance:** $currency${formatNum(netLedger)}
                
                💵 **Cashbook Status:**
                • **Total Cash In:** $currency${formatNum(cashIn)}
                • **Total Cash Out:** $currency${formatNum(cashOut)}
                • **Cash in Hand:** $currency${formatNum(netCash)}
                
                Total recorded transactions: **${transactions.size}**.
                """.trimIndent()
            }
            q.contains("remind") || q.contains("whatsapp") || q.contains("message") -> {
                val top = debtors.firstOrNull()
                if (top != null) {
                    "Here is a ready-to-send collection reminder for **${top.name}**:\n\n" +
                        "\"Namaste ${top.name} ji, a friendly reminder from *$businessName* that an amount of *$currency${formatNum(top.balance)}* is pending in your Khata account. Kindly settle at your convenience via UPI or Cash. Thank you!\""
                } else {
                    "Here is a general payment reminder template:\n\n\"Namaste! Friendly reminder from *$businessName* regarding your pending Khata balance. Kindly clear the dues via UPI at your earliest convenience. Thank you for your business!\""
                }
            }
            else -> {
                """
                Namaste! I am your **Kirio AI Munshi** for **$businessName**.
                
                Here is your quick business snapshot:
                • **Receivables (You Will Get):** $currency${formatNum(totalGet)}
                • **Payables (You Will Give):** $currency${formatNum(totalGive)}
                • **Cashbook Net Balance:** $currency${formatNum(cashIn - cashOut)}
                • **Active Parties:** ${parties.size}
                
                Ask me things like:
                1. *"Who owes me the most money?"*
                2. *"Give me a monthly summary report"*
                3. *"Draft a WhatsApp payment reminder"*
                4. *"What is my supplier payable balance?"*
                """.trimIndent()
            }
        }
    }

    private fun formatNum(amount: Double): String {
        val fmt = NumberFormat.getNumberInstance(Locale("en", "IN"))
        fmt.maximumFractionDigits = 0
        return fmt.format(amount)
    }
}
