package com.example.ui.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ImageStorageHelper
import com.example.model.MenuCategory
import com.example.model.MenuProduct
import com.example.ui.components.CafeProductImage
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SampleLabelChip
import com.example.ui.components.formatPhp
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.UiState

@Composable
fun AdminProductsScreen(
    viewModel: AdminViewModel,
    modifier: Modifier = Modifier
) {
    val productsState by viewModel.products.collectAsStateWithLifecycle()
    val categoriesState by viewModel.categories.collectAsStateWithLifecycle()

    val categories = (categoriesState as? UiState.Success)?.data ?: emptyList()

    var editingProduct by remember { mutableStateOf<MenuProduct?>(null) }
    var isCreatingProduct by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val firstCat = categories.firstOrNull()
                    editingProduct = MenuProduct(
                        categoryId = firstCat?.id ?: "cat_coffee",
                        categoryName = firstCat?.name ?: "Coffee",
                        pricePhp = 150.0,
                        sampleData = false
                    )
                    isCreatingProduct = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("admin_add_product_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Menu Product")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Menu Products", style = MaterialTheme.typography.headlineLarge)
                OutlinedButton(
                    onClick = { showCategoryDialog = true },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("manage_categories_button")
                ) {
                    Icon(Icons.Filled.Category, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Categories")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            when (val state = productsState) {
                UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    EmptyStateCard(title = "Could not load products", subtitle = state.message)
                }
                is UiState.Success -> {
                    val products = state.data
                    if (products.isEmpty()) {
                        EmptyStateCard(
                            title = "No menu products yet",
                            subtitle = "Tap + to add your first beverage or pastry."
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(products, key = { it.id }) { product ->
                                AdminProductCard(
                                    product = product,
                                    onEdit = {
                                        editingProduct = product
                                        isCreatingProduct = false
                                    },
                                    onDelete = { viewModel.deleteProduct(product.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingProduct != null) {
        ProductEditorDialog(
            initial = editingProduct!!,
            isNew = isCreatingProduct,
            categories = categories,
            onDismiss = { editingProduct = null },
            onSave = { updated ->
                viewModel.saveProduct(updated, isNew = isCreatingProduct)
                editingProduct = null
            }
        )
    }

    if (showCategoryDialog) {
        CategoryManagerDialog(
            categories = categories,
            onDismiss = { showCategoryDialog = false },
            onAddCategory = { name ->
                viewModel.saveCategory(
                    MenuCategory(
                        name = name,
                        sortOrder = (categories.maxOfOrNull { it.sortOrder } ?: 0L) + 1L,
                        active = true
                    ),
                    isNew = true
                )
            },
            onDeleteCategory = { viewModel.deleteCategory(it) }
        )
    }
}

@Composable
private fun AdminProductCard(
    product: MenuProduct,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CafeProductImage(
                imageKey = product.imageKey,
                localImagePath = product.localImagePath,
                contentDescription = product.name,
                modifier = Modifier
                    .size(64.dp)
                    .clip(MaterialTheme.shapes.small)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (product.sampleData) {
                        SampleLabelChip(text = "Sample")
                    }
                }
                Text(
                    text = "${product.categoryName} • ${formatPhp(product.pricePhp)}" +
                        if (!product.available) " • Unavailable" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.testTag("edit_prod_${product.id}")) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit ${product.name}")
            }
            IconButton(onClick = onDelete, modifier = Modifier.testTag("del_prod_${product.id}")) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete ${product.name}", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductEditorDialog(
    initial: MenuProduct,
    isNew: Boolean,
    categories: List<MenuCategory>,
    onDismiss: () -> Unit,
    onSave: (MenuProduct) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initial.name) }
    var description by remember { mutableStateOf(initial.description) }
    var priceText by remember { mutableStateOf(if (initial.pricePhp > 0) initial.pricePhp.toString() else "") }
    var selectedCategory by remember {
        mutableStateOf(categories.firstOrNull { it.id == initial.categoryId } ?: categories.firstOrNull())
    }
    var categoryExpanded by remember { mutableStateOf(false) }
    var imageKey by remember { mutableStateOf(initial.imageKey) }
    var localImagePath by remember { mutableStateOf(initial.localImagePath) }
    var customizableDrink by remember { mutableStateOf(initial.customizableDrink) }
    var featured by remember { mutableStateOf(initial.featured) }
    var bestSeller by remember { mutableStateOf(initial.bestSeller) }
    var available by remember { mutableStateOf(initial.available) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                val copied = ImageStorageHelper.copyImageToAppStorage(context, uri)
                if (copied != null) {
                    localImagePath = copied
                }
            }
        }
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Add Product" else "Edit Product") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CafeProductImage(
                    imageKey = imageKey,
                    localImagePath = localImagePath,
                    contentDescription = "Product preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(MaterialTheme.shapes.medium)
                )
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("upload_product_photo_button")
                ) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Choose Photo from Device")
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("editor_product_name")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Price (₱ PHP)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("editor_product_price")
                )

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: initial.categoryName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategory = cat
                                    imageKey = when (cat.name.lowercase()) {
                                        "tea" -> "sample_matcha_tea"
                                        "pastries" -> "sample_pastry_croissant"
                                        else -> "sample_coffee_latte"
                                    }
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                ToggleRow("Drink Customization Options", customizableDrink) { customizableDrink = it }
                ToggleRow("Featured on Home", featured) { featured = it }
                ToggleRow("Best Seller", bestSeller) { bestSeller = it }
                ToggleRow("Available for Ordering", available) { available = it }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceText.toDoubleOrNull() ?: initial.pricePhp
                    onSave(
                        initial.copy(
                            name = name.trim(),
                            description = description.trim(),
                            categoryId = selectedCategory?.id ?: initial.categoryId,
                            categoryName = selectedCategory?.name ?: initial.categoryName,
                            pricePhp = price.coerceIn(0.0, 100000.0),
                            imageKey = imageKey,
                            localImagePath = localImagePath,
                            customizableDrink = customizableDrink,
                            allowedTemps = if (customizableDrink) listOf("Hot", "Iced") else emptyList(),
                            featured = featured,
                            bestSeller = bestSeller,
                            available = available,
                            sampleData = false
                        )
                    )
                },
                enabled = name.isNotBlank() && (priceText.toDoubleOrNull() ?: -1.0) >= 0.0,
                modifier = Modifier.testTag("save_product_dialog_button")
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
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun CategoryManagerDialog(
    categories: List<MenuCategory>,
    onDismiss: () -> Unit,
    onAddCategory: (String) -> Unit,
    onDeleteCategory: (String) -> Unit
) {
    var newCategoryName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Categories") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("New Category") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("new_category_input")
                    )
                    Button(
                        onClick = {
                            if (newCategoryName.isNotBlank()) {
                                onAddCategory(newCategoryName.trim())
                                newCategoryName = ""
                            }
                        },
                        enabled = newCategoryName.isNotBlank(),
                        modifier = Modifier.testTag("add_category_button")
                    ) {
                        Text("Add")
                    }
                }
                categories.forEach { cat ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(cat.name, style = MaterialTheme.typography.bodyLarge)
                        IconButton(onClick = { onDeleteCategory(cat.id) }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete ${cat.name}", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}
