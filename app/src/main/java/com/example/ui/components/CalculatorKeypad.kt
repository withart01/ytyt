package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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

/**
 * Ultra-responsive POS Calculator optimized specifically for entry-level hardware (Samsung J2).
 * Architectural highlights:
 * 1. Zero dynamic shadows / elevation (Flat design = 60fps on Mali-400 GPU).
 * 2. Keypad buttons are completely isolated from display recompositions.
 * 3. Compact dimensions tailored for 4.7" - 5.0" qHD (540x960) screens.
 */
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
    var showItemNameField by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("calculator_keypad_container"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Isolated Display (Only this recomposes when displayValue changes)
        CalculatorDisplay(
            displayValue = displayValue,
            expression = expression
        )

        // Item Name Bar (Compact)
        ItemNameBar(
            customItemName = customItemName,
            showItemNameField = showItemNameField,
            onToggleField = { showItemNameField = !showItemNameField },
            onClearName = { customItemName = "" }
        )

        AnimatedVisibility(visible = showItemNameField) {
            ItemNameInputSection(
                customItemName = customItemName,
                onNameChange = { customItemName = it }
            )
        }

        // Quick Denomination Chips (Isolated)
        QuickDenominationChips(onDenomination = onDenomination)

        Spacer(modifier = Modifier.height(2.dp))

        // Tactile Keypad Grid (Isolated from displayValue recompositions!)
        KeypadButtonGrid(
            onDigit = onDigit,
            onOperator = onOperator,
            onEquals = onEquals,
            onClear = onClear,
            onBackspace = onBackspace
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Action Buttons: Tambah Struk & Bayar Cepat
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onAddToCart(customItemName) },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("add_calc_to_cart_btn"),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tambah Struk", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }

            Button(
                onClick = onQuickPay,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("quick_pay_btn"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0F766E)
                )
            ) {
                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Bayar Cepat", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

/**
 * Isolated display component. Renders flat solid background without gradient or dynamic shadows.
 */
@Composable
private fun CalculatorDisplay(
    displayValue: String,
    expression: String
) {
    val displayAmount = remember(displayValue) { displayValue.toLongOrNull() ?: 0L }
    val formattedRupiah = remember(displayAmount) { EscPosHelper.formatRupiah(displayAmount) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0F172A), // Solid dark slate (no gradient overhead)
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF134E4A)
                ) {
                    Text(
                        text = "TERMINAL KASIR",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2DD4BF),
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }

                if (expression.isNotBlank()) {
                    Text(
                        text = expression,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "IDR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = formattedRupiah,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF34D399), // Neon mint
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}

@Composable
private fun ItemNameBar(
    customItemName: String,
    showItemNameField: Boolean,
    onToggleField: () -> Unit,
    onClearName: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onToggleField)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (customItemName.isBlank()) "+ Beri Nama Item (Opsional)" else "Item: $customItemName",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (customItemName.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.clickable(onClick = onClearName)
            ) {
                Text(
                    text = "Hapus",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ItemNameInputSection(
    customItemName: String,
    onNameChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
    ) {
        OutlinedTextField(
            value = customItemName,
            onValueChange = onNameChange,
            placeholder = { Text("Contoh: Nasi Goreng, Es Teh, Jasa", fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Menu Khusus", "Jasa", "Tambahan", "Paket Custom").forEach { preset ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.clickable { onNameChange(preset) }
                ) {
                    Text(
                        text = preset,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickDenominationChips(
    onDenomination: (Long) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onDenomination(amt) }
                    .testTag("denom_chip_$label"),
                color = Color(0xFFCCFBF1), // Solid teal-50
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF99F6E4))
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F766E)
                )
            }
        }
    }
}

/**
 * Keypad grid component. Does not depend on displayValue, so it does NOT recompose on digit input!
 * Uses lightweight flat buttons with single-pass rendering.
 */
