package com.example.ui.screens
import coil.compose.SubcomposeAsyncImage
import androidx.compose.ui.layout.ContentScale

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Item
import com.example.ui.GroupStat
import com.example.ui.StockViewModel
import com.example.ui.components.InputPromptDialog
import com.example.ui.components.SimpleConfirmDialog
import com.example.ui.components.ValuePickerDialog
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.DangerRed

@Composable
fun ItemsScreen(
    viewModel: StockViewModel,
    onOpenItemForm: (Item?) -> Unit
) {
    val items by viewModel.filteredItems.collectAsState()
    val allItemsList by viewModel.allItems.collectAsState()
    val groupStats by viewModel.groupStats.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val groupBy by viewModel.groupBy.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val inStockOnly by viewModel.inStockOnly.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val selectedIds by viewModel.selectedItemIds.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()

    var showSortDialog by remember { mutableStateOf(false) }
    var showGroupByDialog by remember { mutableStateOf(false) }
    var showBulkTypeDialog by remember { mutableStateOf(false) }
    var showBulkUnitDialog by remember { mutableStateOf(false) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                val normalized = com.example.util.ItemSearchMatcher.normalizeVoiceInput(spokenText)
                viewModel.searchQuery.value = normalized
            }
        }
    }

    val isInGroup = selectedGroup != null

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isInGroup) {
                    IconButton(onClick = { viewModel.selectedGroup.value = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                Text(
                    text = if (isInGroup) selectedGroup ?: "Group" else "Item List",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1
                )

                Row {
                    IconButton(onClick = { viewModel.openBillFlow() }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Bill flow", tint = BrandBlue)
                    }
                    IconButton(onClick = { onOpenItemForm(null) }) {
                        Icon(Icons.Default.Add, contentDescription = "Add item", tint = BrandBlue)
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("Search name, SKU, brand, size…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault())
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak item name, SKU, or type to search...")
                                }
                                try {
                                    speechRecognizerLauncher.launch(intent)
                                } catch (e: Exception) {
                                    viewModel.showToast("Voice recognition not available")
                                }
                            }
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice search", tint = BrandBlue)
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = BrandBlue
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isInGroup) {
                    FilterChip(
                        selected = groupBy != "none",
                        onClick = { showGroupByDialog = true },
                        label = {
                            val label = when (groupBy) {
                                "none" -> "Group by"
                                else -> "Group: " + groupBy.replaceFirstChar { it.uppercase() }
                            }
                            Text(label)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandBlue.copy(alpha = 0.15f),
                            selectedLabelColor = BrandBlue
                        )
                    )
                }

                FilterChip(
                    selected = inStockOnly,
                    onClick = { viewModel.inStockOnly.value = !inStockOnly },
                    label = { Text("In stock") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandBlue.copy(alpha = 0.15f),
                        selectedLabelColor = BrandBlue
                    )
                )

                if (groupBy == "none" || isInGroup) {
                    FilterChip(
                        selected = isSelectionMode,
                        onClick = {
                            val newMode = !isSelectionMode
                            viewModel.isSelectionMode.value = newMode
                            if (!newMode) viewModel.selectedItemIds.value = emptySet()
                        },
                        label = { Text(if (isSelectionMode) "Cancel" else "Select") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandBlue.copy(alpha = 0.15f),
                            selectedLabelColor = BrandBlue
                        )
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = { showSortDialog = true }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Sort",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Main List View
            if (groupBy != "none" && !isInGroup) {
                // Group breakdown cards
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (groupStats.isEmpty()) {
                        item {
                            EmptyStateNotice("No items found for grouping")
                        }
                    } else {
                        items(groupStats) { stat ->
                            GroupStatCard(
                                stat = stat,
                                onClick = {
                                    viewModel.selectedGroup.value = stat.groupKey
                                    viewModel.searchQuery.value = ""
                                }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            } else {
                // Item list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    if (items.isEmpty()) {
                        item {
                            EmptyStateNotice("No items found")
                        }
                    } else {
                        items(items, key = { it.id }) { item ->
                            ItemRowCard(
                                item = item,
                                isSelectionMode = isSelectionMode,
                                isSelected = selectedIds.contains(item.id),
                                onToggleSelect = { viewModel.toggleItemSelection(item.id) },
                                onClick = {
                                    if (isSelectionMode) {
                                        viewModel.toggleItemSelection(item.id)
                                    } else {
                                        viewModel.openItemDetail(item.id)
                                    }
                                }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        // Floating Selection Bar at Bottom
        if (isSelectionMode && selectedIds.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedIds.size} selected",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { viewModel.selectAllFiltered() }) {
                            Text("All")
                        }
                        OutlinedButton(onClick = { showBulkTypeDialog = true }) {
                            Text("Type")
                        }
                        OutlinedButton(onClick = { showBulkUnitDialog = true }) {
                            Text("Unit")
                        }
                        if (isAdmin) {
                            Button(
                                onClick = { showBulkDeleteDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                            ) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }

    // Sort Dialog
    if (showSortDialog) {
        val sortOptions = listOf(
            Pair("o", "Original order"),
            Pair("n", "Name A–Z"),
            Pair("nz", "Name Z–A"),
            Pair("q", "Quantity (high to low)"),
            Pair("c", "Cost (high to low)")
        )
        ValuePickerDialog(
            title = "Sort by",
            optionsWithCount = sortOptions.map { Pair(it.second, 0) },
            currentValue = sortOptions.firstOrNull { it.first == sortOption }?.second ?: "",
            onSelect = { selectedName ->
                val code = sortOptions.firstOrNull { it.second == selectedName }?.first ?: "o"
                viewModel.sortOption.value = code
                showSortDialog = false
            },
            onDismiss = { showSortDialog = false }
        )
    }

    // Group By Dialog
    if (showGroupByDialog) {
        val gbOptions = listOf(
            Pair("none", "None"),
            Pair("name", "Name"),
            Pair("cost", "Cost"),
            Pair("price", "Price"),
            Pair("type", "Type"),
            Pair("brand", "Brand"),
            Pair("size", "Size"),
            Pair("mrp", "Mrp"),
            Pair("unit", "Unit")
        )
        ValuePickerDialog(
            title = "Group by",
            optionsWithCount = gbOptions.map { Pair(it.second, 0) },
            currentValue = gbOptions.firstOrNull { it.first == groupBy }?.second ?: "None",
            onSelect = { selectedName ->
                val code = gbOptions.firstOrNull { it.second == selectedName }?.first ?: "none"
                viewModel.groupBy.value = code
                viewModel.selectedGroup.value = null
                showGroupByDialog = false
            },
            onDismiss = { showGroupByDialog = false }
        )
    }

    // Bulk Type Dialog
    if (showBulkTypeDialog) {
        val types = allItemsList.map { it.type }.filter { it.isNotBlank() }.distinct()
            .map { Pair(it, allItemsList.count { item -> item.type == it }) }
        ValuePickerDialog(
            title = "Set Type for ${selectedIds.size} items",
            optionsWithCount = types,
            currentValue = "",
            onSelect = { newType ->
                viewModel.updateSelectedType(newType)
                showBulkTypeDialog = false
            },
            onDismiss = { showBulkTypeDialog = false }
        )
    }

    // Bulk Unit Dialog
    if (showBulkUnitDialog) {
        val units = listOf("pcs", "kg", "g", "L", "ml", "box", "pack", "bag", "m", "ft", "cm", "set", "pair", "dozen", "roll", "sheet")
            .map { Pair(it, allItemsList.count { item -> item.unit == it }) }
        ValuePickerDialog(
            title = "Set Unit for ${selectedIds.size} items",
            optionsWithCount = units,
            currentValue = "",
            onSelect = { newUnit ->
                if (newUnit.isNotBlank()) {
                    viewModel.updateSelectedUnit(newUnit)
                }
                showBulkUnitDialog = false
            },
            onDismiss = { showBulkUnitDialog = false }
        )
    }

    // Bulk Delete Dialog
    if (showBulkDeleteDialog) {
        SimpleConfirmDialog(
            title = "Delete items",
            message = "Delete ${selectedIds.size} selected item(s)? This cannot be undone.",
            confirmText = "Delete",
            isDanger = true,
            onConfirm = {
                viewModel.deleteSelectedItems()
                showBulkDeleteDialog = false
            },
            onDismiss = { showBulkDeleteDialog = false }
        )
    }
}

@Composable
fun ItemRowCard(
    item: Item,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onClick: () -> Unit
) {
    val isLow = item.qty < 0 || (item.low > 0 && item.qty <= item.low)
    val tags = listOfNotNull(
        StockViewModel.formatRupees(item.cost),
        StockViewModel.formatRupees(item.price),
        item.type.takeIf { it.isNotBlank() },
        item.brand.takeIf { it.isNotBlank() }
    ).joinToString("  ·  ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
                colors = CheckboxDefaults.colors(checkedColor = BrandBlue)
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!item.imageUrl.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.name.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    fontSize = 18.sp
                                )
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "NO
IMAGE",
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    )
                } else {
                    Text(
                        text = "NO
IMAGE",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (item.size.isNotBlank()) {
                    Surface(
                        color = BrandBlue.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrandBlue.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = item.size,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandBlue,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = tags,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(horizontalAlignment = Alignment.End) {
            val qtyStr = if (item.qty % 1.0 == 0.0) item.qty.toLong().toString() else "%.1f".format(item.qty)
            val unitStr = if (item.unit != "pcs" && item.unit.isNotBlank()) " ${item.unit}" else ""
            Text(
                text = "$qtyStr$unitStr",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = if (isLow) DangerRed else BrandBlue
            )
        }
    }
}

@Composable
fun GroupStatCard(
    stat: GroupStat,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stat.groupKey,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(BrandBlue.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${stat.percentage}%",
                        color = BrandBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${stat.itemCount}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Items",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val qtyStr = if (stat.totalQty % 1.0 == 0.0) stat.totalQty.toLong().toString() else "%.1f".format(stat.totalQty)
                    Text(
                        text = qtyStr,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Quantity",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateNotice(msg: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = msg,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
