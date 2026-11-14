package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.LedgerAccountEntity
import com.example.data.local.LedgerItemEntity
import com.example.data.local.LedgerSettlementEntity
import com.example.engine.NumberFormatStyle
import com.example.engine.NumberFormatter
import com.example.ui.CalculatorViewModel
import com.example.ui.theme.*
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LedgerScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val accounts by viewModel.allLedgerAccounts.collectAsState()
    val totalOutstanding by viewModel.totalOutstandingLedger.collectAsState()
    val selectedAccount by viewModel.selectedAccount.collectAsState()
    val settings by viewModel.userSettings.collectAsState()

    var showAddAccountDialog by remember { mutableStateOf(false) }

    // Intercept hardware back button when inside an account detail to return to account list
    BackHandler(enabled = selectedAccount != null) {
        viewModel.selectAccount(null)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NexoraMainBg)
    ) {
        if (selectedAccount == null) {
            // VIEW 1: ACCOUNTS / DIRECTORY LIST
            AccountsDirectoryView(
                accounts = accounts,
                totalOutstanding = totalOutstanding,
                formatStyle = settings.formatStyle,
                onAccountClick = { account -> viewModel.selectAccount(account) },
                onAddAccountClick = { showAddAccountDialog = true }
            )
        } else {
            // VIEW 2: ACCOUNT DETAIL & ITEM LEDGER
            AccountDetailLedgerView(
                account = selectedAccount!!,
                viewModel = viewModel,
                formatStyle = settings.formatStyle,
                onBack = { viewModel.selectAccount(null) }
            )
        }

        // DIALOG: ADD NEW ACCOUNT
        if (showAddAccountDialog) {
            AddAccountDialog(
                onDismiss = { showAddAccountDialog = false },
                onConfirm = { name, phone, notes ->
                    viewModel.createLedgerAccount(name, phone, notes)
                    showAddAccountDialog = false
                }
            )
        }
    }
}