@Composable
private fun KeypadButtonGrid(
    onDigit: (String) -> Unit,
    onOperator: (String) -> Unit,
    onEquals: () -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Row 1: C, Backspace, ÷, ×
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FlatCalcButton(
                text = "C",
                modifier = Modifier.weight(1f),
                bgColor = Color(0xFFFEE2E2),
                textColor = Color(0xFFDC2626),
                borderColor = Color(0xFFFECACA),
                onClick = onClear
            )
            FlatCalcIconButton(
                modifier = Modifier.weight(1f),
                bgColor = Color(0xFFF1F5F9),
                icon = Icons.AutoMirrored.Filled.Backspace,
                onClick = onBackspace
            )
            FlatCalcButton(
                text = "÷",
                modifier = Modifier.weight(1f),
                bgColor = Color(0xFFFEF3C7),
                textColor = Color(0xFFB45309),
                borderColor = Color(0xFFFDE68A),
                onClick = { onOperator("÷") }
            )
            FlatCalcButton(
                text = "×",
                modifier = Modifier.weight(1f),
                bgColor = Color(0xFFFEF3C7),
                textColor = Color(0xFFB45309),
                borderColor = Color(0xFFFDE68A),
                onClick = { onOperator("×") }
            )
        }

        // Row 2: 7, 8, 9, -
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FlatCalcButton(text = "7", modifier = Modifier.weight(1f), onClick = { onDigit("7") })
            FlatCalcButton(text = "8", modifier = Modifier.weight(1f), onClick = { onDigit("8") })
            FlatCalcButton(text = "9", modifier = Modifier.weight(1f), onClick = { onDigit("9") })
            FlatCalcButton(
                text = "-",
                modifier = Modifier.weight(1f),
                bgColor = Color(0xFFFEF3C7),
                textColor = Color(0xFFB45309),
                borderColor = Color(0xFFFDE68A),
                onClick = { onOperator("-") }
            )
        }

        // Row 3: 4, 5, 6, +
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FlatCalcButton(text = "4", modifier = Modifier.weight(1f), onClick = { onDigit("4") })
            FlatCalcButton(text = "5", modifier = Modifier.weight(1f), onClick = { onDigit("5") })
            FlatCalcButton(text = "6", modifier = Modifier.weight(1f), onClick = { onDigit("6") })
            FlatCalcButton(
                text = "+",
                modifier = Modifier.weight(1f),
                bgColor = Color(0xFFFEF3C7),
                textColor = Color(0xFFB45309),
                borderColor = Color(0xFFFDE68A),
                onClick = { onOperator("+") }
            )
        }

        // Row 4: 1, 2, 3, =
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FlatCalcButton(text = "1", modifier = Modifier.weight(1f), onClick = { onDigit("1") })
            FlatCalcButton(text = "2", modifier = Modifier.weight(1f), onClick = { onDigit("2") })
            FlatCalcButton(text = "3", modifier = Modifier.weight(1f), onClick = { onDigit("3") })
            FlatCalcButton(
                text = "=",
                modifier = Modifier.weight(1f),
                bgColor = Color(0xFF0F766E),
                textColor = Color.White,
                borderColor = Color(0xFF0D9488),
                onClick = onEquals
            )
        }

        // Row 5: 0, 00, 000
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FlatCalcButton(text = "0", modifier = Modifier.weight(1f), onClick = { onDigit("0") })
            FlatCalcButton(text = "00", modifier = Modifier.weight(1f), onClick = { onDigit("00") })
            FlatCalcButton(text = "000", modifier = Modifier.weight(1.5f), onClick = { onDigit("000") })
        }
    }
}

/**
 * Ultra-fast Flat Button.
 * Zero shadow elevation, zero multi-pass GPU shaders.
 */
@Composable
private fun FlatCalcButton(
    text: String,
    modifier: Modifier = Modifier,
    bgColor: Color = Color(0xFFF8FAFC),
    textColor: Color = Color(0xFF1E293B),
    borderColor: Color = Color(0xFFCBD5E1),
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag("calc_btn_$text"),
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FlatCalcIconButton(
    modifier: Modifier = Modifier,
    bgColor: Color = Color(0xFFF1F5F9),
    borderColor: Color = Color(0xFFCBD5E1),
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag("calc_icon_btn"),
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF475569),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
