package com.example.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.HistoryEntity
import com.example.engine.NumberFormatStyle
import com.example.engine.NumberFormatter
import com.example.ui.theme.*
import com.example.util.ReceiptImageHelper
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DisplayPanel(
    modifier: Modifier = Modifier,
    expression: String,
    livePreviewResult: String?,
    finalResult: String?,
    errorMessage: String?,
    recentHistory: List<HistoryEntity>,
    formatStyle: NumberFormatStyle = NumberFormatStyle.SOUTH_ASIAN,
    onHistoryItemClick: (HistoryEntity) -> Unit,
    onCopyClick: (String) -> Unit,
    onPasteClick: () -> Unit,
    onViewAllHistoryClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var showReceiptDialog by remember { mutableStateOf(false) }

    // Determine current prominent display value
    val activeDisplayResult = finalResult ?: livePreviewResult
    val compactBadge = try {
        val clean = (activeDisplayResult ?: expression).replace(",", "").trim()
        val bd = BigDecimal(clean)
        NumberFormatter.getCompactBadge(bd, formatStyle)
    } catch (_: Exception) {
        null
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(NexoraSecondaryBg)
            .border(1.dp, NexoraBorder.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP: 3 Previous Results Area (Visible without scrolling)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT CALCULATIONS",
                    color = NexoraMutedText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
                IconButton(
                    onClick = onViewAllHistoryClick,
                    modifier = Modifier.size(24.dp).testTag("btn_view_history")
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "All History",
                        tint = NexoraSecondaryText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (recentHistory.isEmpty()) {
                Text(
                    text = "No previous calculations yet",
                    color = NexoraMutedText.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            } else {
                recentHistory.take(3).forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onHistoryItemClick(item) }
                            .padding(vertical = 2.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}. ${item.expression} =",
                            color = NexoraSecondaryText,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.result,
                            color = NexoraAccentBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // DIVIDER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(NexoraBorder.copy(alpha = 0.4f))
                .padding(vertical = 2.dp)
        )

        // MIDDLE: Current Expression line with Paste, Copy, and Share actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Scrollable Expression
            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState, reverseScrolling = true)
            ) {
                Text(
                    text = if (expression.isEmpty()) "0" else expression,
                    color = if (expression.isEmpty()) NexoraMutedText else NexoraSecondaryText,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth().testTag("txt_expression")
                )
            }

            // Quick actions: Screenshot, Share/Receipt, Paste, Copy
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val activePalette = LocalAppPalette.current
                // Quick Screenshot / Save Image Button
                IconButton(
                    onClick = {
                        val exprText = expression.ifBlank { "0" }
                        val resText = finalResult ?: livePreviewResult ?: "0"
                        if (exprText != "0" || resText != "0") {
                            val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
                            val breakdown = try {
                                NumberFormatter.getLargeNumberBreakdown(BigDecimal(resText.replace(",", "").trim()))
                            } catch (_: Exception) { null }
                            val bmp = ReceiptImageHelper.createCalculationBitmap(
                                expression = exprText,
                                result = resText,
                                dateString = dateStr,
                                southAsianWords = breakdown?.southAsianWords,
                                internationalWords = breakdown?.internationalWords,
                                palette = activePalette
                            )
                            ReceiptImageHelper.saveScreenshotToGallery(context, bmp)
                        } else {
                            Toast.makeText(context, "Please perform a calculation first", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(28.dp).testTag("btn_quick_screenshot")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Take Calculation Screenshot",
                        tint = NexoraAccentBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Share / Receipt Button
                IconButton(
                    onClick = {
                        val exprText = expression.ifBlank { "0" }
                        val resText = finalResult ?: livePreviewResult ?: "0"
                        if (exprText != "0" || resText != "0") {
                            showReceiptDialog = true
                        } else {
                            Toast.makeText(context, "Please perform a calculation first", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(28.dp).testTag("btn_share_receipt")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Calculation / Receipt",
                        tint = NexoraAccentBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onPasteClick,
                    modifier = Modifier.size(28.dp).testTag("btn_paste")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Paste",
                        tint = NexoraSecondaryText,
                        modifier = Modifier.size(15.dp)
                    )
                }

                IconButton(
                    onClick = {
                        val textToCopy = finalResult ?: livePreviewResult ?: expression
                        if (textToCopy.isNotBlank()) onCopyClick(textToCopy)
                    },
                    modifier = Modifier.size(28.dp).testTag("btn_copy")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Result",
                        tint = NexoraSecondaryText,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        // BOTTOM: Large Result / Live Preview / Error
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalAlignment = Alignment.End
        ) {
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = NexoraDanger,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("txt_error")
                )
            } else {
                val displayVal = finalResult ?: livePreviewResult ?: if (expression.isEmpty()) "0" else ""
                Text(
                    text = displayVal,
                    color = if (finalResult != null) NexoraMainText else NexoraSecondaryText.copy(alpha = 0.8f),
                    fontSize = if (displayVal.length > 10) 30.sp else 38.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("txt_result")
                )
            }

            // Compact Number Scale Badge (e.g. "≈ 1.50 Crore" or "≈ 15.00 Million")
            if (compactBadge != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NexoraPrimaryOp.copy(alpha = 0.35f),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = "≈ $compactBadge",
                        color = NexoraAccentBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }

    // CALCULATION SLIP / SHARE / SCREENSHOT DIALOG
    if (showReceiptDialog) {
        val currentExpr = expression.ifBlank { "0" }
        val currentRes = finalResult ?: livePreviewResult ?: currentExpr
        val dateString = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

        val wordsBreakdown = try {
            val clean = currentRes.replace(",", "").trim()
            val bd = BigDecimal(clean)
            NumberFormatter.getLargeNumberBreakdown(bd)
        } catch (_: Exception) {
            null
        }

        Dialog(onDismissRequest = { showReceiptDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = NexoraSecondaryBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(NexoraPrimaryOp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = NexoraAccentBlue, modifier = Modifier.size(16.dp))
                            }
                            Column {
                                Text("D. CALC RECEIPT", color = NexoraMainText, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Text(dateString, color = NexoraMutedText, fontSize = 10.sp)
                            }
                        }

                        IconButton(onClick = { showReceiptDialog = false }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = NexoraSecondaryText, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Digital Slip Card (Clean for Screenshotting)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = NexoraMainBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "CALCULATION SLIP",
                                color = NexoraMutedText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            )

                            // Expression
                            Text(
                                text = "$currentExpr =",
                                color = NexoraSecondaryText,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace
                            )

                            // Result
                            Text(
                                text = currentRes,
                                color = NexoraAccentBlue,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )

                            if (wordsBreakdown != null) {
                                HorizontalDivider(color = NexoraBorder.copy(alpha = 0.4f), thickness = 1.dp)
                                Text(
                                    text = "In Words: ${wordsBreakdown.southAsianWords}",
                                    color = NexoraMainText,
                                    fontSize = 11.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                                Text(
                                    text = "International: ${wordsBreakdown.internationalWords}",
                                    color = NexoraSecondaryText,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // Action Buttons (Screenshot PNG, Share Image, Share Text, Copy)
                    val activePalette = LocalAppPalette.current
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Save Screenshot to Gallery
                        Button(
                            onClick = {
                                val bmp = ReceiptImageHelper.createCalculationBitmap(
                                    expression = currentExpr,
                                    result = currentRes,
                                    dateString = dateString,
                                    southAsianWords = wordsBreakdown?.southAsianWords,
                                    internationalWords = wordsBreakdown?.internationalWords,
                                    palette = activePalette
                                )
                                ReceiptImageHelper.saveScreenshotToGallery(context, bmp)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NexoraAccentBlue, contentColor = NexoraMainBg),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.weight(1f).height(44.dp).testTag("btn_save_screenshot")
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Screenshot", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        // Share PNG Image via Apps
                        Button(
                            onClick = {
                                val bmp = ReceiptImageHelper.createCalculationBitmap(
                                    expression = currentExpr,
                                    result = currentRes,
                                    dateString = dateString,
                                    southAsianWords = wordsBreakdown?.southAsianWords,
                                    internationalWords = wordsBreakdown?.internationalWords,
                                    palette = activePalette
                                )
                                ReceiptImageHelper.shareReceiptImage(context, bmp)
                                showReceiptDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NexoraPrimaryOp, contentColor = NexoraMainText),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.weight(1f).height(44.dp).testTag("btn_share_image")
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Share Image", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Share Text via Apps
                        OutlinedButton(
                            onClick = {
                                val shareText = buildString {
                                    append("🧾 D. Calc Slip\n")
                                    append("Date: $dateString\n")
                                    append("Calculation: $currentExpr\n")
                                    append("Result: = $currentRes\n")
                                    if (wordsBreakdown != null) {
                                        append("In Words: ${wordsBreakdown.southAsianWords}\n")
                                    }
                                    append("\nCalculated via D. Calc")
                                }
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Share Calculation Slip")
                                context.startActivity(shareIntent)
                                showReceiptDialog = false
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NexoraAccentBlue),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.weight(1f).height(42.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Share Text", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }

                        // Copy Text
                        OutlinedButton(
                            onClick = {
                                val copyText = "$currentExpr = $currentRes ($dateString)"
                                onCopyClick(copyText)
                                Toast.makeText(context, "Calculation slip copied to clipboard!", Toast.LENGTH_SHORT).show()
                                showReceiptDialog = false
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NexoraSecondaryText),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.weight(1f).height(42.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Copy Text", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
