package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.printer.EscPosHelper

@Composable
fun CalculatorKeypad(
    displayValue: String,
    expression: String,
    onDigit: (String) -> Unit,
    onOperator: (String) -> Unit,
    onEquals: () -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onDenomination: (Long) -> Unit,
    onAddToCart: (itemName: String) -> Unit,
    onQuickPay: () -> Unit,
    modifier: Modifier = Modifier
) {
    var customItemName by remember { mutableStateOf("") }
    val displayAmount = displayValue.toLongOrNull() ?: 0L

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("calculator_keypad_container"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Display Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.End
            ) {
                if (expression.isNotBlank()) {
                    Text(
                        text = expression,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = EscPosHelper.formatRupiah(displayAmount),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Quick Rupiah Denomination Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                5000L to "+5rb",
                10000L to "+10rb",
                20000L to "+20rb",
                50000L to "+50rb",
                100000L to "+100rb"
            ).forEach { (amt, label) ->
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onDenomination(amt) }
                        .testTag("denom_chip_$label"),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Keypad Grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row 1: C, Backspace, ÷, ×
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalcButton(
                    text = "C",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.errorContainer,
                    textColor = MaterialTheme.colorScheme.onErrorContainer,
                    onClick = onClear
                )
                CalcIconButton(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    icon = Icons.AutoMirrored.Filled.Backspace,
                    onClick = onBackspace
                )
                CalcButton(
                    text = "÷",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    textColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = { onOperator("÷") }
                )
                CalcButton(
                    text = "×",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    textColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = { onOperator("×") }
                )
            }

            // Row 2: 7, 8, 9, -
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalcButton(text = "7", modifier = Modifier.weight(1f), onClick = { onDigit("7") })
                CalcButton(text = "8", modifier = Modifier.weight(1f), onClick = { onDigit("8") })
                CalcButton(text = "9", modifier = Modifier.weight(1f), onClick = { onDigit("9") })
                CalcButton(
                    text = "-",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    textColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = { onOperator("-") }
                )
            }

            // Row 3: 4, 5, 6, +
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalcButton(text = "4", modifier = Modifier.weight(1f), onClick = { onDigit("4") })
                CalcButton(text = "5", modifier = Modifier.weight(1f), onClick = { onDigit("5") })
                CalcButton(text = "6", modifier = Modifier.weight(1f), onClick = { onDigit("6") })
                CalcButton(
                    text = "+",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    textColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = { onOperator("+") }
                )
            }

            // Row 4: 1, 2, 3, =
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalcButton(text = "1", modifier = Modifier.weight(1f), onClick = { onDigit("1") })
                CalcButton(text = "2", modifier = Modifier.weight(1f), onClick = { onDigit("2") })
                CalcButton(text = "3", modifier = Modifier.weight(1f), onClick = { onDigit("3") })
                CalcButton(
                    text = "=",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primary,
                    textColor = MaterialTheme.colorScheme.onPrimary,
                    onClick = onEquals
                )
            }

            // Row 5: 0, 00, 000
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalcButton(text = "0", modifier = Modifier.weight(1f), onClick = { onDigit("0") })
                CalcButton(text = "00", modifier = Modifier.weight(1f), onClick = { onDigit("00") })
                CalcButton(text = "000", modifier = Modifier.weight(1.5f), onClick = { onDigit("000") })
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Buttons: "+ Tambah ke Keranjang" & "Bayar Langsung"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { onAddToCart(customItemName) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("add_calc_to_cart_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tambah Struk", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }

            Button(
                onClick = onQuickPay,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("quick_pay_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Bayar Cepat", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun CalcButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("calc_btn_$text"),
        color = color,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CalcIconButton(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("calc_icon_btn"),
        color = color,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
