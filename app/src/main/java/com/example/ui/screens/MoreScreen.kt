package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppThemeColor
import com.example.data.local.HistoryEntity
import com.example.engine.NumberFormatStyle
import com.example.ui.CalculatorViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class MoreSubTab {
    HISTORY,
    FORMULAS,
    MANUAL,
    SETTINGS,
    ABOUT
}

@Composable
fun MoreScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier,
    initialSubTab: MoreSubTab = MoreSubTab.HISTORY,
    onNavigateToBusiness: (BusinessTab) -> Unit
) {
    var activeSubTab by remember { mutableStateOf(initialSubTab) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NexoraMainBg)
    ) {
        // TOP SUB-TAB ROW
        ScrollableTabRow(
            selectedTabIndex = activeSubTab.ordinal,
            containerColor = NexoraSecondaryBg,
            contentColor = NexoraAccentBlue,
            edgePadding = 12.dp,
            divider = { Divider(color = NexoraBorder, thickness = 1.dp) }
        ) {
            Tab(
                selected = activeSubTab == MoreSubTab.HISTORY,
                onClick = { activeSubTab = MoreSubTab.HISTORY },
                text = { Text("History", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                selectedContentColor = NexoraAccentBlue,
                unselectedContentColor = NexoraSecondaryText
            )
            Tab(
                selected = activeSubTab == MoreSubTab.FORMULAS,
                onClick = { activeSubTab = MoreSubTab.FORMULAS },
                text = { Text("Formulas", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                selectedContentColor = NexoraAccentBlue,
                unselectedContentColor = NexoraSecondaryText
            )
            Tab(
                selected = activeSubTab == MoreSubTab.MANUAL,
                onClick = { activeSubTab = MoreSubTab.MANUAL },
                text = { Text("User Manual (ম্যানুয়েল)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                selectedContentColor = NexoraAccentBlue,
                unselectedContentColor = NexoraSecondaryText
            )
            Tab(
                selected = activeSubTab == MoreSubTab.SETTINGS,
                onClick = { activeSubTab = MoreSubTab.SETTINGS },
                text = { Text("Settings", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                selectedContentColor = NexoraAccentBlue,
                unselectedContentColor = NexoraSecondaryText
            )
            Tab(
                selected = activeSubTab == MoreSubTab.ABOUT,
                onClick = { activeSubTab = MoreSubTab.ABOUT },
                text = { Text("About", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                selectedContentColor = NexoraAccentBlue,
                unselectedContentColor = NexoraSecondaryText
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            when (activeSubTab) {
                MoreSubTab.HISTORY -> HistorySection(viewModel)
                MoreSubTab.FORMULAS -> FormulasSection(onNavigateToBusiness)
                MoreSubTab.MANUAL -> UserManualSection()
                MoreSubTab.SETTINGS -> SettingsSection(viewModel)
                MoreSubTab.ABOUT -> AboutSection()
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// 1. CALCULATION HISTORY
// -----------------------------------------------------------------------------------------
@Composable
private fun HistorySection(viewModel: CalculatorViewModel) {
    val historyList by viewModel.allHistory.collectAsState()
    var filterCategory by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CALCULATION HISTORY",
                color = NexoraAccentBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            if (historyList.isNotEmpty()) {
                TextButton(
                    onClick = { viewModel.clearAllHistory() },
                    colors = ButtonDefaults.textButtonColors(contentColor = NexoraDanger)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear All", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.History, contentDescription = null, tint = NexoraMutedText, modifier = Modifier.size(48.dp))
                    Text("No calculations yet", color = NexoraMutedText, fontSize = 15.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(historyList, key = { it.id }) { item ->
                    val dateStr = remember(item.timestamp) {
                        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(item.timestamp))
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NexoraSecondaryBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.reuseHistory(item) },
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "${item.expression} =",
                                    color = NexoraSecondaryText,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.result,
                                    color = NexoraMainText,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "$dateStr • ${item.category}",
                                    color = NexoraMutedText,
                                    fontSize = 10.sp
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = { viewModel.copyToClipboard("${item.expression} = ${item.result}") },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = NexoraSecondaryText, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { viewModel.deleteHistory(item.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Delete", tint = NexoraMutedText, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// 2. FORMULAS SECTION
// -----------------------------------------------------------------------------------------
data class FormulaTemplate(
    val title: String,
    val category: String,
    val formula: String,
    val description: String,
    val targetTab: BusinessTab
)

@Composable
private fun FormulasSection(onNavigateToBusiness: (BusinessTab) -> Unit) {
    val templates = remember {
        listOf(
            FormulaTemplate(
                title = "Total Landed Cost (Import)",
                category = "Import Business",
                formula = "Landed = Product Cost + Shipping + Customs Duty + VAT + Other Fees",
                description = "Complete calculation from foreign currency (RMB/USD) to landed warehouse cost in BDT.",
                targetTab = BusinessTab.IMPORT
            ),
            FormulaTemplate(
                title = "Retail Selling Price",
                category = "Pricing & Margin",
                formula = "Retail Price = Landed Cost × (1 + Retail Margin %)",
                description = "Computes final retail shelf price given target profit margin on landed cost.",
                targetTab = BusinessTab.IMPORT
            ),
            FormulaTemplate(
                title = "Currency Spread Profit",
                category = "Foreign Exchange",
                formula = "Net Profit = (Selling Revenue - Buying Cost) - Transaction Fees",
                description = "Calculate revenue from trading foreign currency spreads with banking deductions.",
                targetTab = BusinessTab.EXCHANGE
            ),
            FormulaTemplate(
                title = "Compound Interest",
                category = "Banking & Finance",
                formula = "A = P × (1 + r/n)^(nt)",
                description = "Maturity value where interest earns interest over specified compounding frequencies.",
                targetTab = BusinessTab.BUSINESS_MATH
            ),
            FormulaTemplate(
                title = "Monthly Loan EMI",
                category = "Loans",
                formula = "EMI = [P × r × (1+r)^n] / [(1+r)^n - 1]",
                description = "Equal Monthly Installment for bank loans, car financing, or commercial mortgages.",
                targetTab = BusinessTab.BUSINESS_MATH
            ),
            FormulaTemplate(
                title = "Investment ROI & CAGR",
                category = "Investments",
                formula = "ROI = (Gain / Investment) × 100% | CAGR = (Final/Initial)^(1/t) - 1",
                description = "Calculates total percentage return and annual compounding growth rate.",
                targetTab = BusinessTab.BUSINESS_MATH
            )
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "READY-MADE BUSINESS FORMULAS",
                color = NexoraAccentBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(templates) { template ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = NexoraSecondaryBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToBusiness(template.targetTab) }
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = template.title, color = NexoraMainText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NexoraPrimaryOp.copy(alpha = 0.4f)
                        ) {
                            Text(
                                text = template.category,
                                color = NexoraAccentBlue,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = template.formula,
                        color = NexoraAccentBlue,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = template.description,
                            color = NexoraSecondaryText,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = NexoraSecondaryText)
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// 3. USER MANUAL (ইউজার ম্যানুয়েল) - BILINGUAL (BANGLA & ENGLISH)
// -----------------------------------------------------------------------------------------
@Composable
private fun UserManualSection() {
    var isBangla by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // LANGUAGE SWITCHER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isBangla) "ইউজার ম্যানুয়েল (ব্যবহার সহায়িকা)" else "USER MANUAL & GUIDE",
                color = NexoraAccentBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = isBangla,
                    onClick = { isBangla = true },
                    label = { Text("🇧🇩 বাংলা", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NexoraPrimaryOp,
                        selectedLabelColor = NexoraMainText
                    )
                )
                FilterChip(
                    selected = !isBangla,
                    onClick = { isBangla = false },
                    label = { Text("🇬🇧 English", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NexoraPrimaryOp,
                        selectedLabelColor = NexoraMainText
                    )
                )
            }
        }

        if (isBangla) {
            // BANGLA MANUAL
            ManualTopicCard(
                icon = Icons.Default.Calculate,
                title = "১. বেসিক ক্যালকুলেটর ও শেয়ার/স্ক্রিনশট",
                content = """
                    • যোগ (+), বিয়োগ (-), গুণ (×), ভাগ (÷) এবং পার্সেন্টেজ (%) হিসাব তাত্ক্ষণিক ও নির্ভুলভাবে করুন।
                    • অংক লেখার সাথে সাথেই রিয়েল-টাইমে রেজাল্ট প্রিভিউ দেখতে পাবেন।
                    • পূর্বের ৩টি হিসাব সরাসরি স্ক্রিনের উপরে সবসময় দৃশ্যমান থাকে।
                    • হিসাবের রসিদ ও স্ক্রিনশট: এক্সপ্রেশন লাইনের পাশে থাকা 'Camera' (স্ক্রিনশট) আইকনে চাপ দিলেই বর্তমান অংকের একটি পরিচ্ছন্ন ছবি (PNG) সরাসরি আপনার ফোনের গ্যালারিতে (Pictures/DCalc) সেভ হয়ে যাবে।
                    • শেয়ার করা: 'Share' আইকনে চাপ দিলে ডিজিটাল রসিদ ওপেন হবে, যেখান থেকে 'Share Image' (ছবি হিসেবে শেয়ার), 'Share Text' অথবা 'Screenshot' নিয়ে WhatsApp/Messenger-এ পাঠাতে পারবেন।
                """.trimIndent()
            )

            ManualTopicCard(
                icon = Icons.Default.MenuBook,
                title = "২. লেজার / হিসাব খাতা (Ledger System)",
                content = """
                    • 'New Book' বাটনে চাপ দিয়ে দোকান বা ব্যক্তির নাম (যেমন: 'আকবরের দোকান') দিয়ে খাতা খুলুন।
                    • খাতার ভেতরে যেয়ে '+ Add Item' বাটনে চাপুন।
                    • পণ্যের নাম ও দাম লিখুন (যেমন: সিগারেট ৩৫ টাকা, আইসক্রিম ৫০ টাকা, সাবান ৫৫ টাকা)।
                    • বর্তমান তারিখ ও সময় স্বয়ংক্রিয়ভাবে প্রতিটি এন্ট্রিতে রেকর্ড হয়ে যাবে।
                    • মোট বকেয়া টাকা উপরে বড় অক্ষরে প্রদর্শিত হবে।
                    • 'Clear Account' বাটনে চাপ দিয়ে পুরো বাকি পরিশোধ (Settle) করা যাবে এবং অতীতের পরিশোধের রেকর্ড হিস্ট্রিতে দেখা যাবে।
                    • উপরে থাকা শেয়ার আইকনে চাপ দিলে সম্পূর্ণ খাতার বিবরণী কপি বা শেয়ার করা যায়।
                """.trimIndent()
            )

            ManualTopicCard(
                icon = Icons.Default.TrendingUp,
                title = "৩. স্মার্ট বিজনেস ও ডেটা সেভ (Smart Business)",
                content = """
                    • ইমপোর্ট ল্যান্ডেড কস্ট: চীন বা বিদেশ থেকে পণ্য আমদানির ক্ষেত্রে RMB বা USD রেট, শিপিং চার্জ, ওজন (KG), কাস্টমস শুল্ক ও ভ্যাট বসিয়ে মোট ল্যান্ডেড কস্ট ও খুচরা বিক্রয় মূল্য বের করুন।
                    • ডেটা সেভ করা: হিসাব করার পর উপরে থাকা 'Save' বাটনে চাপ দিন। যেকোনো নাম (যেমন: 'চায়না ব্যাগ ১০০ পিস') দিয়ে সেভ করে রাখুন।
                    • ডেটা রিস্টোর করা: 'Saved' বাটনে চাপ দিলে আপনার সেভ করা আগের সব হিসাব দেখতে পাবেন। 'Restore Data' চাপলেই সব মান স্বয়ংক্রিয়ভাবে ইনপুটে বসে যাবে।
                    • এছাড়াও মুদ্রা স্প্রেড লাভ, ব্যাংক লোন EMI, লাভ-ক্ষতি, পার্টনারশিপ বন্টন ও ROI হিসাব করতে পারবেন।
                """.trimIndent()
            )

            ManualTopicCard(
                icon = Icons.Default.CurrencyExchange,
                title = "৪. মুদ্রা রূপান্তরকারী (Currency Converter)",
                content = """
                    • ১৮০টির বেশি দেশের মুদ্রার তাৎক্ষণিক রূপান্তর।
                    • ইন্টারনেট কানেকশন থাকলে রেট অটোমেটিক আপডেট হবে, ইন্টারনেট না থাকলেও অফলাইন ক্যাশ রেটে কাজ করবে।
                    • 'Custom Direct Rate' অপশন চালু করে আপনি নিজের পছন্দমতো রেট দিয়ে হিসাব করতে পারবেন (যেমন: ১ RMB = ১৭.৫০ BDT)।
                """.trimIndent()
            )

            ManualTopicCard(
                icon = Icons.Default.PhoneIphone,
                title = "৫. iPhone / iOS ব্যবহারকারীদের জন্য নির্দেশিকা",
                content = """
                    • এটি একটি নেটিভ অ্যান্ড্রয়েড (.apk) অ্যাপ, তাই iPhone-এ সরাসরি APK ফাইল ইনস্টল হয় না।
                    • তবে iPhone ব্যবহারকারীরা Safari বা যেকোনো ব্রাউজারে Google AI Studio-র Shared App লিংক ওপেন করে স্ট্রিমিং প্রিভিউ-এর মাধ্যমে পুরো অ্যাপটি ব্যবহার করতে পারবেন:
                      https://ais-pre-cvdf4omybpgy2q7z4t2547-820014097826.asia-northeast1.run.app
                """.trimIndent()
            )

            ManualTopicCard(
                icon = Icons.Default.Android,
                title = "৬. Android APK ডাউনলোড ও ইনস্টল নির্দেশিকা",
                content = """
                    • Google AI Studio-র উপরের ডানপাশের Settings / Export মেনুতে ক্লিক করে 'Generate APK' বা 'Download APK' সিলেক্ট করে সরাসরি ফোনে ডাউনলোড করুন।
                    • প্রজেক্ট কোড ডাউনলোড করলে Android Studio দিয়েও 'app/build/outputs/apk/debug/app-debug.apk' বিল্ড করতে পারবেন।
                    • ফোনে ইনস্টল করার সময় 'Allow from this source' চালু করে Install চাপুন।
                """.trimIndent()
            )
        } else {
            // ENGLISH MANUAL
            ManualTopicCard(
                icon = Icons.Default.Calculate,
                title = "1. Basic Calculator & Share / Screenshot",
                content = """
                    • High-precision arithmetic (+, -, ×, ÷, %) with exact decimal calculations.
                    • Live preview result displays dynamically as you type expressions.
                    • Recent 3 calculations remain visible at the top without scrolling.
                    • Calculation Slip & Screenshot: Tap the 'Camera' icon on the display bar to instantly save a clean PNG screenshot of the calculation to Pictures/DCalc.
                    • Share Slip: Tap the 'Share' icon to preview the receipt slip and choose 'Screenshot', 'Share Image' (PNG), 'Share Text', or 'Copy Text'.
                """.trimIndent()
            )

            ManualTopicCard(
                icon = Icons.Default.MenuBook,
                title = "2. Ledger & Account Book System",
                content = """
                    • Tap 'New Book' to create an account for any individual or store (e.g., 'Akbar\'s Shop').
                    • Inside the book, tap '+ Add Item' to record items with names and prices (e.g., Cigarettes 35 Taka, Ice cream 50 Taka, Soap 55 Taka).
                    • Current date and time are automatically stamped on each entry.
                    • Total outstanding due is prominently displayed at the top.
                    • Tap 'Clear Account' to settle the balance to ৳ 0.00 and maintain a permanent audit log of past settlements.
                    • Tap the share button to copy a complete formatted account summary to clipboard.
                """.trimIndent()
            )

            ManualTopicCard(
                icon = Icons.Default.TrendingUp,
                title = "3. Smart Business & Calculation Saving",
                content = """
                    • Import Landed Cost: Factor in foreign currency (RMB/USD), per-KG air/sea shipping, customs duties, VAT, and retail margin to determine exact unit landed cost.
                    • Saving Calculations: After entering parameters, tap 'Save' at the top to store the record with a custom title (e.g., 'Guangzhou Shoes 200pcs').
                    • Restoring Calculations: Tap 'Saved' to open your library of calculations; tap 'Restore Data' to instantly populate all inputs.
                    • Includes Loan EMI schedule, Profit & Loss analysis, Currency Spread, and Investment ROI tools.
                """.trimIndent()
            )

            ManualTopicCard(
                icon = Icons.Default.CurrencyExchange,
                title = "4. Currency Converter",
                content = """
                    • Real-time rates for over 180 currencies worldwide.
                    • Automatic synchronization with online servers and full offline caching.
                    • Toggle 'Custom Direct Rate' to set your personal currency exchange rate (e.g., 1 RMB = 17.50 BDT).
                """.trimIndent()
            )

            ManualTopicCard(
                icon = Icons.Default.PhoneIphone,
                title = "5. Guide for iPhone & iOS Users",
                content = """
                    • D. Calc is built as a native Android application (.apk), so .apk files cannot be installed directly on iPhone.
                    • iPhone / iOS users can still use the full app directly through Safari using the AI Studio Shared App URL:
                      https://ais-pre-cvdf4omybpgy2q7z4t2547-820014097826.asia-northeast1.run.app
                """.trimIndent()
            )

            ManualTopicCard(
                icon = Icons.Default.Android,
                title = "6. Android APK Download & Installation",
                content = """
                    • In AI Studio, open the top-right Settings / Export menu and choose 'Generate APK' or 'Download APK' to save the installable APK file.
                    • When installing on your Android phone, enable 'Allow from this source' in your device settings if prompted.
                """.trimIndent()
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun ManualTopicCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = NexoraSecondaryBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NexoraPrimaryOp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = NexoraAccentBlue, modifier = Modifier.size(20.dp))
                }
                Text(text = title, color = NexoraMainText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            HorizontalDivider(color = NexoraBorder.copy(alpha = 0.4f), thickness = 1.dp)

            Text(
                text = content,
                color = NexoraSecondaryText,
                fontSize = 12.5.sp,
                lineHeight = 19.sp
            )
        }
    }
}

// -----------------------------------------------------------------------------------------
// 4. SETTINGS SECTION (INCLUDING THEME COLOR SELECTOR)
// -----------------------------------------------------------------------------------------
@Composable
private fun SettingsSection(viewModel: CalculatorViewModel) {
    val settings by viewModel.userSettings.collectAsState()
    val prefs = viewModel.userPreferences

    var rmbBdtInput by remember(settings.customRmbToBdt) { mutableStateOf(settings.customRmbToBdt.toString()) }
    var usdBdtInput by remember(settings.customUsdToBdt) { mutableStateOf(settings.customUsdToBdt.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("CALCULATOR PREFERENCES", color = NexoraAccentBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)

        // THEME COLOR SELECTOR (Requirement 5)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = NexoraSecondaryBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Theme Color (থিম কালার)", color = NexoraMainText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Selected: ${AppThemeColor.valueOf(settings.themeColor).displayName}", color = NexoraAccentBlue, fontSize = 11.sp)
                    }
                    Icon(Icons.Default.Palette, contentDescription = null, tint = NexoraAccentBlue, modifier = Modifier.size(20.dp))
                }

                // Theme Swatches
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppThemeColor.entries.chunked(2).forEach { rowThemes ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowThemes.forEach { theme ->
                                val isSelected = settings.themeColor == theme.name
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) NexoraPrimaryOp else NexoraCalcButton,
                                    border = androidx.compose.foundation.BorderStroke(
                                        if (isSelected) 1.5.dp else 1.dp,
                                        if (isSelected) Color(theme.hexCode) else NexoraBorder
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            viewModel.triggerHaptic()
                                            prefs.setThemeColor(theme)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(Color(theme.hexCode))
                                        )
                                        Column {
                                            Text(
                                                text = theme.displayName,
                                                color = if (isSelected) NexoraMainText else NexoraSecondaryText,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                            Text(
                                                text = theme.banglaName,
                                                color = NexoraMutedText,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // NUMBER NOTATION STYLE
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = NexoraSecondaryBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Number Grouping & Format", color = NexoraMainText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = settings.formatStyle == NumberFormatStyle.SOUTH_ASIAN,
                        onClick = { prefs.setFormatStyle(NumberFormatStyle.SOUTH_ASIAN) },
                        label = { Text("South Asian (Lakh, Crore)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NexoraPrimaryOp,
                            selectedLabelColor = NexoraMainText
                        )
                    )
                    FilterChip(
                        selected = settings.formatStyle == NumberFormatStyle.INTERNATIONAL,
                        onClick = { prefs.setFormatStyle(NumberFormatStyle.INTERNATIONAL) },
                        label = { Text("International (Million, Billion)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NexoraPrimaryOp,
                            selectedLabelColor = NexoraMainText
                        )
                    )
                }
            }
        }

        // DECIMAL PRECISION
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = NexoraSecondaryBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Decimal Display Precision: ${settings.decimalPrecision} places", color = NexoraMainText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(2, 4, 6, 8, 10).forEach { prec ->
                        FilterChip(
                            selected = settings.decimalPrecision == prec,
                            onClick = { prefs.setDecimalPrecision(prec) },
                            label = { Text("$prec", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NexoraPrimaryOp,
                                selectedLabelColor = NexoraMainText
                            )
                        )
                    }
                }
            }
        }

        // DEFAULT EXCHANGE RATES
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = NexoraSecondaryBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Default Direct Exchange Rates (BDT)", color = NexoraMainText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rmbBdtInput,
                        onValueChange = {
                            rmbBdtInput = it
                            it.toDoubleOrNull()?.let { rate -> prefs.setCustomRmbToBdt(rate) }
                        },
                        label = { Text("1 RMB = BDT") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NexoraAccentBlue,
                            unfocusedBorderColor = NexoraBorder,
                            focusedTextColor = NexoraMainText,
                            unfocusedTextColor = NexoraMainText
                        )
                    )

                    OutlinedTextField(
                        value = usdBdtInput,
                        onValueChange = {
                            usdBdtInput = it
                            it.toDoubleOrNull()?.let { rate -> prefs.setCustomUsdToBdt(rate) }
                        },
                        label = { Text("1 USD = BDT") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NexoraAccentBlue,
                            unfocusedBorderColor = NexoraBorder,
                            focusedTextColor = NexoraMainText,
                            unfocusedTextColor = NexoraMainText
                        )
                    )
                }
            }
        }

        // HAPTIC FEEDBACK
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = NexoraSecondaryBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Haptic Feedback", color = NexoraMainText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Subtle mechanical vibration on key tap", color = NexoraMutedText, fontSize = 11.sp)
                    }
                    Switch(
                        checked = settings.hapticEnabled,
                        onCheckedChange = { prefs.setHapticEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NexoraMainBg,
                            checkedTrackColor = NexoraAccentBlue,
                            uncheckedThumbColor = NexoraMutedText,
                            uncheckedTrackColor = NexoraCalcButton
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// -----------------------------------------------------------------------------------------
// 5. ABOUT SECTION
// -----------------------------------------------------------------------------------------
@Composable
private fun AboutSection() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = NexoraSecondaryBg,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, NexoraAccentBlue.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(NexoraPrimaryOp)
                        .border(1.5.dp, NexoraAccentBlue, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = null,
                        tint = NexoraAccentBlue,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = "D. CALC",
                    color = NexoraMainText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Text(
                    text = "Version 1.0.0 (Native Android & Web)",
                    color = NexoraAccentBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Divider(color = NexoraBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = "A high-precision, lightweight calculator crafted for professionals, import businesses, shop ledgers, and everyday mathematics. Built in Kotlin & Jetpack Compose with local database persistence.",
                    color = NexoraSecondaryText,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = NexoraSecondaryBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Core Engineering Pillars", color = NexoraMainText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                PillarRow("⚡ Deterministic Local Math", "100% offline calculations using BigDecimal precision.")
                PillarRow("📖 Integrated Ledger System", "Track accounts, item debts, dates, and balance settlements.")
                PillarRow("💾 Business Calculations Saved", "Store and restore complete import and landed cost datasets.")
                PillarRow("🎨 Dynamic 6-Theme Engine", "Tailored dark palettes from Midnight Navy to Obsidian AMOLED.")
                PillarRow("📱 Cross-Platform Ready", "Native Android APK and PWA support for iPhone/iOS users.")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun PillarRow(title: String, desc: String) {
    Column {
        Text(text = title, color = NexoraAccentBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(text = desc, color = NexoraSecondaryText, fontSize = 12.sp)
    }
}
