package com.example.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AddOnOption
import com.example.model.DrinkSize
import com.example.model.MilkOption
import com.example.ui.components.CafeProductImage
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SampleLabelChip
import com.example.ui.components.formatPhp
import com.example.viewmodel.CustomerViewModel
import com.example.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductCustomizationScreen(
    productId: String,
    viewModel: CustomerViewModel,
    onNavigateBack: () -> Unit,
    onAddedToCart: () -> Unit
) {
    val productsState by viewModel.products.collectAsStateWithLifecycle()
    val products = (productsState as? UiState.Success)?.data ?: emptyList()
    val product = products.firstOrNull { it.id == productId }

    if (product == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Item Details") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).padding(20.dp)) {
                EmptyStateCard(
                    title = "Menu item not found",
                    subtitle = "This item may have been removed by the café."
                )
            }
        }
        return
    }

    val temps = remember(product.id) {
        if (product.customizableDrink) {
            product.allowedTemps.ifEmpty { listOf("Hot", "Iced") }
        } else {
            listOf("Warmed", "Room Temp")
        }
    }

    var selectedTemp by remember(product.id) { mutableStateOf(temps.first()) }
    var selectedSize by remember(product.id) { mutableStateOf(DrinkSize.REGULAR) }
    var selectedSugar by remember(product.id) { mutableStateOf("100%") }
    var selectedMilk by remember(product.id) { mutableStateOf(MilkOption.WHOLE) }
    var extraShots by remember(product.id) { mutableIntStateOf(0) }
    val selectedAddOns = remember(product.id) { mutableStateListOf<AddOnOption>() }
    var quantity by remember(product.id) { mutableIntStateOf(1) }

    val unitPrice = CustomerViewModel.calculateCustomizedUnitPrice(
        basePrice = product.pricePhp,
        customizableDrink = product.customizableDrink,
        size = selectedSize,
        milk = selectedMilk,
        extraShots = extraShots,
        addOns = selectedAddOns
    )
    val totalPrice = unitPrice * quantity

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(product.name, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Quantity Stepper
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        IconButton(
                            onClick = { if (quantity > 1) quantity-- },
                            modifier = Modifier.size(40.dp).testTag("customize_qty_minus")
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity")
                        }
                        Text(
                            text = quantity.toString(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(
                            onClick = { if (quantity < 20) quantity++ },
                            modifier = Modifier.size(40.dp).testTag("customize_qty_plus")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Increase quantity")
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.addCustomizedToCart(
                                product = product,
                                temperature = selectedTemp,
                                size = selectedSize,
                                sugarLevel = selectedSugar,
                                milk = selectedMilk,
                                extraShots = extraShots,
                                addOns = selectedAddOns.toList(),
                                quantity = quantity
                            )
                            onAddedToCart()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("add_customized_to_cart_button")
                    ) {
                        Text(
                            text = "Add • ${formatPhp(totalPrice)}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                CafeProductImage(
                    imageKey = product.imageKey,
                    localImagePath = product.localImagePath,
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                )
            }

            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = product.categoryName.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        if (product.sampleData) {
                            SampleLabelChip()
                        }
                    }
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Base Price: ${formatPhp(product.pricePhp)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp)) }

            // Temperature / Serving Choice (Applicable to both Drinks & Pastries)
            item {
                OptionSection(
                    title = if (product.customizableDrink) "Temperature" else "Serving Style"
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        temps.forEach { temp ->
                            FilterChip(
                                selected = selectedTemp == temp,
                                onClick = { selectedTemp = temp },
                                label = { Text(temp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.testTag("temp_chip_$temp")
                            )
                        }
                    }
                }
            }

            // Drink-specific customizations ONLY when product.customizableDrink == true
            if (product.customizableDrink) {
                item {
                    OptionSection(title = "Cup Size") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DrinkSize.entries.forEach { size ->
                                val extraLabel = if (size.deltaPhp > 0) " (+₱${size.deltaPhp.toInt()})" else ""
                                FilterChip(
                                    selected = selectedSize == size,
                                    onClick = { selectedSize = size },
                                    label = { Text("${size.label}$extraLabel") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.testTag("size_chip_${size.name}")
                                )
                            }
                        }
                    }
                }

                item {
                    OptionSection(title = "Sugar Level") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("0%", "25%", "50%", "75%", "100%").forEach { sugar ->
                                FilterChip(
                                    selected = selectedSugar == sugar,
                                    onClick = { selectedSugar = sugar },
                                    label = { Text(sugar) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }
                }

                item {
                    OptionSection(title = "Milk Preference") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MilkOption.entries.forEach { milk ->
                                val extra = if (milk.deltaPhp > 0) " (+₱${milk.deltaPhp.toInt()})" else ""
                                FilterChip(
                                    selected = selectedMilk == milk,
                                    onClick = { selectedMilk = milk },
                                    label = { Text("${milk.label}$extra") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }
                }

                item {
                    OptionSection(title = "Additional Espresso Shots (+₱35 each)") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            IconButton(
                                onClick = { if (extraShots > 0) extraShots-- },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "Remove espresso shot")
                            }
                            Text(
                                text = "$extraShots Extra Shot${if (extraShots == 1) "" else "s"}",
                                style = MaterialTheme.typography.titleSmall
                            )
                            IconButton(
                                onClick = { if (extraShots < 4) extraShots++ },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Add espresso shot")
                            }
                        }
                    }
                }

                item {
                    OptionSection(title = "Add-Ons") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AddOnOption.entries.forEach { addOn ->
                                val selected = addOn in selectedAddOns
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        if (selected) selectedAddOns.remove(addOn) else selectedAddOns.add(addOn)
                                    },
                                    label = { Text("${addOn.label} (+₱${addOn.deltaPhp.toInt()})") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        content()
    }
}
