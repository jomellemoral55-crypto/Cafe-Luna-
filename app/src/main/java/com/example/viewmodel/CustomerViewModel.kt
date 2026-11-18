package com.example.viewmodel

import android.util.Log
import com.example.data.CafeRepository
import com.example.data.SampleCatalog
import com.example.model.AddOnOption
import com.example.model.CafeOrder
import com.example.model.CartItem
import com.example.model.DrinkSize
import com.example.model.MenuCategory
import com.example.model.MenuProduct
import com.example.model.MilkOption
import com.example.model.OrderStatus
import com.example.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

internal fun <T> Flow<T>.asUiStateFlow(
    tag: String,
    defaultError: String
): Flow<UiState<T>> =
    this.map<T, UiState<T>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(tag, defaultError, rootError)
            emit(UiState.Error(rootError.message ?: defaultError))
        }

class CustomerViewModel(
    private val repository: CafeRepository,
    private val currentUserId: String,
    private val isBootstrappedAdmin: Boolean
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow("ALL")
    val selectedCategoryId: StateFlow<String> = _selectedCategoryId.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _lastPlacedOrder = MutableStateFlow<CafeOrder?>(null)
    val lastPlacedOrder: StateFlow<CafeOrder?> = _lastPlacedOrder.asStateFlow()

    private var seededAttempted = false

    val userProfile: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId)
            .asUiStateFlow(TAG, "Failed to load profile")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val categories: StateFlow<UiState<List<MenuCategory>>> =
        repository.observeCategories()
            .map { list ->
                if (list.isEmpty()) SampleCatalog.defaultCategories else list.filter { it.active }
            }
            .asUiStateFlow(TAG, "Failed to load categories")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val products: StateFlow<UiState<List<MenuProduct>>> =
        repository.observeProducts()
            .map { list ->
                if (list.isEmpty()) {
                    if (isBootstrappedAdmin && !seededAttempted) {
                        seededAttempted = true
                        viewModelScope.launch { repository.seedSampleMenuIfAdmin() }
                    }
                    SampleCatalog.defaultProducts
                } else {
                    list
                }
            }
            .asUiStateFlow(TAG, "Failed to load menu")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val cartItems: StateFlow<UiState<List<CartItem>>> =
        repository.observeCart(currentUserId)
            .asUiStateFlow(TAG, "Failed to load cart")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val orders: StateFlow<UiState<List<CafeOrder>>> =
        repository.observeUserOrders(currentUserId)
            .asUiStateFlow(TAG, "Failed to load orders")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    init {
        viewModelScope.launch {
            repository.ensureUserProfile()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(categoryId: String) {
        _selectedCategoryId.value = categoryId
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun addCustomizedToCart(
        product: MenuProduct,
        temperature: String,
        size: DrinkSize,
        sugarLevel: String,
        milk: MilkOption,
        extraShots: Int,
        addOns: List<AddOnOption>,
        quantity: Int
    ) {
        val unitPrice = calculateCustomizedUnitPrice(
            basePrice = product.pricePhp,
            customizableDrink = product.customizableDrink,
            size = size,
            milk = milk,
            extraShots = extraShots,
            addOns = addOns
        )
        val summary = if (product.customizableDrink) {
            buildList {
                if (temperature.isNotBlank()) add(temperature)
                add(size.label)
                add("$sugarLevel Sugar")
                add(milk.label)
                if (extraShots > 0) add("+$extraShots Espresso Shot${if (extraShots > 1) "s" else ""}")
                if (addOns.isNotEmpty()) add(addOns.joinToString(", ") { it.label })
            }.joinToString(" • ")
        } else {
            if (temperature == "Warmed") "Warmed Freshly" else "Standard Serving"
        }

        val item = CartItem(
            productId = product.id,
            productName = product.name,
            categoryName = product.categoryName,
            imageKey = product.imageKey,
            localImagePath = product.localImagePath,
            basePricePhp = product.pricePhp,
            unitPricePhp = unitPrice,
            quantity = quantity.toLong().coerceIn(1L, 50L),
            temperature = if (product.customizableDrink) temperature else "Standard",
            size = if (product.customizableDrink) size.label else "Standard",
            sugarLevel = if (product.customizableDrink) sugarLevel else "N/A",
            milkPreference = if (product.customizableDrink) milk.label else "N/A",
            extraShots = if (product.customizableDrink) extraShots.toLong() else 0L,
            addOns = if (product.customizableDrink) addOns.map { it.label } else emptyList(),
            customizationSummary = summary,
            customizableDrink = product.customizableDrink
        )

        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.addToCart(item)
            _isBusy.value = false
            _feedbackMessage.value = if (result.isSuccess) {
                "${product.name} added to cart"
            } else {
                result.exceptionOrNull()?.message ?: "Could not add item to cart"
            }
        }
    }

    fun quickAddProduct(product: MenuProduct) {
        val defaultTemp = product.allowedTemps.firstOrNull() ?: if (product.customizableDrink) "Hot" else "Standard"
        addCustomizedToCart(
            product = product,
            temperature = defaultTemp,
            size = DrinkSize.REGULAR,
            sugarLevel = "100%",
            milk = MilkOption.WHOLE,
            extraShots = 0,
            addOns = emptyList(),
            quantity = 1
        )
    }

    fun updateCartItemQuantity(cartItemId: String, newQty: Long) {
        viewModelScope.launch {
            val result = repository.updateCartQuantity(cartItemId, newQty)
            if (result.isFailure) {
                _feedbackMessage.value = result.exceptionOrNull()?.message ?: "Failed to update cart"
            }
        }
    }

    fun removeCartItem(cartItemId: String) {
        viewModelScope.launch {
            val result = repository.removeCartItem(cartItemId)
            if (result.isFailure) {
                _feedbackMessage.value = result.exceptionOrNull()?.message ?: "Failed to remove item"
            }
        }
    }

    fun placeOrder(
        customerName: String,
        contactNumber: String,
        orderType: String,
        paymentMethod: String,
        notes: String?,
        cartItems: List<CartItem>,
        serviceChargePhp: Double,
        onSuccess: (CafeOrder) -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.placeOrder(
                customerName = customerName,
                contactNumber = contactNumber,
                orderType = orderType,
                paymentMethod = paymentMethod,
                notes = notes,
                cartItems = cartItems,
                serviceChargePhp = serviceChargePhp
            )
            _isBusy.value = false
            result.onSuccess { order ->
                _lastPlacedOrder.value = order
                _feedbackMessage.value = "Order ${order.orderNumber} received!"
                onSuccess(order)
            }.onFailure { err ->
                _feedbackMessage.value = err.message ?: "Failed to place order"
            }
        }
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.updateOrderStatus(orderId, OrderStatus.CANCELLED)
            _isBusy.value = false
            _feedbackMessage.value = if (result.isSuccess) {
                "Order cancelled"
            } else {
                result.exceptionOrNull()?.message ?: "Unable to cancel order"
            }
        }
    }

    fun updateProfile(displayName: String, phone: String?) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.updateUserProfile(displayName, phone)
            _isBusy.value = false
            _feedbackMessage.value = if (result.isSuccess) {
                "Profile saved"
            } else {
                result.exceptionOrNull()?.message ?: "Failed to save profile"
            }
        }
    }

    companion object {
        private const val TAG = "CustomerVM"
        private const val STOP_TIMEOUT_MS = 5000L

        fun calculateCustomizedUnitPrice(
            basePrice: Double,
            customizableDrink: Boolean,
            size: DrinkSize,
            milk: MilkOption,
            extraShots: Int,
            addOns: List<AddOnOption>
        ): Double {
            if (!customizableDrink) return basePrice
            val shotsCost = extraShots * 35.0
            val addOnsCost = addOns.sumOf { it.deltaPhp }
            return basePrice + size.deltaPhp + milk.deltaPhp + shotsCost + addOnsCost
        }
    }
}
