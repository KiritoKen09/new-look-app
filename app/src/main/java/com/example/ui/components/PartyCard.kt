package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Party
import com.example.data.model.PartyType
import com.example.ui.theme.CreditRed
import com.example.ui.theme.DebitGreen
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun PartyCard(
    party: Party,
    currencySymbol: String,
    onClick: () -> Unit,
    onRemindClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fmt = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 0
    }
    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    val initials = party.name.trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "K" }

    val isPositive = party.balance > 0.0
    val isNegative = party.balance < 0.0
    val amountColor = when {
        isPositive -> CreditRed // In Khatabook: Customer owes you (You'll Get) -> Red / Urgent or Green depending on convention; let's label clearly!
        isNegative -> DebitGreen
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("party_card_${party.id}")
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Initials Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (party.type == PartyType.CUSTOMER)
                                Color(0xFFE3F2FD)
                            else
                                Color(0xFFE0F2F1)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (party.type == PartyType.CUSTOMER)
                            Color(0xFF1565C0)
                        else
                            Color(0xFF00695C)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Party Name & Last Updated Subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = party.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = buildString {
                            append(if (party.type == PartyType.CUSTOMER) "Customer" else "Supplier")
                            append(" • ")
                            append(dateFormat.format(Date(party.updatedAt)))
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right Column: Amount + Remind link (Khatabook style)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$currencySymbol${fmt.format(abs(party.balance))}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isPositive) DebitGreen else if (isNegative) CreditRed else amountColor
                    )
                    if (isPositive) {
                        Text(
                            text = "REMIND >",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1565C0),
                            textDecoration = TextDecoration.None,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable(onClick = onRemindClick)
                                .padding(top = 2.dp, bottom = 2.dp, start = 4.dp)
                                .testTag("remind_party_${party.id}")
                        )
                    } else {
                        Text(
                            text = if (isNegative) "You'll Give" else "Settled",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 66.dp)
            )
        }
    }
}
