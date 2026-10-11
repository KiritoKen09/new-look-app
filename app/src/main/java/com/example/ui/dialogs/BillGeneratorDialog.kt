package com.example.ui.dialogs

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessProfile
import com.example.data.model.Party
import com.example.data.model.PaymentMode
import com.example.data.model.TransactionType
import java.text.NumberFormat
import java.util.Locale

data class BillLineItem(
    val name: String,
    val qty: Int,
    val rate: Double
) {
    val total: Double get() = qty * rate
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillGeneratorDialog(
    profile: BusinessProfile,
    parties: List<Party>,
    onDismiss: () -> Unit,
    onRecordBillToKhata: (
        partyId: Long?,
        amount: Double,
        billNumber: String,
        itemsSummary: String,
        isCreditSale: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    val currency = profile.currencySymbol.ifBlank { "₹" }
    val fmt = NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 2 }

    var selectedParty by remember { mutableStateOf(parties.firstOrNull()) }
    var partyExpanded by remember { mutableStateOf(false) }
    var billNumber by remember { mutableStateOf("INV-${(1000..9999).random()}") }
    var gstPercent by remember { mutableStateOf(5) }
    var isCreditSale by remember { mutableStateOf(true) }

    val lineItems = remember {
        mutableStateListOf(
            BillLineItem("Basmati Rice 25kg Bag", 1, 1450.0),
            BillLineItem("Refined Sunflower Oil 5L", 2, 720.0)
        )
    }

    var newItemName by remember { mutableStateOf("") }
    var newItemQty by remember { mutableStateOf("1") }
    var newItemRate by remember { mutableStateOf("") }

    val subTotal = lineItems.sumOf { it.total }
    val gstAmount = subTotal * (gstPercent / 100.0)
    val grandTotal = subTotal + gstAmount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "GST Bill & Invoice Maker",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = billNumber,
                        onValueChange = { billNumber = it },
                        label = { Text("Invoice No.") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (parties.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = partyExpanded,
                        onExpandedChange = { partyExpanded = !partyExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedParty?.name ?: "Walk-in Customer",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Bill To Party") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = partyExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = partyExpanded,
                            onDismissRequest = { partyExpanded = false }
                        ) {
                            parties.forEach { party ->
                                DropdownMenuItem(
                                    text = { Text(party.name) },
                                    onClick = {
                                        selectedParty = party
                                        partyExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isCreditSale,
                        onClick = { isCreditSale = true },
                        label = { Text("Credit (Add to Khata)") }
                    )
                    FilterChip(
                        selected = !isCreditSale,
                        onClick = { isCreditSale = false },
                        label = { Text("Paid Cash (Cashbook)") }
                    )
                }

                Text(
                    text = "Bill Items (${lineItems.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                lineItems.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${item.qty} x $currency${fmt.format(item.rate)}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Text(
                            text = "$currency${fmt.format(item.total)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { lineItems.removeAt(index) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove Item"
                            )
                        }
                    }
                }

                // Add New Item Row
                OutlinedTextField(
                    value = newItemName,
                    onValueChange = { newItemName = it },
                    label = { Text("New Item Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newItemQty,
                        onValueChange = { newItemQty = it },
                        label = { Text("Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                    OutlinedTextField(
                        value = newItemRate,
                        onValueChange = { newItemRate = it },
                        label = { Text("Rate ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                    FilledTonalButton(
                        onClick = {
                            val q = newItemQty.toIntOrNull() ?: 1
                            val r = newItemRate.toDoubleOrNull() ?: 0.0
                            if (newItemName.isNotBlank() && r > 0.0) {
                                lineItems.add(BillLineItem(newItemName.trim(), q, r))
                                newItemName = ""
                                newItemQty = "1"
                                newItemRate = ""
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                    }
                }

                // GST Selector
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("GST:", style = MaterialTheme.typography.labelMedium)
                    listOf(0, 5, 12, 18).forEach { pct ->
                        FilterChip(
                            selected = gstPercent == pct,
                            onClick = { gstPercent = pct },
                            label = { Text("$pct%") }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal:", style = MaterialTheme.typography.bodySmall)
                            Text("$currency${fmt.format(subTotal)}", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("GST ($gstPercent%):", style = MaterialTheme.typography.bodySmall)
                            Text("$currency${fmt.format(gstAmount)}", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Grand Total:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "$currency${fmt.format(grandTotal)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                FilledTonalButton(
                    onClick = {
                        val summaryText = buildString {
                            appendLine("🧾 *TAX INVOICE - ${profile.businessName}*")
                            if (profile.gstin.isNotBlank()) appendLine("GSTIN: ${profile.gstin}")
                            appendLine("Invoice No: $billNumber")
                            appendLine("Customer: ${selectedParty?.name ?: "Walk-in"}")
                            appendLine("--------------------------------")
                            lineItems.forEach {
                                appendLine("• ${it.name} (${it.qty} x $currency${fmt.format(it.rate)}) = $currency${fmt.format(it.total)}")
                            }
                            appendLine("--------------------------------")
                            appendLine("Subtotal: $currency${fmt.format(subTotal)}")
                            appendLine("GST ($gstPercent%): $currency${fmt.format(gstAmount)}")
                            appendLine("*Grand Total: $currency${fmt.format(grandTotal)}*")
                            if (profile.upiId.isNotBlank()) appendLine("Pay via UPI: ${profile.upiId}")
                        }
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, summaryText)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Invoice"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share Bill")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Invoice on WhatsApp")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (grandTotal > 0.0) {
                        val summary = lineItems.joinToString(", ") { "${it.name} x${it.qty}" }
                        onRecordBillToKhata(
                            selectedParty?.id,
                            grandTotal,
                            billNumber,
                            summary,
                            isCreditSale
                        )
                    }
                },
                enabled = lineItems.isNotEmpty(),
                modifier = Modifier.testTag("save_bill_button")
            ) {
                Text("Save to Ledger")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
