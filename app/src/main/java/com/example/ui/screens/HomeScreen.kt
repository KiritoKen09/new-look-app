package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessProfile
import com.example.data.model.Party
import com.example.ui.components.PartyCard
import com.example.ui.components.SummaryCards
import com.example.ui.viewmodel.PartyFilter
import com.example.util.AppStrings
import kotlin.math.abs

@Composable
fun HomeScreen(
    profile: BusinessProfile,
    allParties: List<Party>,
    filteredParties: List<Party>,
    searchQuery: String,
    selectedFilter: PartyFilter,
    strings: AppStrings,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (PartyFilter) -> Unit,
    onPartyClick: (Party) -> Unit,
    onRemindParty: (Party) -> Unit,
    onAddPartyClick: () -> Unit,
    onQuickEntryClick: () -> Unit,
    onGenerateBillClick: () -> Unit,
    onQrStandeeClick: () -> Unit,
    onPdfReportClick: () -> Unit,
    onBusinessCardClick: () -> Unit,
    onCloudSyncClick: () -> Unit,
    onImportCsvClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onSetupStoreDetailsClick: () -> Unit = {},
    onShowIntroGuideClick: () -> Unit = {}
) {
    val totalGet = allParties.filter { it.balance > 0 }.sumOf { it.balance }
    val totalGive = allParties.filter { it.balance < 0 }.sumOf { abs(it.balance) }
    val currency = profile.currencySymbol.ifBlank { "₹" }

    val isOwnerProfileIncomplete = profile.businessName.isBlank() ||
        profile.ownerName.isBlank() ||
        profile.phone.isBlank() ||
        profile.upiId.isBlank()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddPartyClick,
                icon = {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Add Customer"
                    )
                },
                text = {
                    Text(
                        text = "+ ADD CUSTOMER",
                        fontWeight = FontWeight.Bold
                    )
                },
                containerColor = Color(0xFFC2185B), // Classic Khatabook magenta/crimson action button
                contentColor = Color.White,
                shape = RoundedCornerShape(50),
                modifier = Modifier.testTag("add_party_fab")
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                // Khatabook Style Clean Solid Blue Header + Summary Card
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1565C0))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Column {
                            // Top Store Name Bar (Tap to edit store details)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable(onClick = onSetupStoreDetailsClick)
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Storefront,
                                        contentDescription = "Store",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = profile.businessName.ifBlank { "Tap to Set Store Name" },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Edit Store Profile",
                                        tint = Color(0xFFBBDEFB),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = onGenerateBillClick,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("header_bill_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = "Create Bill",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = onLanguageClick,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("language_selector_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = "Change Language",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = onCloudSyncClick,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("cloud_sync_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = "Cloud Backup",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = onShowIntroGuideClick,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("app_guide_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HelpOutline,
                                        contentDescription = "How to Use App",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            SummaryCards(
                                youWillGet = totalGet,
                                youWillGive = totalGive,
                                currencySymbol = currency,
                                youWillGetLabel = strings.youWillGet,
                                youWillGiveLabel = strings.youWillGive,
                                onViewReportClick = onPdfReportClick,
                                onShowQrClick = onQrStandeeClick
                            )
                        }
                    }
                }

                // Store Owner Details Prompt Card (Shown when owner hasn't filled out name, phone, address, or UPI)
                if (isOwnerProfileIncomplete) {
                    item {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .clickable(onClick = onSetupStoreDetailsClick)
                                .testTag("owner_profile_prompt_banner")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Complete Your Store Owner Profile",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE65100)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Tap to input store owner name, address, phone & UPI ID for bills and QR.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF5D4037)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(onClick = onSetupStoreDetailsClick) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Set Up",
                                        tint = Color(0xFFE65100),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Set Up",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE65100)
                                    )
                                }
                            }
                        }
                    }
                }

                // Search & Filter Bar
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text(strings.searchPlaceholder) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchQueryChange("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.background,
                                unfocusedContainerColor = MaterialTheme.colorScheme.background
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("party_search_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            FilterChip(
                                selected = selectedFilter == PartyFilter.ALL,
                                onClick = { onFilterChange(PartyFilter.ALL) },
                                label = { Text("${strings.allParties} (${allParties.size})") }
                            )
                            FilterChip(
                                selected = selectedFilter == PartyFilter.CUSTOMERS,
                                onClick = { onFilterChange(PartyFilter.CUSTOMERS) },
                                label = { Text(strings.customers) }
                            )
                            FilterChip(
                                selected = selectedFilter == PartyFilter.SUPPLIERS,
                                onClick = { onFilterChange(PartyFilter.SUPPLIERS) },
                                label = { Text(strings.suppliers) }
                            )
                            FilterChip(
                                selected = selectedFilter == PartyFilter.RECEIVABLES,
                                onClick = { onFilterChange(PartyFilter.RECEIVABLES) },
                                label = { Text("You'll Get") }
                            )
                            FilterChip(
                                selected = selectedFilter == PartyFilter.PAYABLES,
                                onClick = { onFilterChange(PartyFilter.PAYABLES) },
                                label = { Text("You'll Give") }
                            )
                        }
                    }
                }

                if (filteredParties.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 20.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No customers or suppliers found",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap '+ ADD CUSTOMER' below to add a party to your Khata.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(filteredParties, key = { it.id }) { party ->
                        PartyCard(
                            party = party,
                            currencySymbol = currency,
                            onClick = { onPartyClick(party) },
                            onRemindClick = { onRemindParty(party) }
                        )
                    }
                }
            }
        }
    }
}
