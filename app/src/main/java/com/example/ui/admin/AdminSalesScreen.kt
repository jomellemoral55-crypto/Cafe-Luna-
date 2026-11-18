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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.OrderStatus
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.OrderStatusChip
import com.example.ui.components.SalesBarChart
import com.example.ui.components.formatPhp
import com.example.ui.components.formatTimestamp
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.UiState

@Composable
fun AdminSalesScreen(
    viewModel: AdminViewModel,
    modifier: Modifier = Modifier
) {
    val ordersState by viewModel.orders.collectAsStateWithLifecycle()
    val orders = (ordersState as? UiState.Success)?.data ?: emptyList()

    val validOrders = orders.filter { it.status != OrderStatus.CANCELLED.label }
    val totalRevenue = validOrders.sumOf { it.totalPhp }
    val completedRevenue = validOrders.filter { it.status == OrderStatus.COMPLETED.label }.sumOf { it.totalPhp }
    val avgTicket = if (validOrders.isEmpty()) 0.0 else totalRevenue / validOrders.size

    val paymentBreakdown = validOrders
        .groupBy { it.paymentMethod }
        .map { (method, list) -> method to list.sumOf { it.totalPhp } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Sales & Analytics", style = MaterialTheme.typography.headlineLarge)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Gross Order Revenue",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                    Text(
                        text = formatPhp(totalRevenue),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Completed: ${formatPhp(completedRevenue)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "Avg Ticket: ${formatPhp(avgTicket)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Revenue by Payment Method", style = MaterialTheme.typography.titleMedium)
                    SalesBarChart(dataPoints = paymentBreakdown)
                }
            }
        }

        item {
            Text("Sales History (${validOrders.size})", style = MaterialTheme.typography.titleLarge)
        }

        if (validOrders.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No sales recorded yet",
                    subtitle = "Completed and active customer orders will appear in your sales ledger."
                )
            }
        } else {
            items(validOrders, key = { it.id }) { order ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${order.orderNumber} • ${order.customerName}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = formatPhp(order.totalPhp),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${order.paymentMethod} • ${formatTimestamp(order.createdAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OrderStatusChip(status = order.status)
                        }
                        HorizontalDivider(modifier = Modifier.padding(top = 2.dp))
                        Text(
                            text = order.items.joinToString(", ") { "${it.quantity}x ${it.productName}" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
