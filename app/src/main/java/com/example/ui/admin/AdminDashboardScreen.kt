package com.example.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.OrderStatus
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.OrderStatusChip
import com.example.ui.components.SalesBarChart
import com.example.ui.components.SampleNoticeBanner
import com.example.ui.components.formatPhp
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun AdminDashboardScreen(
    viewModel: AdminViewModel,
    onNavigateToOrders: () -> Unit,
    onNavigateToProducts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ordersState by viewModel.orders.collectAsStateWithLifecycle()
    val productsState by viewModel.products.collectAsStateWithLifecycle()

    val orders = (ordersState as? UiState.Success)?.data ?: emptyList()
    val products = (productsState as? UiState.Success)?.data ?: emptyList()

    val activeOrders = orders.filter {
        it.status != OrderStatus.COMPLETED.label && it.status != OrderStatus.CANCELLED.label
    }
    val nonCancelledOrders = orders.filter { it.status != OrderStatus.CANCELLED.label }
    val todayCal = Calendar.getInstance()
    val dailySales = nonCancelledOrders.filter { ord ->
        val date = ord.createdAt?.toDate()
        if (date == null) true
        else {
            val c = Calendar.getInstance().apply { time = date }
            c.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                c.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
        }
    }.sumOf { it.totalPhp }

    // Build chart data by day or category
    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val chartPoints = nonCancelledOrders
        .groupBy { ord ->
            val date = ord.createdAt?.toDate()
            if (date != null) dayFormat.format(date) else "Today"
        }
        .map { (day, list) -> day to list.sumOf { it.totalPhp } }
        .take(6)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Admin Dashboard",
                    style = MaterialTheme.typography.headlineLarge
                )
                Text(
                    text = if (viewModel.isCloudAdmin) "Café Antonio Luna • Live Cloud Sync"
                    else "Café Antonio Luna • Preview Sandbox",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        if (!viewModel.isCloudAdmin) {
            item {
                SampleNoticeBanner(
                    message = "Preview Mode: Signed-in account is a customer account. Changes here stay local; sign in with an authorized store admin email for live cloud writes."
                )
            }
        }

        // 2x2 KPI Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AdminKpiCard(
                        title = "Total Orders",
                        value = orders.size.toString(),
                        icon = Icons.Outlined.ReceiptLong,
                        modifier = Modifier.weight(1f)
                    )
                    AdminKpiCard(
                        title = "Daily Sales",
                        value = formatPhp(dailySales),
                        icon = Icons.Outlined.Payments,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AdminKpiCard(
                        title = "Menu Products",
                        value = products.size.toString(),
                        icon = Icons.Outlined.Coffee,
                        modifier = Modifier.weight(1f)
                    )
                    AdminKpiCard(
                        title = "Pending Orders",
                        value = activeOrders.size.toString(),
                        icon = Icons.Outlined.HourglassTop,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Sales Chart Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Sales Overview (₱)", style = MaterialTheme.typography.titleMedium)
                    SalesBarChart(dataPoints = chartPoints)
                }
            }
        }

        // Quick Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateToProducts,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f).testTag("admin_dash_products_btn")
                ) {
                    Text("Manage Menu")
                }
                OutlinedButton(
                    onClick = onNavigateToOrders,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f).testTag("admin_dash_orders_btn")
                ) {
                    Text("View Queue (${activeOrders.size})")
                }
            }
        }

        // Pending Queue
        item {
            Text("Pending Order Queue", style = MaterialTheme.typography.titleLarge)
        }

        if (activeOrders.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No pending orders",
                    subtitle = "New customer orders will appear here automatically."
                )
            }
        } else {
            items(activeOrders.take(5), key = { it.id }) { order ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${order.orderNumber} • ${order.customerName}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${order.orderType} • ${order.items.sumOf { it.quantity }} items • ${formatPhp(order.totalPhp)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OrderStatusChip(status = order.status)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminKpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
