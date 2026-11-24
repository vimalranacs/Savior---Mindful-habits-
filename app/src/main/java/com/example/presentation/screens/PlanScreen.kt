package com.example.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.local.GroceryItemEntity
import com.example.data.local.GroceryListEntity
import com.example.domain.GrocerySuggestion
import com.example.domain.GrocerySuggestionEngine
import com.example.domain.SpendingAnalyticsEngine
import com.example.presentation.SaviorUiState
import com.example.presentation.components.CalmCard
import com.example.presentation.components.CalmSectionHeader
import com.example.presentation.components.CalmStatusBadge
import com.example.ui.theme.LocalSaviorColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val groceryCategories = listOf(
    "Dairy & Eggs",
    "Produce",
    "Bakery",
    "Pantry",
    "Household",
    "Personal Care",
    "Snacks",
    "Other"
)

private val unitOptions = listOf("pcs", "kg", "g", "L", "ml", "pack", "bottle")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlanScreen(
    uiState: SaviorUiState,
    initialTab: Int = 0,
    onCreateList: (String) -> Unit,
    onDeleteList: (GroceryListEntity) -> Unit,
    onSaveItem: (GroceryItemEntity) -> Unit,
    onAddSuggestion: (GrocerySuggestion, Long) -> Unit,
    onToggleChecked: (GroceryItemEntity) -> Unit,
    onToggleFavourite: (GroceryItemEntity) -> Unit,
    onToggleRunningLow: (GroceryItemEntity) -> Unit,
    onToggleAvailableAtHome: (GroceryItemEntity) -> Unit,
    onDeleteItem: (Long) -> Unit,
    onSaveEveningPlan: (tomorrowMeals: String, householdNeeds: String) -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }
    val defaultListId = uiState.groceryLists.firstOrNull()?.id ?: 1L
    var selectedListId by rememberSaveable { mutableLongStateOf(defaultListId) }

    LaunchedEffect(uiState.groceryLists) {
        if (uiState.groceryLists.none { it.id == selectedListId }) {
            selectedListId = uiState.groceryLists.firstOrNull()?.id ?: 1L
        }
    }

    var editingItem by remember { mutableStateOf<GroceryItemEntity?>(null) }
    var showItemDialog by remember { mutableStateOf(false) }
    var showNewListDialog by remember { mutableStateOf(false) }

    val todayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val todayNote = uiState.eveningPlans.firstOrNull { it.dateKey == todayKey }
    var tomorrowMealsInput by remember(todayNote) { mutableStateOf(todayNote?.tomorrowMeals.orEmpty()) }
    var householdNeedsInput by remember(todayNote) { mutableStateOf(todayNote?.householdNeeds.orEmpty()) }

    val itemsForActiveList = remember(uiState.groceryItems, selectedListId) {
        uiState.groceryItems.filter { it.listId == selectedListId }
    }
    val runningLowItems = remember(uiState.groceryItems) {
        uiState.groceryItems.filter {
            !it.isAvailableAtHome && (it.isRunningLow || GrocerySuggestionEngine.isDueForReplenishment(it))
        }
    }
    val regularItems = remember(uiState.groceryItems) {
        uiState.groceryItems.filter { it.isFavourite || it.purchaseCount >= 2 }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingItem = null
                    showItemDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_grocery_item_fab")
            ) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = "Add Grocery Item")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("plan_screen_list"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CalmSectionHeader(
                    title = "Plan Before You Order",
                    subtitle = "A five-minute evening kitchen check prevents tomorrow's impulse deliveries"
                )
            }

            item {
                val tabs = listOf("Grocery Lists", "Evening Routine")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    tabs.forEachIndexed { idx, title ->
                        SegmentedButton(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = tabs.size),
                            modifier = Modifier.testTag("plan_tab_$idx")
                        ) {
                            Text(title)
                        }
                    }
                }
            }

            if (selectedTab == 0) {
                // Multiple Shopping Lists Selector
                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.groceryLists.forEach { list ->
                            FilterChip(
                                selected = list.id == selectedListId,
                                onClick = { selectedListId = list.id },
                                label = { Text(list.name) }
                            )
                        }
                        AssistChip(
                            onClick = { showNewListDialog = true },
                            label = { Text("+ New List") },
                            modifier = Modifier.testTag("new_grocery_list_chip")
                        )
                    }
                }

                // Active List Summary Bar
                val activeList = uiState.groceryLists.firstOrNull { it.id == selectedListId }
                val activePlannedTotal = itemsForActiveList
                    .filter { !it.isChecked && !it.isAvailableAtHome }
                    .sumOf { it.approxPrice * it.quantity }

                item {
                    CalmCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = activeList?.name ?: "Shopping List",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${itemsForActiveList.count { !it.isChecked && !it.isAvailableAtHome }} to buy • " +
                                        "${itemsForActiveList.count { it.isAvailableAtHome }} at home",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (activePlannedTotal > 0.0) {
                                    CalmStatusBadge(
                                        label = "Est. ${
                                            SpendingAnalyticsEngine.formatCurrency(
                                                activePlannedTotal,
                                                uiState.settings.currencySymbol
                                            )
                                        }",
                                        isPositive = true
                                    )
                                }
                                if (activeList != null && !activeList.isDefault) {
                                    IconButton(onClick = { onDeleteList(activeList) }) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = "Delete shopping list"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (itemsForActiveList.isEmpty()) {
                    item {
                        CalmCard {
                            Text(
                                text = "This list is empty",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Tap the + button or pick from the explainable suggestions below to plan your next grocery run.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(itemsForActiveList, key = { it.id }) { item ->
                        GroceryItemCard(
                            item = item,
                            currencySymbol = uiState.settings.currencySymbol,
                            onToggleChecked = { onToggleChecked(item) },
                            onToggleFavourite = { onToggleFavourite(item) },
                            onToggleRunningLow = { onToggleRunningLow(item) },
                            onToggleAvailableAtHome = { onToggleAvailableAtHome(item) },
                            onEdit = {
                                editingItem = item
                                showItemDialog = true
                            },
                            onDelete = { onDeleteItem(item.id) }
                        )
                    }
                }

                // Explainable Smart Suggestions Section
                if (uiState.suggestions.isNotEmpty()) {
                    item {
                        CalmSectionHeader(
                            title = "Explainable Offline Suggestions",
                            subtitle = "Based on your marked items, replenishment cycles, favourites, and staples"
                        )
                    }

                    items(uiState.suggestions, key = { it.name }) { suggestion ->
                        CalmCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${suggestion.name} (${suggestion.defaultQuantity.toCleanString()} ${suggestion.defaultUnit})",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = suggestion.explanation,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedButton(
                                    onClick = { onAddSuggestion(suggestion, selectedListId) },
                                    modifier = Modifier.testTag("add_suggestion_${suggestion.name}")
                                ) {
                                    Text("+ Add")
                                }
                            }
                        }
                    }
                }
            } else {
                // Evening Planning Screen
                item {
                    CalmCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Nightlight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "A small reminder for tomorrow's you",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text(
                            text = "Take a moment to check your kitchen, review what is running low, and note tomorrow's meals so you don't have to order under pressure.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Running low / due for replenishment
                item {
                    CalmCard {
                        Text(
                            text = "Running Low & Due for Replenishment (${runningLowItems.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (runningLowItems.isEmpty()) {
                            Text(
                                text = "No items currently marked as running low. Tap 'Running low' on any grocery item whenever you notice your kitchen stock getting low.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            runningLowItems.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "• ${item.name} (${item.quantity.toCleanString()} ${item.unit})",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    TextButton(onClick = { onToggleAvailableAtHome(item) }) {
                                        Text("Have at home")
                                    }
                                }
                            }
                        }
                    }
                }

                // Regularly purchased / Favourites quick add
                if (regularItems.isNotEmpty()) {
                    item {
                        CalmCard {
                            Text(
                                text = "Your Regular Staples & Favourites",
                                style = MaterialTheme.typography.titleMedium
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                regularItems.forEach { fav ->
                                    AssistChip(
                                        onClick = {
                                            if (!fav.isRunningLow) onToggleRunningLow(fav)
                                        },
                                        label = {
                                            Text(
                                                if (fav.isRunningLow) "${fav.name} (Low)" else "+ Mark ${fav.name} low"
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Tomorrow's Meals & Upcoming Household Needs
                item {
                    CalmCard {
                        Text(
                            text = "Plan Tomorrow's Meals & Household Needs",
                            style = MaterialTheme.typography.titleMedium
                        )
                        OutlinedTextField(
                            value = tomorrowMealsInput,
                            onValueChange = { tomorrowMealsInput = it },
                            label = { Text("Tomorrow's meals (breakfast, lunch, dinner)") },
                            placeholder = { Text("e.g., Poha & fruit, Dal rice + sabzi…") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("evening_meals_input"),
                            minLines = 2
                        )
                        OutlinedTextField(
                            value = householdNeedsInput,
                            onValueChange = { householdNeedsInput = it },
                            label = { Text("Upcoming household needs") },
                            placeholder = { Text("e.g., Pick up milk & coriander on the way home…") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("evening_household_input"),
                            minLines = 2
                        )
                        Button(
                            onClick = {
                                onSaveEveningPlan(tomorrowMealsInput, householdNeedsInput)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_evening_plan_button"),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(imageVector = Icons.Outlined.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                if (todayNote != null) "Update Tonight's Plan" else "Complete Tonight's Check-In"
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (showItemDialog) {
        GroceryItemEditorDialog(
            initialItem = editingItem,
            activeListId = selectedListId,
            currencySymbol = uiState.settings.currencySymbol,
            onDismiss = { showItemDialog = false },
            onSave = { item ->
                onSaveItem(item)
                showItemDialog = false
            }
        )
    }

    if (showNewListDialog) {
        NewGroceryListDialog(
            onDismiss = { showNewListDialog = false },
            onCreate = { name ->
                onCreateList(name)
                showNewListDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroceryItemCard(
    item: GroceryItemEntity,
    currencySymbol: String,
    onToggleChecked: () -> Unit,
    onToggleFavourite: () -> Unit,
    onToggleRunningLow: () -> Unit,
    onToggleAvailableAtHome: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val saviorColors = LocalSaviorColors.current
    val isDue = remember(item) { GrocerySuggestionEngine.isDueForReplenishment(item) }

    CalmCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = item.isChecked,
                    onCheckedChange = { onToggleChecked() },
                    modifier = Modifier.testTag("check_item_${item.name}")
                )
                Column {
                    Text(
                        text = "${item.name} • ${item.quantity.toCleanString()} ${item.unit}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                    )
                    val metaParts = buildList {
                        add(item.category)
                        if (item.approxPrice > 0.0) {
                            add(
                                "~" + SpendingAnalyticsEngine.formatCurrency(
                                    item.approxPrice * item.quantity,
                                    currencySymbol
                                )
                            )
                        }
                        if (item.frequencyDays > 0) {
                            add("Every ${item.frequencyDays}d")
                        }
                    }
                    Text(
                        text = metaParts.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row {
                IconButton(onClick = onToggleFavourite) {
                    Icon(
                        imageVector = if (item.isFavourite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favourite item",
                        tint = if (item.isFavourite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(imageVector = Icons.Outlined.Edit, contentDescription = "Edit item")
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Outlined.DeleteOutline, contentDescription = "Delete item")
                }
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = item.isRunningLow,
                onClick = onToggleRunningLow,
                label = { Text(if (item.isRunningLow) "Running low" else "Mark running low") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                }
            )
            FilterChip(
                selected = item.isAvailableAtHome,
                onClick = onToggleAvailableAtHome,
                label = { Text(if (item.isAvailableAtHome) "Available at home" else "At home?") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Home,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                }
            )
            if (isDue && !item.isRunningLow) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = saviorColors.warmContainer,
                    border = BorderStroke(1.dp, saviorColors.borderSubtle)
                ) {
                    Text(
                        text = "Cycle due",
                        style = MaterialTheme.typography.labelSmall,
                        color = saviorColors.warmAccent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroceryItemEditorDialog(
    initialItem: GroceryItemEntity?,
    activeListId: Long,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (GroceryItemEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialItem?.name.orEmpty()) }
    var quantityText by remember {
        mutableStateOf(initialItem?.quantity?.toCleanString() ?: "1")
    }
    var unit by remember { mutableStateOf(initialItem?.unit ?: "pcs") }
    var category by remember { mutableStateOf(initialItem?.category ?: "Pantry") }
    var priceText by remember {
        mutableStateOf(
            initialItem?.approxPrice?.takeIf { it > 0.0 }?.toCleanString().orEmpty()
        )
    }
    var frequencyText by remember {
        mutableStateOf(
            initialItem?.frequencyDays?.takeIf { it > 0 }?.toString().orEmpty()
        )
    }
    var reminderEnabled by remember { mutableStateOf(initialItem?.reminderEnabled ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialItem == null) "Add Grocery Item" else "Edit Grocery Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("grocery_item_name_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Unit price ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1.3f)
                    )
                }

                Text("Unit", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    unitOptions.forEach { u ->
                        FilterChip(
                            selected = unit == u,
                            onClick = { unit = u },
                            label = { Text(u) }
                        )
                    }
                }

                Text("Category", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    groceryCategories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = frequencyText,
                    onValueChange = { frequencyText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Replenishment cycle in days (optional, e.g. 3 or 7)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                FilterChip(
                    selected = reminderEnabled,
                    onClick = { reminderEnabled = !reminderEnabled },
                    label = { Text("Include in running-low evening check") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    val qty = quantityText.toDoubleOrNull()?.coerceAtLeast(0.1) ?: 1.0
                    val price = priceText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                    val freq = frequencyText.toIntOrNull()?.coerceAtLeast(0) ?: 0
                    val entity = (initialItem ?: GroceryItemEntity(
                        listId = activeListId,
                        name = name.trim()
                    )).copy(
                        name = name.trim(),
                        quantity = qty,
                        unit = unit,
                        category = category,
                        approxPrice = price,
                        frequencyDays = freq,
                        reminderEnabled = reminderEnabled
                    )
                    onSave(entity)
                },
                modifier = Modifier.testTag("save_grocery_item_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun NewGroceryListDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var listName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Shopping List") },
        text = {
            OutlinedTextField(
                value = listName,
                onValueChange = { listName = it },
                label = { Text("List name (e.g., Weekend Farmers Market)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                enabled = listName.isNotBlank(),
                onClick = { onCreate(listName.trim()) }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun Double.toCleanString(): String {
    return if (this % 1.0 == 0.0) {
        this.toLong().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", this)
    }
}
