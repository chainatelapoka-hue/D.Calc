package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.local.HistoryEntity
import com.example.engine.NumberFormatStyle
import com.example.ui.CalculatorViewModel
import com.example.ui.components.ButtonRole
import com.example.ui.components.DisplayPanel
import com.example.ui.components.NexoraButton
import com.example.ui.theme.NexoraMainBg

@Composable
fun BasicCalculatorScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier,
    onViewHistory: () -> Unit
) {
    val expression by viewModel.expression.collectAsState()
    val livePreview by viewModel.livePreviewResult.collectAsState()
    val finalResult by viewModel.finalResult.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val recentHistory by viewModel.recentHistory.collectAsState()
    val settings by viewModel.userSettings.collectAsState()

    val btnHeight: Dp = 50.dp
    val spacing: Dp = 8.dp

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NexoraMainBg)
            .padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP: Display Panel (with 3 previous results visible)
        DisplayPanel(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            expression = expression,
            livePreviewResult = livePreview,
            finalResult = finalResult,
            errorMessage = errorMessage,
            recentHistory = recentHistory,
            formatStyle = settings.formatStyle,
            onHistoryItemClick = { item -> viewModel.reuseHistory(item) },
            onCopyClick = { text -> viewModel.copyToClipboard(text) },
            onPasteClick = { viewModel.pasteToExpression() },
            onViewAllHistoryClick = onViewHistory
        )

        Spacer(modifier = Modifier.height(10.dp))

        // BOTTOM: Compact Keypad Grid (5 Rows x 4 Columns)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            // ROW 1: C, ⌫, %, ÷
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                NexoraButton(
                    modifier = Modifier.weight(1f),
                    text = "C",
                    role = ButtonRole.SECONDARY,
                    height = btnHeight,
                    onLongClick = { viewModel.onClear() },
                    onClick = { viewModel.onClear() }
                )
                NexoraButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.AutoMirrored.Filled.Backspace,
                    text = "backspace",
                    role = ButtonRole.SECONDARY,
                    height = btnHeight,
                    onLongClick = { viewModel.onClear() },
                    onClick = { viewModel.onBackspace() }
                )
                NexoraButton(
                    modifier = Modifier.weight(1f),
                    text = "%",
                    role = ButtonRole.SECONDARY,
                    height = btnHeight,
                    onClick = { viewModel.onOperator("%") }
                )
                NexoraButton(
                    modifier = Modifier.weight(1f),
                    text = "÷",
                    role = ButtonRole.PRIMARY_OP,
                    height = btnHeight,
                    onClick = { viewModel.onOperator("÷") }
                )
            }

            // ROW 2: 7, 8, 9, ×
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                NexoraButton(
                    modifier = Modifier.weight(1f),
                    text = "7",
                    role = ButtonRole.NUMBER,
                    height = btnHeight,
                    onClick = { viewModel.onDigit("7") }
                )
                NexoraButton(
                    modifier = Modifier.weight(1f),
                    text = "8",
                    role = ButtonRole.NUMBER,
                    height = btnHeight,
                    onClick = { viewModel.onDigit("8") }
                )
                NexoraButton(
                    modifier = Modifier.weight(1f),
                    text = "9",
                    role = ButtonRole.NUMBER,
                    height = btnHeight,
                    onClick = { viewModel.onDigit("9") }
                )
                NexoraButton(
                    modifier = Modifier.weight(1f),
                    text = "×",
                    role = ButtonRole.PRIMARY_OP,
                    height = btnHeight,
                    onClick = { viewModel.onOperator("×") }
                )
            }

            // ROW 3: 4, 5, 6, -
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                NexoraButton(
                    modifier = Modifier.weight(1f),
                    text = "4",
                    role = ButtonRole.NUMBER,
                    height = btnHeight,
                    onClick = { viewModel.onDigit("4") }
                )
                NexoraButton(
                    modifier = Modifier.weight(1f),
                    text = "5",
                    role = ButtonRole.NUMBER,
                    height = btnHeight,
                    onClick = { viewModel.onDigit("5") }
                )
                NexoraButton(
                    modifier = Modifier.weight(1f),
                    text = "6",
                    role = ButtonRole.NUMBER,
                    height = btnHeight,
                    onClick = { viewModel.onDigit("6") }
                )
                NexoraButton(
                    modifier = Modifier.weight(1f),
                    text = "-",
                    role = ButtonRole.PRIMARY_OP,
                    height = btnHeight,
                    onClick = { viewModel.onOperator("-") }
                )
            }

            // ROW 4: 1, 2, 3, +
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                NexoraButton(
                    modifier = Modifier.weight(1f),
                    text = "1",
                    role = ButtonRole.NUMBER,
                    height = btnHeight,
                    onClick = { viewModel.onDigit("1") }
                )
                NexoraButton(
                    modifier = Modifier.weight(1f),
                    text = "2",
                    role = ButtonRole.NUMBER,
                    height = btnHeight,
                    onClick = { viewModel.onDigit("2") }
                )
                NexoraButton(
                    modifier = Modifier.weight(1f),
                    text = "3",
                    role = ButtonRole.NUMBER,
                    height = btnHeight,
                    onClick = { viewModel.onDigit("3") }
                )
                NexoraButton(
                    modifier = Modifier.weight(1f),
                    text = "+",
                    role = ButtonRole.PRIMARY_OP,
                    height = btnHeight,
                    onClick = { viewModel.onOperator("+") }
                )
            }

            // ROW 5: ±, 0, ., =
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                NexoraButton(
                    modifier = Modifier.weight(1f),
                    text = "±",
                    role = ButtonRole.SECONDARY,
                    height = btnHeight,
                    onClick = { viewModel.onFunction("±") }
                )
                NexoraButton(
                    modifier = Modifier.weight(1f),
                    text = "0",
                    role = ButtonRole.NUMBER,
                    height = btnHeight,
                    onClick = { viewModel.onDigit("0") }
                )
                NexoraButton(
                    modifier = Modifier.weight(1f),
                    text = ".",
                    role = ButtonRole.NUMBER,
                    height = btnHeight,
                    onClick = { viewModel.onDigit(".") }
                )
                NexoraButton(
                    modifier = Modifier.weight(1f),
                    text = "=",
                    role = ButtonRole.EQUAL,
                    height = btnHeight,
                    onClick = { viewModel.onEqual("BASIC") }
                )
            }
        }
    }
}
