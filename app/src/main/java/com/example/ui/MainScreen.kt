package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.theme.*

enum class NexoraNavSection(
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    BASIC("Basic", Icons.Default.Calculate, "tab_basic"),
    LEDGER("Ledger", Icons.Default.MenuBook, "tab_ledger"),
    CONVERTER("Converter", Icons.Default.CurrencyExchange, "tab_converter"),
    BUSINESS("Business", Icons.Default.TrendingUp, "tab_business"),
    MORE("More", Icons.Default.MoreHoriz, "tab_more")
}

@Composable
fun MainScreen(
    viewModel: CalculatorViewModel = viewModel()
) {
    var currentSection by remember { mutableStateOf(NexoraNavSection.BASIC) }
    var initialMoreSubTab by remember { mutableStateOf(MoreSubTab.HISTORY) }
    var targetBusinessTab by remember { mutableStateOf(BusinessTab.IMPORT) }

    // Intercept back button to return to Basic screen if on another screen
    BackHandler(enabled = currentSection != NexoraNavSection.BASIC) {
        currentSection = NexoraNavSection.BASIC
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = NexoraMainBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            // Elegant Compact Brand Header with status bar padding
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(NexoraPrimaryOp)
                            .border(1.dp, NexoraAccentBlue, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "D",
                            color = NexoraAccentBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "D. CALC",
                        color = NexoraMainText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Active Section Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NexoraSecondaryBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NexoraBorder)
                ) {
                    Text(
                        text = currentSection.title.uppercase(),
                        color = NexoraAccentBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        },
        bottomBar = {
            // Apple/One UI style Navigation Bar properly respecting system navigation bars
            NavigationBar(
                containerColor = NexoraSecondaryBg,
                windowInsets = WindowInsets.navigationBars,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .border(
                        width = 1.dp,
                        color = NexoraBorder.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
            ) {
                NexoraNavSection.entries.forEach { section ->
                    val selected = currentSection == section
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            viewModel.triggerHaptic()
                            currentSection = section
                        },
                        icon = {
                            Icon(
                                imageVector = section.icon,
                                contentDescription = section.title,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = section.title,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NexoraMainBg,
                            selectedTextColor = NexoraAccentBlue,
                            indicatorColor = NexoraAccentBlue,
                            unselectedIconColor = NexoraSecondaryText,
                            unselectedTextColor = NexoraMutedText
                        ),
                        modifier = Modifier.testTag(section.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentSection) {
                NexoraNavSection.BASIC -> {
                    BasicCalculatorScreen(
                        viewModel = viewModel,
                        onViewHistory = {
                            initialMoreSubTab = MoreSubTab.HISTORY
                            currentSection = NexoraNavSection.MORE
                        }
                    )
                }
                NexoraNavSection.LEDGER -> {
                    LedgerScreen(viewModel = viewModel)
                }
                NexoraNavSection.CONVERTER -> {
                    CurrencyConverterScreen(viewModel = viewModel)
                }
                NexoraNavSection.BUSINESS -> {
                    BusinessCalculatorScreen(viewModel = viewModel, initialTab = targetBusinessTab)
                }
                NexoraNavSection.MORE -> {
                    MoreScreen(
                        viewModel = viewModel,
                        initialSubTab = initialMoreSubTab,
                        onNavigateToBusiness = { targetTab ->
                            targetBusinessTab = targetTab
                            currentSection = NexoraNavSection.BUSINESS
                        }
                    )
                }
            }
        }
    }
}
