package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import android.widget.Toast
import org.json.JSONObject
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.BusinessMath
import com.example.engine.NumberFormatStyle
import com.example.engine.NumberFormatter
import com.example.ui.CalculatorViewModel
import com.example.ui.theme.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

enum class BusinessTab {
    IMPORT,
    EXCHANGE,
    BUSINESS_MATH,
    LARGE_NUMBER
}

@Composable
fun BusinessCalculatorScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier,
    initialTab: BusinessTab = BusinessTab.IMPORT
) {
    var selectedTab by remember { mutableStateOf(initialTab) }
    LaunchedEffect(initialTab) {
        selectedTab = initialTab
    }
    val settings by viewModel.userSettings.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NexoraMainBg)
    ) {
        // TOP TAB SELECTOR
        ScrollableTabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = NexoraSecondaryBg,
            contentColor = NexoraAccentBlue,
            edgePadding = 12.dp,
            divider = { Divider(color = NexoraBorder, thickness = 1.dp) }
        ) {
            Tab(
                selected = selectedTab == BusinessTab.IMPORT,
                onClick = { selectedTab = BusinessTab.IMPORT },
                text = { Text("Import Landed", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                selectedContentColor = NexoraAccentBlue,
                unselectedContentColor = NexoraSecondaryText
            )
            Tab(
                selected = selectedTab == BusinessTab.EXCHANGE,
                onClick = { selectedTab = BusinessTab.EXCHANGE },
                text = { Text("Currency Spread", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                selectedContentColor = NexoraAccentBlue,
                unselectedContentColor = NexoraSecondaryText
            )
            Tab(
                selected = selectedTab == BusinessTab.BUSINESS_MATH,
                onClick = { selectedTab = BusinessTab.BUSINESS_MATH },
                text = { Text("Business Math", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                selectedContentColor = NexoraAccentBlue,
                unselectedContentColor = NexoraSecondaryText
            )
            Tab(
                selected = selectedTab == BusinessTab.LARGE_NUMBER,
                onClick = { selectedTab = BusinessTab.LARGE_NUMBER },
                text = { Text("Number Words", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                selectedContentColor = NexoraAccentBlue,
                unselectedContentColor = NexoraSecondaryText
            )
        }

        // CONTENT BODY
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            when (selectedTab) {
                BusinessTab.IMPORT -> ImportCalculatorTab(viewModel, settings.formatStyle)
                BusinessTab.EXCHANGE -> ExchangeProfitTab(viewModel, settings.formatStyle)
                BusinessTab.BUSINESS_MATH -> BusinessMathTab(viewModel, settings.formatStyle)
                BusinessTab.LARGE_NUMBER -> LargeNumberTab(viewModel)
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// 1. IMPORT BUSINESS CALCULATOR
// -----------------------------------------------------------------------------------------
@Composable
private fun ImportCalculatorTab(
    viewModel: CalculatorViewModel,
    formatStyle: NumberFormatStyle
) {
    var currencyType by remember { mutableStateOf("RMB") } // RMB or USD
    var unitPriceStr by remember { mutableStateOf("45") }
    var quantityStr by remember { mutableStateOf("200") }
    var exchangeRateStr by remember { mutableStateOf("17.50") }
    var weightKgStr by remember { mutableStateOf("15") }
    var shippingRatePerKgStr by remember { mutableStateOf("750") }
    var customsDutyPctStr by remember { mutableStateOf("10") }
    var vatPctStr by remember { mutableStateOf("15") }
    var otherExpensesStr by remember { mutableStateOf("3000") }
    var wholesaleMarginPctStr by remember { mutableStateOf("20") }
    var retailMarginPctStr by remember { mutableStateOf("35") }

    val context = LocalContext.current
    val allRecords by viewModel.allBusinessRecords.collectAsState()
    val importRecords = remember(allRecords) { allRecords.filter { it.category == "IMPORT" } }

    var showSaveDialog by remember { mutableStateOf(false) }
    var showRecordsDialog by remember { mutableStateOf(false) }
    var saveTitle by remember { mutableStateOf("") }

    val result = remember(
        unitPriceStr, quantityStr, exchangeRateStr, weightKgStr, shippingRatePerKgStr,
        customsDutyPctStr, vatPctStr, otherExpensesStr, wholesaleMarginPctStr, retailMarginPctStr
    ) {
        val qty = safeBigDecimal(quantityStr, BigDecimal.ONE)
        val unitPrice = safeBigDecimal(unitPriceStr)
        val exRate = safeBigDecimal(exchangeRateStr, BigDecimal.ONE)
        val weight = safeBigDecimal(weightKgStr)
        val shipPerKg = safeBigDecimal(shippingRatePerKgStr)
        val duty = safeBigDecimal(customsDutyPctStr)
        val vat = safeBigDecimal(vatPctStr)
        val other = safeBigDecimal(otherExpensesStr)
        val wholesaleMargin = safeBigDecimal(wholesaleMarginPctStr)
        val retailMargin = safeBigDecimal(retailMarginPctStr)

        BusinessMath.calculateImportCost(
            quantity = qty,
            unitPriceForeign = unitPrice,
            exchangeRateToBdt = exRate,
            weightKg = weight,
            shippingRatePerKgBdt = shipPerKg,
            flatShippingBdt = BigDecimal.ZERO,
            customsDutyPercent = duty,
            vatPercent = vat,
            otherExpensesBdt = other,
            wholesaleMarginPercent = wholesaleMargin,
            retailMarginPercent = retailMargin
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // CURRENCY SELECTOR (RMB vs USD) & SAVE ACTIONS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = { showSaveDialog = true },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = NexoraAccentBlue,
                    contentColor = NexoraMainBg
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = { showRecordsDialog = true },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NexoraSecondaryText),
                border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Saved (${importRecords.size})", fontSize = 12.sp)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "IMPORT COST ESTIMATOR",
                color = NexoraAccentBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = currencyType == "RMB",
                    onClick = {
                        currencyType = "RMB"
                        exchangeRateStr = "17.50"
                    },
                    label = { Text("🇨🇳 RMB / CNY", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NexoraPrimaryOp,
                        selectedLabelColor = NexoraMainText
                    )
                )
                FilterChip(
                    selected = currencyType == "USD",
                    onClick = {
                        currencyType = "USD"
                        exchangeRateStr = "122.50"
                    },
                    label = { Text("🇺🇸 USD", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NexoraPrimaryOp,
                        selectedLabelColor = NexoraMainText
                    )
                )
            }
        }

        // INPUT CARDS
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = NexoraSecondaryBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Product & Exchange Rate", color = NexoraMainText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BizInputField(
                        modifier = Modifier.weight(1f),
                        label = "Unit Price ($currencyType)",
                        value = unitPriceStr,
                        onValueChange = { unitPriceStr = it }
                    )
                    BizInputField(
                        modifier = Modifier.weight(1f),
                        label = "Quantity (Pcs)",
                        value = quantityStr,
                        onValueChange = { quantityStr = it }
                    )
                }

                BizInputField(
                    label = "Exchange Rate (1 $currencyType = BDT)",
                    value = exchangeRateStr,
                    onValueChange = { exchangeRateStr = it }
                )
            }
        }

        // SHIPPING & LOGISTICS
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = NexoraSecondaryBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Shipping & Duty Fees", color = NexoraMainText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BizInputField(
                        modifier = Modifier.weight(1f),
                        label = "Weight (KG)",
                        value = weightKgStr,
                        onValueChange = { weightKgStr = it }
                    )
                    BizInputField(
                        modifier = Modifier.weight(1f),
                        label = "Ship Rate / KG (BDT)",
                        value = shippingRatePerKgStr,
                        onValueChange = { shippingRatePerKgStr = it }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BizInputField(
                        modifier = Modifier.weight(1f),
                        label = "Customs Duty (%)",
                        value = customsDutyPctStr,
                        onValueChange = { customsDutyPctStr = it }
                    )
                    BizInputField(
                        modifier = Modifier.weight(1f),
                        label = "VAT (%)",
                        value = vatPctStr,
                        onValueChange = { vatPctStr = it }
                    )
                }

                BizInputField(
                    label = "Other Expenses (C&F, Clearing, Local Transport) BDT",
                    value = otherExpensesStr,
                    onValueChange = { otherExpensesStr = it }
                )
            }
        }

        // MARGINS
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = NexoraSecondaryBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Desired Margins & Markups", color = NexoraMainText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BizInputField(
                        modifier = Modifier.weight(1f),
                        label = "Wholesale Margin (%)",
                        value = wholesaleMarginPctStr,
                        onValueChange = { wholesaleMarginPctStr = it }
                    )
                    BizInputField(
                        modifier = Modifier.weight(1f),
                        label = "Retail Margin (%)",
                        value = retailMarginPctStr,
                        onValueChange = { retailMarginPctStr = it }
                    )
                }
            }
        }

        // TARIFF DISCLAIMER
        Text(
            text = "⚠️ Notice: Customs duty, VAT, and shipping rates are editable user estimates and do not represent official government tariff schedules.",
            color = NexoraMutedText,
            fontSize = 11.sp,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        // RESULTS SUMMARY CARD
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = NexoraSecondaryBg,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, NexoraAccentBlue.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "CALCULATED LANDED RESULTS",
                    color = NexoraAccentBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                ResultRow("Product Cost (BDT)", "৳ ${NumberFormatter.formatWithCommas(result.productCostBdt, formatStyle, 2)}")
                ResultRow("Total Shipping (BDT)", "৳ ${NumberFormatter.formatWithCommas(result.shippingCostBdt, formatStyle, 2)}")
                ResultRow("Duty & VAT (BDT)", "৳ ${NumberFormatter.formatWithCommas(result.customsDutyBdt.add(result.vatBdt), formatStyle, 2)}")
                ResultRow("Other Expenses", "৳ ${NumberFormatter.formatWithCommas(result.otherExpensesBdt, formatStyle, 2)}")

                Divider(color = NexoraBorder, thickness = 1.dp)

                ResultRow("Total Landed Cost", "৳ ${NumberFormatter.formatWithCommas(result.totalLandedCostBdt, formatStyle, 2)}", isHighlight = true, highlightColor = NexoraWarning)
                ResultRow("Per-Unit Landed Cost", "৳ ${result.perUnitLandedCostBdt}", isHighlight = true, highlightColor = NexoraAccentBlue)
                ResultRow("Suggested Wholesale Price", "৳ ${result.suggestedWholesaleBdt}")
                ResultRow("Suggested Retail Price", "৳ ${result.suggestedRetailBdt}")

                Divider(color = NexoraBorder, thickness = 1.dp)

                ResultRow("Projected Total Revenue", "৳ ${NumberFormatter.formatWithCommas(result.totalRevenueRetailBdt, formatStyle, 2)}")
                ResultRow("Projected Net Profit", "৳ ${NumberFormatter.formatWithCommas(result.totalProfitBdt, formatStyle, 2)} (${result.profitMarginPercent}%)", isHighlight = true, highlightColor = NexoraSuccess)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // SAVE DIALOG
    if (showSaveDialog) {
        Dialog(onDismissRequest = { showSaveDialog = false }) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = NexoraSecondaryBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Save Import Calculation", color = NexoraMainText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Give a name to save these inputs and landed cost results for future reference.", color = NexoraSecondaryText, fontSize = 12.sp)

                    OutlinedTextField(
                        value = saveTitle,
                        onValueChange = { saveTitle = it },
                        placeholder = { Text("e.g. 200 Pcs Bags from China", color = NexoraMutedText, fontSize = 13.sp) },
                        label = { Text("Calculation Title") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = NexoraMainText,
                            unfocusedTextColor = NexoraMainText,
                            focusedBorderColor = NexoraAccentBlue,
                            unfocusedBorderColor = NexoraBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showSaveDialog = false }) {
                            Text("Cancel", color = NexoraSecondaryText)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (saveTitle.isBlank()) {
                                    Toast.makeText(context, "Please enter a title", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val summary = "Total Landed: ৳ ${NumberFormatter.formatWithCommas(result.totalLandedCostBdt, formatStyle, 0)} | Unit: ৳ ${result.perUnitLandedCostBdt} | Profit: ৳ ${NumberFormatter.formatWithCommas(result.totalProfitBdt, formatStyle, 0)}"
                                val json = JSONObject().apply {
                                    put("currencyType", currencyType)
                                    put("unitPrice", unitPriceStr)
                                    put("quantity", quantityStr)
                                    put("exchangeRate", exchangeRateStr)
                                    put("weight", weightKgStr)
                                    put("shippingRate", shippingRatePerKgStr)
                                    put("duty", customsDutyPctStr)
                                    put("vat", vatPctStr)
                                    put("other", otherExpensesStr)
                                    put("wholesaleMargin", wholesaleMarginPctStr)
                                    put("retailMargin", retailMarginPctStr)
                                }.toString()

                                viewModel.saveBusinessRecord(
                                    title = saveTitle,
                                    category = "IMPORT",
                                    summaryResult = summary,
                                    inputDataJson = json,
                                    onSaved = {
                                        Toast.makeText(context, "Saved to Business records!", Toast.LENGTH_SHORT).show()
                                        showSaveDialog = false
                                        saveTitle = ""
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NexoraAccentBlue, contentColor = NexoraMainBg),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Save Record", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // SAVED RECORDS DIALOG
    if (showRecordsDialog) {
        Dialog(onDismissRequest = { showRecordsDialog = false }) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = NexoraSecondaryBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
                modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Saved Import Calculations (${importRecords.size})", color = NexoraMainText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showRecordsDialog = false }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = NexoraSecondaryText, modifier = Modifier.size(18.dp))
                        }
                    }

                    if (importRecords.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                            Text("No saved import calculations yet.", color = NexoraMutedText, fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(importRecords, key = { it.id }) { record ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = NexoraMainBg,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(record.title, color = NexoraAccentBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            IconButton(
                                                onClick = { viewModel.deleteBusinessRecord(record.id) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = NexoraDanger, modifier = Modifier.size(16.dp))
                                            }
                                        }

                                        Text(record.summaryResult, color = NexoraMainText, fontSize = 12.sp)
                                        Text("Saved: ${record.formattedDate}", color = NexoraMutedText, fontSize = 10.sp)

                                        Button(
                                            onClick = {
                                                try {
                                                    val obj = JSONObject(record.inputDataJson)
                                                    currencyType = obj.optString("currencyType", "RMB")
                                                    unitPriceStr = obj.optString("unitPrice", "45")
                                                    quantityStr = obj.optString("quantity", "200")
                                                    exchangeRateStr = obj.optString("exchangeRate", "17.50")
                                                    weightKgStr = obj.optString("weight", "15")
                                                    shippingRatePerKgStr = obj.optString("shippingRate", "750")
                                                    customsDutyPctStr = obj.optString("duty", "10")
                                                    vatPctStr = obj.optString("vat", "15")
                                                    otherExpensesStr = obj.optString("other", "3000")
                                                    wholesaleMarginPctStr = obj.optString("wholesaleMargin", "20")
                                                    retailMarginPctStr = obj.optString("retailMargin", "35")
                                                    Toast.makeText(context, "Calculation data restored!", Toast.LENGTH_SHORT).show()
                                                    showRecordsDialog = false
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Could not restore: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = NexoraPrimaryOp, contentColor = NexoraMainText),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Restore Data", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
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
// 2. CURRENCY & EXCHANGE SPREAD CALCULATOR
// -----------------------------------------------------------------------------------------
@Composable
private fun ExchangeProfitTab(
    viewModel: CalculatorViewModel,
    formatStyle: NumberFormatStyle
) {
    var amountStr by remember { mutableStateOf("10000") }
    var buyingRateStr by remember { mutableStateOf("17.20") }
    var sellingRateStr by remember { mutableStateOf("17.65") }
    var feePctStr by remember { mutableStateOf("0.5") }
    var flatFeeStr by remember { mutableStateOf("100") }

    val context = LocalContext.current
    val allRecords by viewModel.allBusinessRecords.collectAsState()
    val exchangeRecords = remember(allRecords) { allRecords.filter { it.category == "EXCHANGE" } }

    var showSaveDialog by remember { mutableStateOf(false) }
    var showRecordsDialog by remember { mutableStateOf(false) }
    var saveTitle by remember { mutableStateOf("") }

    val result = remember(amountStr, buyingRateStr, sellingRateStr, feePctStr, flatFeeStr) {
        val amt = safeBigDecimal(amountStr)
        val buy = safeBigDecimal(buyingRateStr)
        val sell = safeBigDecimal(sellingRateStr)
        val feePct = safeBigDecimal(feePctStr)
        val flat = safeBigDecimal(flatFeeStr)

        BusinessMath.calculateCurrencyExchangeProfit(amt, buy, sell, feePct, flat)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = { showSaveDialog = true },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = NexoraAccentBlue,
                    contentColor = NexoraMainBg
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = { showRecordsDialog = true },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NexoraSecondaryText),
                border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Saved (${exchangeRecords.size})", fontSize = 12.sp)
            }
        }

        Text(
            text = "CURRENCY SPREAD & EXCHANGE PROFIT",
            color = NexoraAccentBlue,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = NexoraSecondaryBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BizInputField(
                    label = "Currency Amount (e.g. RMB or USD)",
                    value = amountStr,
                    onValueChange = { amountStr = it }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BizInputField(
                        modifier = Modifier.weight(1f),
                        label = "Buying Rate",
                        value = buyingRateStr,
                        onValueChange = { buyingRateStr = it }
                    )
                    BizInputField(
                        modifier = Modifier.weight(1f),
                        label = "Selling Rate",
                        value = sellingRateStr,
                        onValueChange = { sellingRateStr = it }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BizInputField(
                        modifier = Modifier.weight(1f),
                        label = "Transaction Fee (%)",
                        value = feePctStr,
                        onValueChange = { feePctStr = it }
                    )
                    BizInputField(
                        modifier = Modifier.weight(1f),
                        label = "Flat Processing Fee",
                        value = flatFeeStr,
                        onValueChange = { flatFeeStr = it }
                    )
                }
            }
        }

        // SPREAD RESULTS
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = NexoraSecondaryBg,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, NexoraAccentBlue.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "EXCHANGE PROFIT BREAKDOWN",
                    color = NexoraAccentBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                ResultRow("Exchange Rate Spread", "${result.spread} per unit")
                ResultRow("Total Buying Cost", "${NumberFormatter.formatWithCommas(result.effectiveBuyingCost, formatStyle, 2)}")
                ResultRow("Total Selling Revenue", "${NumberFormatter.formatWithCommas(result.effectiveSellingRevenue, formatStyle, 2)}")

                Divider(color = NexoraBorder, thickness = 1.dp)

                ResultRow("Gross Exchange Profit", "${NumberFormatter.formatWithCommas(result.totalGrossProfit, formatStyle, 2)}")
                ResultRow("Transaction Fees Deducted", "- ${NumberFormatter.formatWithCommas(result.feeDeduction, formatStyle, 2)}", highlightColor = NexoraDanger)

                Divider(color = NexoraBorder, thickness = 1.dp)

                ResultRow("Net Exchange Profit", "${NumberFormatter.formatWithCommas(result.netProfit, formatStyle, 2)}", isHighlight = true, highlightColor = if (result.netProfit >= BigDecimal.ZERO) NexoraSuccess else NexoraDanger)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showSaveDialog) {
        Dialog(onDismissRequest = { showSaveDialog = false }) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = NexoraSecondaryBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Save Exchange Calculation", color = NexoraMainText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = saveTitle,
                        onValueChange = { saveTitle = it },
                        placeholder = { Text("e.g. 10,000 RMB Spread", color = NexoraMutedText, fontSize = 13.sp) },
                        label = { Text("Calculation Title") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = NexoraMainText,
                            unfocusedTextColor = NexoraMainText,
                            focusedBorderColor = NexoraAccentBlue,
                            unfocusedBorderColor = NexoraBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showSaveDialog = false }) {
                            Text("Cancel", color = NexoraSecondaryText)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (saveTitle.isBlank()) {
                                    Toast.makeText(context, "Please enter a title", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val summary = "Amount: $amountStr | Net Profit: ${NumberFormatter.formatWithCommas(result.netProfit, formatStyle, 2)}"
                                val json = JSONObject().apply {
                                    put("amount", amountStr)
                                    put("buyingRate", buyingRateStr)
                                    put("sellingRate", sellingRateStr)
                                    put("feePct", feePctStr)
                                    put("flatFee", flatFeeStr)
                                }.toString()
                                viewModel.saveBusinessRecord(
                                    title = saveTitle,
                                    category = "EXCHANGE",
                                    summaryResult = summary,
                                    inputDataJson = json,
                                    onSaved = {
                                        Toast.makeText(context, "Saved to Business records!", Toast.LENGTH_SHORT).show()
                                        showSaveDialog = false
                                        saveTitle = ""
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NexoraAccentBlue, contentColor = NexoraMainBg),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Save Record", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showRecordsDialog) {
        Dialog(onDismissRequest = { showRecordsDialog = false }) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = NexoraSecondaryBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
                modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Saved Exchange Records (${exchangeRecords.size})", color = NexoraMainText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showRecordsDialog = false }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = NexoraSecondaryText, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (exchangeRecords.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                            Text("No saved exchange calculations yet.", color = NexoraMutedText, fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(exchangeRecords, key = { it.id }) { record ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = NexoraMainBg,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(record.title, color = NexoraAccentBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            IconButton(
                                                onClick = { viewModel.deleteBusinessRecord(record.id) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = NexoraDanger, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Text(record.summaryResult, color = NexoraMainText, fontSize = 12.sp)
                                        Text("Saved: ${record.formattedDate}", color = NexoraMutedText, fontSize = 10.sp)
                                        Button(
                                            onClick = {
                                                try {
                                                    val obj = JSONObject(record.inputDataJson)
                                                    amountStr = obj.optString("amount", "10000")
                                                    buyingRateStr = obj.optString("buyingRate", "17.20")
                                                    sellingRateStr = obj.optString("sellingRate", "17.65")
                                                    feePctStr = obj.optString("feePct", "0.5")
                                                    flatFeeStr = obj.optString("flatFee", "100")
                                                    Toast.makeText(context, "Exchange data restored!", Toast.LENGTH_SHORT).show()
                                                    showRecordsDialog = false
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Could not restore: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = NexoraPrimaryOp, contentColor = NexoraMainText),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Restore Data", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
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
// 3. BUSINESS MATHEMATICS (P&L, Discount, Margin vs Markup, Partnership, EMI, Interest)
// -----------------------------------------------------------------------------------------
enum class MathTool {
    PROFIT_LOSS,
    DISCOUNT_MARKUP,
    MARGIN_VS_MARKUP,
    PARTNERSHIP,
    LOAN_EMI,
    INTEREST,
    ROI_CAGR
}

@Composable
private fun BusinessMathTab(
    viewModel: CalculatorViewModel,
    formatStyle: NumberFormatStyle
) {
    var activeTool by remember { mutableStateOf(MathTool.PROFIT_LOSS) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // TOOL SELECTOR CHIPS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ToolChip("P&L", activeTool == MathTool.PROFIT_LOSS) { activeTool = MathTool.PROFIT_LOSS }
            ToolChip("Discount", activeTool == MathTool.DISCOUNT_MARKUP) { activeTool = MathTool.DISCOUNT_MARKUP }
            ToolChip("Margin/Markup", activeTool == MathTool.MARGIN_VS_MARKUP) { activeTool = MathTool.MARGIN_VS_MARKUP }
            ToolChip("EMI Loan", activeTool == MathTool.LOAN_EMI) { activeTool = MathTool.LOAN_EMI }
            ToolChip("Interest", activeTool == MathTool.INTEREST) { activeTool = MathTool.INTEREST }
            ToolChip("Partners", activeTool == MathTool.PARTNERSHIP) { activeTool = MathTool.PARTNERSHIP }
            ToolChip("ROI & CAGR", activeTool == MathTool.ROI_CAGR) { activeTool = MathTool.ROI_CAGR }
        }

        when (activeTool) {
            MathTool.PROFIT_LOSS -> ProfitLossSection(formatStyle)
            MathTool.DISCOUNT_MARKUP -> DiscountMarkupSection(formatStyle)
            MathTool.MARGIN_VS_MARKUP -> MarginVsMarkupSection()
            MathTool.LOAN_EMI -> LoanEmiSection(formatStyle)
            MathTool.INTEREST -> InterestSection(formatStyle)
            MathTool.PARTNERSHIP -> PartnershipSection(formatStyle)
            MathTool.ROI_CAGR -> RoiCagrSection(formatStyle)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ToolChip(title: String, isSelected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(title, fontSize = 11.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = NexoraPrimaryOp,
            selectedLabelColor = NexoraMainText,
            containerColor = NexoraCalcButton,
            labelColor = NexoraSecondaryText
        )
    )
}

@Composable
private fun ProfitLossSection(formatStyle: NumberFormatStyle) {
    var costPriceStr by remember { mutableStateOf("1200") }
    var sellingPriceStr by remember { mutableStateOf("1650") }
    var quantityStr by remember { mutableStateOf("50") }

    val cp = safeBigDecimal(costPriceStr)
    val sp = safeBigDecimal(sellingPriceStr)
    val qty = safeBigDecimal(quantityStr, BigDecimal.ONE)

    val totalCost = cp.multiply(qty)
    val totalSales = sp.multiply(qty)
    val totalProfit = totalSales.subtract(totalCost)
    val profitPctCost = if (totalCost > BigDecimal.ZERO) totalProfit.multiply(BigDecimal(100)).divide(totalCost, 2, RoundingMode.HALF_UP) else BigDecimal.ZERO
    val profitMarginSales = if (totalSales > BigDecimal.ZERO) totalProfit.multiply(BigDecimal(100)).divide(totalSales, 2, RoundingMode.HALF_UP) else BigDecimal.ZERO

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = NexoraSecondaryBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Profit & Loss Calculator", color = NexoraMainText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            BizInputField(label = "Unit Cost Price (CP)", value = costPriceStr, onValueChange = { costPriceStr = it })
            BizInputField(label = "Unit Selling Price (SP)", value = sellingPriceStr, onValueChange = { sellingPriceStr = it })
            BizInputField(label = "Quantity", value = quantityStr, onValueChange = { quantityStr = it })

            Divider(color = NexoraBorder, thickness = 1.dp)

            ResultRow("Total Cost", NumberFormatter.formatWithCommas(totalCost, formatStyle, 2))
            ResultRow("Total Revenue", NumberFormatter.formatWithCommas(totalSales, formatStyle, 2))
            ResultRow("Net ${if (totalProfit >= BigDecimal.ZERO) "Profit" else "Loss"}", NumberFormatter.formatWithCommas(totalProfit, formatStyle, 2), isHighlight = true, highlightColor = if (totalProfit >= BigDecimal.ZERO) NexoraSuccess else NexoraDanger)
            ResultRow("Profit % on Cost (Markup)", "$profitPctCost %")
            ResultRow("Profit % on Selling Price (Margin)", "$profitMarginSales %")
        }
    }
}

@Composable
private fun DiscountMarkupSection(formatStyle: NumberFormatStyle) {
    var originalPriceStr by remember { mutableStateOf("2500") }
    var discountPercentStr by remember { mutableStateOf("15") }

    val orig = safeBigDecimal(originalPriceStr)
    val discPct = safeBigDecimal(discountPercentStr)
    val savedAmount = orig.multiply(discPct).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
    val finalPrice = orig.subtract(savedAmount)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = NexoraSecondaryBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Discount & Savings Calculator", color = NexoraMainText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            BizInputField(label = "Original Price", value = originalPriceStr, onValueChange = { originalPriceStr = it })
            BizInputField(label = "Discount Percentage (%)", value = discountPercentStr, onValueChange = { discountPercentStr = it })

            Divider(color = NexoraBorder, thickness = 1.dp)

            ResultRow("You Save", NumberFormatter.formatWithCommas(savedAmount, formatStyle, 2), isHighlight = true, highlightColor = NexoraSuccess)
            ResultRow("Final Discounted Price", NumberFormatter.formatWithCommas(finalPrice, formatStyle, 2), isHighlight = true, highlightColor = NexoraAccentBlue)
        }
    }
}

@Composable
private fun MarginVsMarkupSection() {
    var marginInputStr by remember { mutableStateOf("25") }
    var markupInputStr by remember { mutableStateOf("33.33") }

    val margin = marginInputStr.toDoubleOrNull() ?: 0.0
    val equivalentMarkup = if (margin < 100.0) (margin / (100.0 - margin)) * 100.0 else 0.0

    val markup = markupInputStr.toDoubleOrNull() ?: 0.0
    val equivalentMargin = (markup / (100.0 + markup)) * 100.0

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = NexoraSecondaryBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Margin vs Markup Converter", color = NexoraMainText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)

            BizInputField(label = "Enter Desired Margin (%)", value = marginInputStr, onValueChange = { marginInputStr = it })
            ResultRow("Equivalent Markup Required", String.format(Locale.US, "%.2f %%", equivalentMarkup), isHighlight = true, highlightColor = NexoraWarning)

            Divider(color = NexoraBorder, thickness = 1.dp)

            BizInputField(label = "Enter Desired Markup (%)", value = markupInputStr, onValueChange = { markupInputStr = it })
            ResultRow("Resulting Margin Earned", String.format(Locale.US, "%.2f %%", equivalentMargin), isHighlight = true, highlightColor = NexoraAccentBlue)
        }
    }
}

@Composable
private fun LoanEmiSection(formatStyle: NumberFormatStyle) {
    var loanAmountStr by remember { mutableStateOf("500000") }
    var rateStr by remember { mutableStateOf("9.5") }
    var tenureMonthsStr by remember { mutableStateOf("36") }

    val p = safeBigDecimal(loanAmountStr)
    val r = safeBigDecimal(rateStr)
    val n = tenureMonthsStr.toIntOrNull() ?: 12

    val emiResult = remember(loanAmountStr, rateStr, tenureMonthsStr) {
        BusinessMath.calculateEmi(p, r, n)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = NexoraSecondaryBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Loan EMI & Installment Estimator", color = NexoraMainText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            BizInputField(label = "Loan Principal Amount", value = loanAmountStr, onValueChange = { loanAmountStr = it })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BizInputField(modifier = Modifier.weight(1f), label = "Annual Interest Rate (%)", value = rateStr, onValueChange = { rateStr = it })
                BizInputField(modifier = Modifier.weight(1f), label = "Tenure (Months)", value = tenureMonthsStr, onValueChange = { tenureMonthsStr = it })
            }

            Divider(color = NexoraBorder, thickness = 1.dp)

            ResultRow("Monthly EMI Payment", NumberFormatter.formatWithCommas(emiResult.monthlyEmi, formatStyle, 2), isHighlight = true, highlightColor = NexoraAccentBlue)
            ResultRow("Total Interest Payable", NumberFormatter.formatWithCommas(emiResult.totalInterest, formatStyle, 2), highlightColor = NexoraWarning)
            ResultRow("Total Payment (Principal + Interest)", NumberFormatter.formatWithCommas(emiResult.totalPayment, formatStyle, 2))
        }
    }
}

@Composable
private fun InterestSection(formatStyle: NumberFormatStyle) {
    var principalStr by remember { mutableStateOf("100000") }
    var rateStr by remember { mutableStateOf("8.5") }
    var yearsStr by remember { mutableStateOf("5") }

    val p = safeBigDecimal(principalStr)
    val r = safeBigDecimal(rateStr)
    val t = safeBigDecimal(yearsStr, BigDecimal.ONE)

    val simple = BusinessMath.calculateSimpleInterest(p, r, t)
    val compound = BusinessMath.calculateCompoundInterest(p, r, t.toDouble(), 12)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = NexoraSecondaryBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Simple vs Compound Interest", color = NexoraMainText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            BizInputField(label = "Principal Deposit", value = principalStr, onValueChange = { principalStr = it })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BizInputField(modifier = Modifier.weight(1f), label = "Interest Rate (%)", value = rateStr, onValueChange = { rateStr = it })
                BizInputField(modifier = Modifier.weight(1f), label = "Period (Years)", value = yearsStr, onValueChange = { yearsStr = it })
            }

            Divider(color = NexoraBorder, thickness = 1.dp)

            Text("Simple Interest", color = NexoraMutedText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            ResultRow("Interest Earned", NumberFormatter.formatWithCommas(simple.totalInterest, formatStyle, 2))
            ResultRow("Maturity Amount", NumberFormatter.formatWithCommas(simple.maturityAmount, formatStyle, 2))

            Divider(color = NexoraBorder, thickness = 1.dp)

            Text("Compound Interest (Compounded Monthly)", color = NexoraAccentBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            ResultRow("Compound Interest Earned", NumberFormatter.formatWithCommas(compound.totalInterest, formatStyle, 2), isHighlight = true, highlightColor = NexoraSuccess)
            ResultRow("Compound Maturity Amount", NumberFormatter.formatWithCommas(compound.maturityAmount, formatStyle, 2), isHighlight = true, highlightColor = NexoraAccentBlue)
        }
    }
}

@Composable
private fun PartnershipSection(formatStyle: NumberFormatStyle) {
    var totalProfitStr by remember { mutableStateOf("1500000") }
    var partnerAStr by remember { mutableStateOf("500000") }
    var partnerBStr by remember { mutableStateOf("300000") }
    var partnerCStr by remember { mutableStateOf("200000") }

    val profit = safeBigDecimal(totalProfitStr)
    val capA = safeBigDecimal(partnerAStr)
    val capB = safeBigDecimal(partnerBStr)
    val capC = safeBigDecimal(partnerCStr)

    val totalCapital = capA.add(capB).add(capC)
    val shareA = if (totalCapital > BigDecimal.ZERO) capA.multiply(profit).divide(totalCapital, 2, RoundingMode.HALF_UP) else BigDecimal.ZERO
    val pctA = if (totalCapital > BigDecimal.ZERO) capA.multiply(BigDecimal(100)).divide(totalCapital, 2, RoundingMode.HALF_UP) else BigDecimal.ZERO

    val shareB = if (totalCapital > BigDecimal.ZERO) capB.multiply(profit).divide(totalCapital, 2, RoundingMode.HALF_UP) else BigDecimal.ZERO
    val pctB = if (totalCapital > BigDecimal.ZERO) capB.multiply(BigDecimal(100)).divide(totalCapital, 2, RoundingMode.HALF_UP) else BigDecimal.ZERO

    val shareC = if (totalCapital > BigDecimal.ZERO) capC.multiply(profit).divide(totalCapital, 2, RoundingMode.HALF_UP) else BigDecimal.ZERO
    val pctC = if (totalCapital > BigDecimal.ZERO) capC.multiply(BigDecimal(100)).divide(totalCapital, 2, RoundingMode.HALF_UP) else BigDecimal.ZERO

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = NexoraSecondaryBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Partnership Profit Distribution", color = NexoraMainText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            BizInputField(label = "Total Profit to Distribute", value = totalProfitStr, onValueChange = { totalProfitStr = it })
            BizInputField(label = "Partner A Capital Contribution", value = partnerAStr, onValueChange = { partnerAStr = it })
            BizInputField(label = "Partner B Capital Contribution", value = partnerBStr, onValueChange = { partnerBStr = it })
            BizInputField(label = "Partner C Capital Contribution", value = partnerCStr, onValueChange = { partnerCStr = it })

            Divider(color = NexoraBorder, thickness = 1.dp)

            ResultRow("Partner A Payout ($pctA %)", NumberFormatter.formatWithCommas(shareA, formatStyle, 2), isHighlight = true, highlightColor = NexoraAccentBlue)
            ResultRow("Partner B Payout ($pctB %)", NumberFormatter.formatWithCommas(shareB, formatStyle, 2), isHighlight = true, highlightColor = NexoraSuccess)
            ResultRow("Partner C Payout ($pctC %)", NumberFormatter.formatWithCommas(shareC, formatStyle, 2), isHighlight = true, highlightColor = NexoraWarning)
        }
    }
}

@Composable
private fun RoiCagrSection(formatStyle: NumberFormatStyle) {
    var initialInvestmentStr by remember { mutableStateOf("100000") }
    var finalValueStr by remember { mutableStateOf("175000") }
    var yearsStr by remember { mutableStateOf("3") }

    val initial = safeBigDecimal(initialInvestmentStr)
    val finalVal = safeBigDecimal(finalValueStr)
    val years = yearsStr.toDoubleOrNull() ?: 1.0

    val roiResult = remember(initialInvestmentStr, finalValueStr, yearsStr) {
        BusinessMath.calculateRoi(initial, finalVal, years)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = NexoraSecondaryBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Investment Return (ROI & CAGR)", color = NexoraMainText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            BizInputField(label = "Initial Investment", value = initialInvestmentStr, onValueChange = { initialInvestmentStr = it })
            BizInputField(label = "Final Value / Current Worth", value = finalValueStr, onValueChange = { finalValueStr = it })
            BizInputField(label = "Investment Duration (Years)", value = yearsStr, onValueChange = { yearsStr = it })

            Divider(color = NexoraBorder, thickness = 1.dp)

            ResultRow("Net Profit / Gain", NumberFormatter.formatWithCommas(roiResult.absoluteProfit, formatStyle, 2), isHighlight = true, highlightColor = if (roiResult.absoluteProfit >= BigDecimal.ZERO) NexoraSuccess else NexoraDanger)
            ResultRow("Total ROI (Absolute Return)", "${roiResult.roiPercent} %", isHighlight = true, highlightColor = NexoraAccentBlue)
            ResultRow("Annualized Return (CAGR)", "${roiResult.cagrPercent} % / year", isHighlight = true, highlightColor = NexoraGold)
        }
    }
}

// -----------------------------------------------------------------------------------------
// 4. LARGE NUMBER CONVERTER (Lakhs, Crores, Arabs vs Millions, Billions, Trillions)
// -----------------------------------------------------------------------------------------
@Composable
private fun LargeNumberTab(viewModel: CalculatorViewModel) {
    val inputNumber by viewModel.largeNumberInput.collectAsState()
    val breakdown by viewModel.largeNumberBreakdown.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "LARGE NUMBER & CURRENCY WORDS CONVERTER",
            color = NexoraAccentBlue,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        // INPUT FIELD
        OutlinedTextField(
            value = inputNumber,
            onValueChange = { viewModel.setLargeNumberInput(it) },
            label = { Text("Enter Any Number (up to hundreds of Crores / Billions)") },
            placeholder = { Text("e.g. 10000000") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("input_large_number"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NexoraAccentBlue,
                unfocusedBorderColor = NexoraBorder,
                focusedTextColor = NexoraMainText,
                unfocusedTextColor = NexoraMainText
            )
        )

        // QUICK PRESETS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PresetPill("1 Lakh (1L)") { viewModel.setLargeNumberInput("100000") }
            PresetPill("10 Lakh (1M)") { viewModel.setLargeNumberInput("1000000") }
            PresetPill("1 Crore (1Cr)") { viewModel.setLargeNumberInput("10000000") }
            PresetPill("100 Cr (1B)") { viewModel.setLargeNumberInput("1000000000") }
        }

        if (breakdown != null) {
            // SOUTH ASIAN BREAKDOWN CARD
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = NexoraSecondaryBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, NexoraAccentBlue.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SOUTH ASIAN NOTATION (Lakh / Crore / Arab)", color = NexoraAccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        IconButton(
                            onClick = { viewModel.copyToClipboard(breakdown!!.southAsianWords) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = NexoraSecondaryText, modifier = Modifier.size(16.dp))
                        }
                    }

                    Text(
                        text = breakdown!!.southAsianNotation,
                        color = NexoraMainText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        text = breakdown!!.southAsianWords,
                        color = NexoraWarning,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // INTERNATIONAL BREAKDOWN CARD
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
                        Text("INTERNATIONAL NOTATION (Million / Billion)", color = NexoraSecondaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        IconButton(
                            onClick = { viewModel.copyToClipboard(breakdown!!.internationalWords) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = NexoraSecondaryText, modifier = Modifier.size(16.dp))
                        }
                    }

                    Text(
                        text = breakdown!!.internationalNotation,
                        color = NexoraMainText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        text = breakdown!!.internationalWords,
                        color = NexoraSuccess,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // SCIENTIFIC NOTATION
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = NexoraSecondaryBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("SCIENTIFIC NOTATION", color = NexoraMutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = breakdown!!.scientificNotation,
                        color = NexoraMainText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PresetPill(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(NexoraCalcButton)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = NexoraMainText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun BizInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NexoraAccentBlue,
            unfocusedBorderColor = NexoraBorder,
            focusedTextColor = NexoraMainText,
            unfocusedTextColor = NexoraMainText
        )
    )
}

@Composable
private fun ResultRow(
    label: String,
    value: String,
    isHighlight: Boolean = false,
    highlightColor: Color = NexoraMainText
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (isHighlight) NexoraMainText else NexoraSecondaryText,
            fontSize = if (isHighlight) 14.sp else 13.sp,
            fontWeight = if (isHighlight) FontWeight.SemiBold else FontWeight.Normal
        )
        Text(
            text = value,
            color = if (isHighlight) highlightColor else NexoraMainText,
            fontSize = if (isHighlight) 16.sp else 13.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun safeBigDecimal(str: String, default: BigDecimal = BigDecimal.ZERO): BigDecimal {
    val clean = str.replace(",", "").trim()
    if (clean.isEmpty() || clean == "." || clean == "-" || clean == "+") return default
    return try {
        BigDecimal(clean)
    } catch (_: Exception) {
        default
    }
}