@Composable
private fun AccountsDirectoryView(
    accounts: List<LedgerAccountEntity>,
    totalOutstanding: Double,
    formatStyle: NumberFormatStyle,
    onAccountClick: (LedgerAccountEntity) -> Unit,
    onAddAccountClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredAccounts = remember(accounts, searchQuery) {
        if (searchQuery.isBlank()) accounts
        else accounts.filter { it.name.contains(searchQuery, ignoreCase = true) || it.notes.contains(searchQuery, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // SUMMARY BANNER CARD
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = NexoraSecondaryBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(NexoraPrimaryOp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = "Ledger",
                                    tint = NexoraAccentBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "LEDGER / হিসাব খাতা",
                                    color = NexoraMainText,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Customer, shop & personal debts",
                                    color = NexoraMutedText,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Add Button
                        FilledTonalButton(
                            onClick = onAddAccountClick,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = NexoraAccentBlue,
                                contentColor = NexoraMainBg
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_new_account")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Book", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = NexoraBorder.copy(alpha = 0.4f), thickness = 1.dp)

                    // Outstanding Total Display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "TOTAL OUTSTANDING (বকেয়া)",
                                color = NexoraMutedText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            val totalBd = BigDecimal(totalOutstanding).setScale(2, java.math.RoundingMode.HALF_UP)
                            val formattedTotal = NumberFormatter.formatWithCommas(totalBd, formatStyle)
                            Text(
                                text = "৳ $formattedTotal",
                                color = NexoraAccentBlue,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NexoraCalcButton
                        ) {
                            Text(
                                text = "${accounts.size} Books",
                                color = NexoraSecondaryText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // SEARCH BAR
        if (accounts.isNotEmpty()) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search shop or person name...", color = NexoraMutedText, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = NexoraSecondaryText, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = NexoraSecondaryText, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = NexoraSecondaryBg,
                        unfocusedContainerColor = NexoraSecondaryBg,
                        focusedBorderColor = NexoraAccentBlue,
                        unfocusedBorderColor = NexoraBorder,
                        focusedTextColor = NexoraMainText,
                        unfocusedTextColor = NexoraMainText
                    ),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                )
            }
        }

        // EMPTY STATE
        if (accounts.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = NexoraSecondaryBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(NexoraPrimaryOp.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = NexoraAccentBlue, modifier = Modifier.size(28.dp))
                        }
                        Text(
                            text = "No Account Books Yet",
                            color = NexoraMainText,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Add an entity or individual (e.g., \"Akbar's Shop\") to track item purchases, prices, dates, and balance settlements.",
                            color = NexoraSecondaryText,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onAddAccountClick,
                            colors = ButtonDefaults.buttonColors(containerColor = NexoraAccentBlue, contentColor = NexoraMainBg),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Akbar's Shop", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // ACCOUNTS LIST
            items(filteredAccounts, key = { it.id }) { account ->
                AccountCard(
                    account = account,
                    onClick = { onAccountClick(account) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AccountCard(
    account: LedgerAccountEntity,
    onClick: () -> Unit
) {
    val dateStr = remember(account.lastUpdated) {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        sdf.format(Date(account.lastUpdated))
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = NexoraSecondaryBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Initial Badge
                val initial = account.name.trim().take(1).uppercase(Locale.getDefault())
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(NexoraPrimaryOp)
                        .border(1.dp, NexoraAccentBlue.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (initial.isEmpty()) "A" else initial,
                        color = NexoraAccentBlue,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = account.name,
                        color = NexoraMainText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (account.phone.isNotBlank()) {
                            Text(
                                text = account.phone,
                                color = NexoraMutedText,
                                fontSize = 11.sp
                            )
                            Text("•", color = NexoraMutedText, fontSize = 10.sp)
                        }
                        Text(
                            text = "Updated: $dateStr",
                            color = NexoraMutedText,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = NexoraSecondaryText,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun AccountDetailLedgerView(
    account: LedgerAccountEntity,
    viewModel: CalculatorViewModel,
    formatStyle: NumberFormatStyle,
    onBack: () -> Unit
) {
    val activeItems by viewModel.selectedAccountActiveItems.collectAsState()
    val totalAmount by viewModel.selectedAccountTotal.collectAsState()
    val settlements by viewModel.selectedAccountSettlements.collectAsState()
    val context = LocalContext.current

    var showAddItemDialog by remember { mutableStateOf(false) }
    var showSettleDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showSettlementHistory by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // TOP BAR: BACK & TITLE & ACTIONS
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = NexoraAccentBlue
                    )
                }

                Text(
                    text = account.name,
                    color = NexoraMainText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Share / Copy Summary
                    IconButton(
                        onClick = {
                            val summary = buildString {
                                append("Account: ${account.name}\n")
                                append("Date: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())}\n")
                                append("Total Balance: ৳ ${NumberFormatter.formatWithCommas(BigDecimal(totalAmount).setScale(2, java.math.RoundingMode.HALF_UP), formatStyle)}\n\n")
                                append("Items:\n")
                                activeItems.forEachIndexed { i, item ->
                                    val itemTotal = item.price * item.quantity
                                    append("${i + 1}. ${item.itemName} - ৳ $itemTotal (${item.formattedDate})\n")
                                }
                            }
                            viewModel.copyToClipboard(summary)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = NexoraSecondaryText, modifier = Modifier.size(18.dp))
                    }

                    // Delete Account
                    IconButton(
                        onClick = { showDeleteAccountDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Account", tint = NexoraDanger, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // ACCOUNT BALANCE BANNER (Requirement 2 & 4)
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = NexoraSecondaryBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "CURRENT OUTSTANDING (মোট বকেয়া)",
                                color = NexoraMutedText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            val totalBd = BigDecimal(totalAmount).setScale(2, java.math.RoundingMode.HALF_UP)
                            val formattedTotal = NumberFormatter.formatWithCommas(totalBd, formatStyle)
                            Text(
                                text = "৳ $formattedTotal",
                                color = if (totalAmount > 0) NexoraAccentBlue else NexoraSuccess,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (totalAmount > 0) NexoraPrimaryOp else NexoraSuccess.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (totalAmount > 0) "${activeItems.size} Unsettled" else "All Cleared",
                                color = if (totalAmount > 0) NexoraAccentBlue else NexoraSuccess,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = NexoraBorder.copy(alpha = 0.4f), thickness = 1.dp)

                    // ACTION BUTTONS: ADD ITEM & CLEAR ACCOUNT
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showAddItemDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NexoraAccentBlue,
                                contentColor = NexoraMainBg
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(44.dp).testTag("btn_add_item")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Item", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                if (totalAmount <= 0) {
                                    Toast.makeText(context, "Balance is already ৳ 0.00", Toast.LENGTH_SHORT).show()
                                } else {
                                    showSettleDialog = true
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (totalAmount > 0) NexoraSuccess else NexoraMutedText
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (totalAmount > 0) NexoraSuccess else NexoraBorder
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(44.dp).testTag("btn_clear_account")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear Account", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // ITEMS HEADER
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECORDED ITEMS (${activeItems.size})",
                    color = NexoraSecondaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                if (settlements.isNotEmpty()) {
                    Text(
                        text = if (showSettlementHistory) "Hide History" else "Past Settlements (${settlements.size})",
                        color = NexoraAccentBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { showSettlementHistory = !showSettlementHistory }
                            .padding(vertical = 2.dp)
                    )
                }
            }
        }

        // SETTLEMENT HISTORY (if expanded)
        if (showSettlementHistory && settlements.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NexoraCalcButton,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "PAST SETTLEMENTS (পরিশোধের রেকর্ড)",
                            color = NexoraAccentBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        settlements.forEach { s ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Cleared ৳ ${NumberFormatter.formatWithCommas(BigDecimal(s.amount).setScale(2, java.math.RoundingMode.HALF_UP), formatStyle)}",
                                        color = NexoraSuccess,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${s.formattedDate} • ${s.note}",
                                        color = NexoraMutedText,
                                        fontSize = 10.sp
                                    )
                                }
                                Icon(Icons.Default.Check, contentDescription = null, tint = NexoraSuccess, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // ITEMS LIST (Requirements 2 & 3: name, price, auto-recorded date)
        if (activeItems.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = NexoraSecondaryBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = NexoraMutedText, modifier = Modifier.size(36.dp))
                        Text(
                            text = "No Items in this Account Book",
                            color = NexoraMainText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Tap '+ Add Item' above to record items (e.g. cigarettes 35 Taka, ice cream 50 Taka, soap 55 Taka). Date will be recorded automatically.",
                            color = NexoraSecondaryText,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(activeItems, key = { it.id }) { item ->
                LedgerItemCard(
                    item = item,
                    formatStyle = formatStyle,
                    onDelete = { viewModel.deleteLedgerItem(item.id, account.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // DIALOG: ADD ITEM (Requirements 2 & 3)
    if (showAddItemDialog) {
        AddLedgerItemDialog(
            accountName = account.name,
            onDismiss = { showAddItemDialog = false },
            onConfirm = { name, price, quantity ->
                viewModel.addLedgerItem(account.id, name, price, quantity)
                showAddItemDialog = false
            }
        )
    }

    // DIALOG: CLEAR / SETTLE ACCOUNT (Requirement 4)
    if (showSettleDialog) {
        ClearAccountDialog(
            accountName = account.name,
            currentTotal = totalAmount,
            formatStyle = formatStyle,
            onDismiss = { showSettleDialog = false },
            onConfirm = { note ->
                viewModel.settleLedgerAccount(account.id, totalAmount, note)
                showSettleDialog = false
            }
        )
    }

    // DIALOG: DELETE ACCOUNT
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Delete ${account.name}?", color = NexoraMainText) },
            text = { Text("This will permanently remove this account book, all recorded items, and settlement history.", color = NexoraSecondaryText) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteLedgerAccount(account.id)
                        showDeleteAccountDialog = false
                    }
                ) {
                    Text("Delete", color = NexoraDanger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Cancel", color = NexoraSecondaryText)
                }
            },
            containerColor = NexoraSecondaryBg
        )
    }
}

@Composable
private fun LedgerItemCard(
    item: LedgerItemEntity,
    formatStyle: NumberFormatStyle,
    onDelete: () -> Unit
) {
    val totalItemPrice = item.price * item.quantity
    val formattedPrice = NumberFormatter.formatWithCommas(
        BigDecimal(totalItemPrice).setScale(2, java.math.RoundingMode.HALF_UP),
        formatStyle
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = NexoraSecondaryBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = item.itemName,
                    color = NexoraMainText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // Date automatically recorded
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Date",
                        tint = NexoraMutedText,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = item.formattedDate,
                        color = NexoraMutedText,
                        fontSize = 11.sp
                    )
                    if (item.quantity > 1) {
                        Text(
                            text = "• Qty: ${item.quantity} (৳ ${item.price} each)",
                            color = NexoraAccentBlue,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "৳ $formattedPrice",
                    color = NexoraAccentBlue,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete item",
                        tint = NexoraMutedText,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, notes: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    val quickNames = listOf("Akbar's Shop", "Karim Store", "Tea Stall / চায়ের দোকান", "Grocery / মুদি দোকান", "Personal / বন্ধু")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = NexoraSecondaryBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "New Account Book",
                    color = NexoraMainText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                // Quick Suggestions
                Text("Quick Suggestions:", color = NexoraMutedText, fontSize = 11.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(quickNames) { suggestion ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NexoraCalcButton,
                            modifier = Modifier.clickable { name = suggestion }
                        ) {
                            Text(
                                text = suggestion,
                                color = NexoraAccentBlue,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) error = false
                    },
                    label = { Text("Entity or Individual Name (e.g. Akbar's Shop)") },
                    isError = error,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = NexoraMainText,
                        unfocusedTextColor = NexoraMainText,
                        focusedBorderColor = NexoraAccentBlue,
                        unfocusedBorderColor = NexoraBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_account_name")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone / Mobile (Optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = NexoraMainText,
                        unfocusedTextColor = NexoraMainText,
                        focusedBorderColor = NexoraAccentBlue,
                        unfocusedBorderColor = NexoraBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Note / Location (Optional)") },
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
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = NexoraSecondaryText)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                error = true
                            } else {
                                onConfirm(name, phone, notes)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NexoraAccentBlue, contentColor = NexoraMainBg),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Create Book", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddLedgerItemDialog(
    accountName: String,
    onDismiss: () -> Unit,
    onConfirm: (itemName: String, price: Double, quantity: Int) -> Unit
) {
    var itemName by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("1") }
    var error by remember { mutableStateOf<String?>(null) }

    val currentDateStr = remember {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        sdf.format(Date())
    }

    // Common item suggestions based on user prompt (cigarettes 35 Taka, ice cream 50 Taka, soap 55 Taka, etc.)
    val suggestions = listOf(
        Pair("Cigarettes (সিগারেট)", "35"),
        Pair("Ice cream (আইসক্রিম)", "50"),
        Pair("Soap (সাবান)", "55"),
        Pair("Tea (চা)", "10"),
        Pair("Biscuits (বিস্কুট)", "20"),
        Pair("Water (পানি)", "15")
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = NexoraSecondaryBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column {
                    Text(
                        text = "Add Item to $accountName",
                        color = NexoraMainText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    // Requirement 3: Automatic current date indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = NexoraAccentBlue, modifier = Modifier.size(13.dp))
                        Text(
                            text = "Auto-Date: $currentDateStr",
                            color = NexoraAccentBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Quick item presets
                Text("Quick Presets:", color = NexoraMutedText, fontSize = 11.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(suggestions) { (name, price) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NexoraCalcButton,
                            modifier = Modifier.clickable {
                                itemName = name.substringBefore(" (")
                                priceText = price
                            }
                        ) {
                            Text(
                                text = "$name ৳$price",
                                color = NexoraSecondaryText,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it; error = null },
                    label = { Text("Item Name (e.g. Cigarettes, Ice cream)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = NexoraMainText,
                        unfocusedTextColor = NexoraMainText,
                        focusedBorderColor = NexoraAccentBlue,
                        unfocusedBorderColor = NexoraBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_item_name")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it; error = null },
                        label = { Text("Price (Taka)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        leadingIcon = { Text("৳", color = NexoraAccentBlue, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = NexoraMainText,
                            unfocusedTextColor = NexoraMainText,
                            focusedBorderColor = NexoraAccentBlue,
                            unfocusedBorderColor = NexoraBorder
                        ),
                        modifier = Modifier.weight(1.3f).testTag("input_item_price")
                    )

                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Qty") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = NexoraMainText,
                            unfocusedTextColor = NexoraMainText,
                            focusedBorderColor = NexoraAccentBlue,
                            unfocusedBorderColor = NexoraBorder
                        ),
                        modifier = Modifier.weight(0.7f)
                    )
                }

                if (error != null) {
                    Text(text = error!!, color = NexoraDanger, fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = NexoraSecondaryText)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val priceVal = priceText.toDoubleOrNull()
                            val qtyVal = quantityText.toIntOrNull() ?: 1
                            if (itemName.isBlank()) {
                                error = "Please enter item name"
                            } else if (priceVal == null || priceVal <= 0.0) {
                                error = "Please enter a valid price in Taka"
                            } else {
                                onConfirm(itemName, priceVal, qtyVal)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NexoraAccentBlue, contentColor = NexoraMainBg),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_save_item")
                    ) {
                        Text("Add to Ledger", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ClearAccountDialog(
    accountName: String,
    currentTotal: Double,
    formatStyle: NumberFormatStyle,
    onDismiss: () -> Unit,
    onConfirm: (note: String) -> Unit
) {
    var note by remember { mutableStateOf("Full payment / পরিশোধ") }

    val totalBd = BigDecimal(currentTotal).setScale(2, java.math.RoundingMode.HALF_UP)
    val formattedTotal = NumberFormatter.formatWithCommas(totalBd, formatStyle)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = NexoraSecondaryBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(NexoraSuccess.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NexoraSuccess, modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Text(
                            text = "Clear & Settle Balance",
                            color = NexoraMainText,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = accountName,
                            color = NexoraAccentBlue,
                            fontSize = 12.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NexoraCalcButton,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Amount to Settle", color = NexoraMutedText, fontSize = 12.sp)
                        Text(
                            text = "৳ $formattedTotal",
                            color = NexoraSuccess,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "This will mark all current items as settled and reset outstanding balance to ৳ 0.00.",
                            color = NexoraSecondaryText,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Settlement Note (e.g. Paid in cash)") },
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
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = NexoraSecondaryText)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(note) },
                        colors = ButtonDefaults.buttonColors(containerColor = NexoraSuccess, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_confirm_clear")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Confirm Clear", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
