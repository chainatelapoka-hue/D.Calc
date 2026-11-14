package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CurrencyCatalog
import com.example.data.model.CurrencyItem
import com.example.engine.NumberFormatStyle
import com.example.engine.NumberFormatter
import com.example.ui.CalculatorViewModel
import com.example.ui.theme.*
import java.math.BigDecimal
import java.util.Locale

@Composable
fun CurrencyConverterScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val currencyFrom by viewModel.currencyFrom.collectAsState()
    val currencyTo by viewModel.currencyTo.collectAsState()
    val inputAmount by viewModel.currencyInputAmount.collectAsState()
    val useCustomRate by viewModel.useCustomRate.collectAsState()
    val customRateInput by viewModel.customRateInput.collectAsState()

    val conversionResult by viewModel.currencyConversionResult.collectAsState()
    val lastUpdated by viewModel.currencyLastUpdated.collectAsState()
    val isOffline by viewModel.currencyIsOffline.collectAsState()
    val isRefreshing by viewModel.currencyIsRefreshing.collectAsState()
    val statusMsg by viewModel.currencyStatusMessage.collectAsState()
    val settings by viewModel.userSettings.collectAsState()

    var showCurrencyDialogFor by remember { mutableStateOf<String?>(null) } // "FROM" or "TO" or null
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    val animatedRotation by animateFloatAsState(targetValue = rotationAngle, label = "swap_rot")

    val fromItem = CurrencyCatalog.find(currencyFrom)
    val toItem = CurrencyCatalog.find(currencyTo)

    val convertedValue = conversionResult.first
    val effectiveRate = conversionResult.second

    val formattedConverted = try {
        NumberFormatter.formatWithCommas(
            BigDecimal(convertedValue.toString()),
            settings.formatStyle,
            settings.decimalPrecision
        )
    } catch (_: Exception) {
        String.format(Locale.US, "%.2f", convertedValue)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NexoraMainBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // HEADER: Status Banner & Refresh
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CURRENCY CONVERTER",
                    color = NexoraAccentBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isOffline) NexoraWarning else NexoraSuccess)
                    )
                    Text(
                        text = if (isOffline) "Offline / Cached Rates" else "Live Rates Active",
                        color = if (isOffline) NexoraWarning else NexoraSuccess,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Refresh Button
            IconButton(
                onClick = { viewModel.refreshCurrencyRates() },
                enabled = !isRefreshing,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(NexoraSecondaryBg)
                    .border(1.dp, NexoraBorder, RoundedCornerShape(12.dp))
                    .testTag("btn_refresh_currency")
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = NexoraAccentBlue
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Rates",
                        tint = NexoraAccentBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // SOURCE CURRENCY CARD
        CurrencyCard(
            title = "YOU PAY / FROM",
            item = fromItem,
            amount = inputAmount,
            isEditable = true,
            onAmountChange = { viewModel.setCurrencyInputAmount(it) },
            onSelectCurrencyClick = { showCurrencyDialogFor = "FROM" }
        )

        // SWAP CURRENCIES BUTTON
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .rotate(animatedRotation)
                    .clip(CircleShape)
                    .background(NexoraPrimaryOp)
                    .border(1.5.dp, NexoraAccentBlue, CircleShape)
                    .clickable {
                        rotationAngle += 180f
                        viewModel.swapCurrencies()
                    }
                    .testTag("btn_swap_currencies"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = "Swap Currencies",
                    tint = NexoraMainText,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // DESTINATION CURRENCY CARD
        CurrencyCard(
            title = "YOU RECEIVE / TO",
            item = toItem,
            amount = formattedConverted,
            isEditable = false,
            onAmountChange = {},
            onSelectCurrencyClick = { showCurrencyDialogFor = "TO" }
        )

        // CONVERSION RATE DETAILS CARD
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = NexoraSecondaryBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EXCHANGE RATE",
                        color = NexoraMutedText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (useCustomRate) "CUSTOM RATE" else "MARKET REFERENCE",
                        color = if (useCustomRate) NexoraWarning else NexoraAccentBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "1 ${fromItem.code} = ${String.format(Locale.US, "%.4f", effectiveRate)} ${toItem.code}",
                    color = NexoraMainText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = "1 ${toItem.code} = ${String.format(Locale.US, "%.4f", if (effectiveRate > 0) 1.0 / effectiveRate else 0.0)} ${fromItem.code}",
                    color = NexoraSecondaryText,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )

                Divider(color = NexoraBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Source: open.er-api.com",
                        color = NexoraMutedText,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "Updated: $lastUpdated",
                        color = NexoraMutedText,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // USER CUSTOM EXCHANGE RATE CARD
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = NexoraSecondaryBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (useCustomRate) NexoraWarning.copy(alpha = 0.6f) else NexoraBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Custom Buying / Selling Rate",
                            color = NexoraMainText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Override market rate with your dealer/bank rate",
                            color = NexoraMutedText,
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked = useCustomRate,
                        onCheckedChange = { viewModel.setUseCustomRate(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NexoraMainBg,
                            checkedTrackColor = NexoraWarning,
                            uncheckedThumbColor = NexoraMutedText,
                            uncheckedTrackColor = NexoraCalcButton
                        ),
                        modifier = Modifier.testTag("switch_custom_rate")
                    )
                }

                AnimatedVisibility(visible = useCustomRate) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = customRateInput,
                            onValueChange = { viewModel.setCustomRateInput(it) },
                            label = { Text("Enter 1 ${fromItem.code} = X ${toItem.code}") },
                            placeholder = { Text("e.g. 17.50") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_custom_rate"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NexoraWarning,
                                unfocusedBorderColor = NexoraBorder,
                                focusedTextColor = NexoraMainText,
                                unfocusedTextColor = NexoraMainText
                            )
                        )

                        // Quick buttons for RMB & USD defaults
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    viewModel.setCurrencyFrom("CNY")
                                    viewModel.setCurrencyTo("BDT")
                                    viewModel.setCustomRateInput(settings.customRmbToBdt.toString())
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NexoraCalcButton),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("RMB→BDT (${settings.customRmbToBdt})", fontSize = 11.sp, color = NexoraMainText)
                            }
                            Button(
                                onClick = {
                                    viewModel.setCurrencyFrom("USD")
                                    viewModel.setCurrencyTo("BDT")
                                    viewModel.setCustomRateInput(settings.customUsdToBdt.toString())
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NexoraCalcButton),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("USD→BDT (${settings.customUsdToBdt})", fontSize = 11.sp, color = NexoraMainText)
                            }
                        }
                    }
                }
            }
        }
    }

    // CURRENCY SELECTION MODAL DIALOG
    if (showCurrencyDialogFor != null) {
        CurrencySelectionDialog(
            title = if (showCurrencyDialogFor == "FROM") "Select Source Currency" else "Select Target Currency",
            onDismiss = { showCurrencyDialogFor = null },
            onSelect = { selectedItem ->
                if (showCurrencyDialogFor == "FROM") {
                    viewModel.setCurrencyFrom(selectedItem.code)
                } else {
                    viewModel.setCurrencyTo(selectedItem.code)
                }
                showCurrencyDialogFor = null
            }
        )
    }
}

