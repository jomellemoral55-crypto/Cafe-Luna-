package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val phone: String? = null,
    val role: String = "customer",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MenuCategory(
    @DocumentId val id: String = "",
    val name: String = "",
    val sortOrder: Long = 0L,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MenuProduct(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val pricePhp: Double = 0.0,
    val imageKey: String = "sample_coffee_latte",
    val localImagePath: String? = null,
    val featured: Boolean = false,
    val bestSeller: Boolean = false,
    val available: Boolean = true,
    val sampleData: Boolean = true,
    val customizableDrink: Boolean = true,
    val allowedTemps: List<String> = listOf("Hot", "Iced"),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CartItem(
    @DocumentId val id: String = "",
    val productId: String = "",
    val productName: String = "",
    val categoryName: String = "",
    val imageKey: String = "sample_coffee_latte",
    val localImagePath: String? = null,
    val basePricePhp: Double = 0.0,
    val unitPricePhp: Double = 0.0,
    val quantity: Long = 1L,
    val temperature: String = "Hot",
    val size: String = "Regular (12oz)",
    val sugarLevel: String = "100%",
    val milkPreference: String = "Whole Milk",
    val extraShots: Long = 0L,
    val addOns: List<String> = emptyList(),
    val customizationSummary: String = "",
    val customizableDrink: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val lineTotalPhp: Double get() = unitPricePhp * quantity
}

@Keep
@IgnoreExtraProperties
data class OrderLineItem(
    val productId: String = "",
    val productName: String = "",
    val unitPrice: Double = 0.0,
    val quantity: Long = 1L,
    val lineTotal: Double = 0.0,
    val customizationSummary: String = "",
    val imageKey: String = "sample_coffee_latte"
)

@Keep
@IgnoreExtraProperties
data class CafeOrder(
    @DocumentId val id: String = "",
    val orderNumber: String = "",
    val userId: String = "",
    val customerName: String = "",
    val contactNumber: String = "",
    val orderType: String = "Pickup",
    val paymentMethod: String = "Cash",
    val status: String = OrderStatus.RECEIVED.label,
    val items: List<OrderLineItem> = emptyList(),
    val subtotalPhp: Double = 0.0,
    val serviceChargePhp: Double = 0.0,
    val totalPhp: Double = 0.0,
    val notes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class OrderStatus(val label: String, val stepIndex: Int) {
    RECEIVED("Order Received", 0),
    PREPARING("Preparing Your Order", 1),
    READY("Ready for Pickup", 2),
    COMPLETED("Completed", 3),
    CANCELLED("Cancelled", -1);

    companion object {
        fun fromLabel(raw: String): OrderStatus =
            entries.firstOrNull { it.label == raw } ?: RECEIVED

        val trackingSteps: List<OrderStatus> = listOf(RECEIVED, PREPARING, READY, COMPLETED)
    }
}

enum class DrinkSize(val label: String, val deltaPhp: Double) {
    REGULAR("Regular (12oz)", 0.0),
    LARGE("Large (16oz)", 25.0),
    GRANDE("Grande (20oz)", 45.0)
}

enum class MilkOption(val label: String, val deltaPhp: Double) {
    WHOLE("Whole Milk", 0.0),
    OAT("Oat Milk", 35.0),
    SOY("Soy Milk", 25.0),
    ALMOND("Almond Milk", 35.0)
}

enum class AddOnOption(val label: String, val deltaPhp: Double) {
    VANILLA("Vanilla Syrup", 20.0),
    CARAMEL("Caramel Drizzle", 20.0),
    WHIPPED_CREAM("Whipped Cream", 25.0),
    ESPRESSO_JELLY("Espresso Jelly", 30.0)
}
