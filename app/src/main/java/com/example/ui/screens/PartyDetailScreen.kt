package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessProfile
import com.example.data.model.Party
import com.example.data.model.PaymentMode
import com.example.data.model.TransactionEntry
import com.example.data.model.TransactionType
import com.example.ui.components.TransactionItem
import com.example.ui.dialogs.AddTransactionDialog
import com.example.ui.theme.CreditRed
import com.example.ui.theme.DebitGreen
import com.example.util.MonthlyReportPdfGenerator
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyDetailScreen(
    party: Party,
    transactions: List<TransactionEntry>,
    profile: BusinessProfile,
    onBack: () -> Unit,
    onAddTransaction: (
        amount: Double,
        type: TransactionType,
        paymentMode: PaymentMode,
        description: String,
        billNumber: String
    ) -> Unit,
    onDeleteTransaction: (TransactionEntry) -> Unit,
    onDeleteParty: (Party) -> Unit,
    onSendReminder: (Party) -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val currency = profile.currencySymbol.ifBlank { "₹" }
    val fmt = NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }

    var showTxDialogType by remember { mutableStateOf<TransactionType?>(null) }

    val isReceivable = party.balance > 0
    val isPayable = party.balance < 0
    val balanceColor = when {
        isReceivable -> DebitGreen
        isPayable -> CreditRed
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = party.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (party.phone.isNotBlank()) "${party.type.name} • ${party.phone}" else party.type.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFBBDEFB)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("party_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (party.phone.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${party.phone}"))
                                runCatching { context.startActivity(dial) }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Call Party",
                                tint = Color.White
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            val pdf = MonthlyReportPdfGenerator.generateMonthlyReportPdf(
                                context = context,
                                profile = profile,
                                parties = listOf(party),
                                transactions = transactions,
                                periodLabel = "Party Statement - ${party.name}"
                            )
                            if (pdf != null) {
                                MonthlyReportPdfGenerator.sharePdfFile(context, pdf, "Statement - ${party.name}")
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Share Party Statement PDF",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { onDeleteParty(party) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Party",
                            tint = Color(0xFFFFCDD2)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1565C0)
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { showTxDialogType = TransactionType.YOU_GAVE },
                        colors = ButtonDefaults.buttonColors(containerColor = CreditRed),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("you_gave_button")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "You Gave")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("YOU GAVE $currency", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showTxDialogType = TransactionType.YOU_GOT },
                        colors = ButtonDefaults.buttonColors(containerColor = DebitGreen),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("you_got_button")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = "You Got")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("YOU GOT $currency", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = when {
                                        isReceivable -> "You Will Get from ${party.name}"
                                        isPayable -> "You Will Give to ${party.name}"
                                        else -> "Account Settled"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$currency${fmt.format(abs(party.balance))}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = balanceColor
                                )
                            }

                            if (isReceivable) {
                                FilledTonalButton(
                                    onClick = { onSendReminder(party) },
                                    modifier = Modifier.testTag("party_detail_remind_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = "Remind",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Remind")
                                }
                            }
                        }

                        if (party.address.isNotBlank() || party.gstin.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = listOfNotNull(
                                    party.address.takeIf { it.isNotBlank() },
                                    party.gstin.takeIf { it.isNotBlank() }?.let { "GSTIN: $it" }
                                ).joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Entries (${transactions.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            if (transactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No transactions yet. Use 'YOU GAVE' or 'YOU GOT' below.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(transactions, key = { it.id }) { entry ->
                    TransactionItem(
                        entry = entry,
                        currencySymbol = currency,
                        showPartyName = false,
                        onDeleteClick = { onDeleteTransaction(entry) }
                    )
                }
            }
        }
    }

    if (showTxDialogType != null) {
        AddTransactionDialog(
            partyName = party.name,
            initialType = showTxDialogType!!,
            currencySymbol = currency,
            onDismiss = { showTxDialogType = null },
            onSave = { amt, type, mode, desc, bill ->
                onAddTransaction(amt, type, mode, desc, bill)
                showTxDialogType = null
            }
        )
    }
}