@Composable
private fun CurrencyCard(
    title: String,
    item: CurrencyItem,
    amount: String,
    isEditable: Boolean,
    onAmountChange: (String) -> Unit,
    onSelectCurrencyClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = NexoraSecondaryBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = NexoraMutedText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // Select Currency Picker Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(NexoraCalcButton)
                        .clickable { onSelectCurrencyClick() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = item.flagEmoji, fontSize = 18.sp)
                    Text(
                        text = item.code,
                        color = NexoraMainText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select",
                        tint = NexoraSecondaryText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Amount Input / Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.symbol,
                    color = NexoraSecondaryText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 8.dp)
                )

                if (isEditable) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = onAmountChange,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_currency_amount"),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = NexoraMainText,
                            textAlign = TextAlign.End
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                } else {
                    Text(
                        text = amount,
                        color = NexoraAccentBlue,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).testTag("txt_converted_amount")
                    )
                }
            }

            Text(
                text = item.name,
                color = NexoraSecondaryText,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CurrencySelectionDialog(
    title: String,
    onDismiss: () -> Unit,
    onSelect: (CurrencyItem) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredList = remember(searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) {
            CurrencyCatalog.ALL_CURRENCIES
        } else {
            CurrencyCatalog.ALL_CURRENCIES.filter {
                it.code.lowercase().contains(q) || it.name.lowercase().contains(q)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = NexoraSecondaryBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = NexoraMainText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = NexoraSecondaryText)
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by code or country...", color = NexoraMutedText, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NexoraSecondaryText) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = NexoraSecondaryText)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_search_currency"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexoraAccentBlue,
                        unfocusedBorderColor = NexoraBorder,
                        focusedTextColor = NexoraMainText,
                        unfocusedTextColor = NexoraMainText
                    )
                )

                // List of Currencies
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredList) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelect(item) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(text = item.flagEmoji, fontSize = 22.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.code,
                                    color = NexoraMainText,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = item.name,
                                    color = NexoraSecondaryText,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = item.symbol,
                                color = NexoraAccentBlue,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
