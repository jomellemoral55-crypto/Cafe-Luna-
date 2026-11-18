package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CafeRepository
import com.example.data.SampleCatalog
import com.example.model.CafeOrder
import com.example.model.MenuCategory
import com.example.model.MenuProduct
import com.example.model.OrderLineItem
import com.example.model.OrderStatus
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(
    private val repository: CafeRepository,
    val isCloudAdmin: Boolean
) : ViewModel() {

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    // Sandbox fallback lists when a non-admin previews the Admin Dashboard
    private val _sandboxProducts = MutableStateFlow(SampleCatalog.defaultProducts)
    private val _sandboxCategories = MutableStateFlow(SampleCatalog.defaultCategories)
    private val _sandboxOrders = MutableStateFlow(demoPreviewOrders())

    val categories: StateFlow<UiState<List<MenuCategory>>> =
        if (isCloudAdmin) {
            repository.observeCategories()
                .map { it.ifEmpty { SampleCatalog.defaultCategories } }
                .asUiStateFlow(TAG, "Failed to load categories")
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)
        } else {
            _sandboxCategories
                .map<List<MenuCategory>, UiState<List<MenuCategory>>> { UiState.Success(it) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)
        }

    val products: StateFlow<UiState<List<MenuProduct>>> =
        if (isCloudAdmin) {
            repository.observeProducts()
                .map { it.ifEmpty { SampleCatalog.defaultProducts } }
                .asUiStateFlow(TAG, "Failed to load products")
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)
        } else {
            _sandboxProducts
                .map<List<MenuProduct>, UiState<List<MenuProduct>>> { UiState.Success(it) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)
        }

    val orders: StateFlow<UiState<List<CafeOrder>>> =
        if (isCloudAdmin) {
            repository.observeAllOrdersForAdmin()
                .asUiStateFlow(TAG, "Failed to load customer orders")
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)
        } else {
            _sandboxOrders
                .map<List<CafeOrder>, UiState<List<CafeOrder>>> { UiState.Success(it) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)
        }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun seedSampleMenu() {
        if (!isCloudAdmin) {
            _sandboxCategories.value = SampleCatalog.defaultCategories
            _sandboxProducts.value = SampleCatalog.defaultProducts
            _feedbackMessage.value = "Sample menu reset in local preview"
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.seedSampleMenuIfAdmin()
            _isBusy.value = false
            _feedbackMessage.value = if (res.isSuccess) {
                "Sample menu synced to Firestore"
            } else {
                res.exceptionOrNull()?.message ?: "Failed to sync sample menu"
            }
        }
    }

    fun saveProduct(product: MenuProduct, isNew: Boolean) {
        if (!isCloudAdmin) {
            val current = _sandboxProducts.value.toMutableList()
            if (isNew) {
                current.add(0, product.copy(id = "preview_${System.currentTimeMillis()}"))
            } else {
                val idx = current.indexOfFirst { it.id == product.id }
                if (idx >= 0) current[idx] = product
            }
            _sandboxProducts.value = current
            _feedbackMessage.value = "Saved in preview mode"
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveProduct(product, isNew)
            _isBusy.value = false
            _feedbackMessage.value = if (res.isSuccess) {
                "${product.name} saved"
            } else {
                res.exceptionOrNull()?.message ?: "Failed to save product"
            }
        }
    }

    fun deleteProduct(productId: String) {
        if (!isCloudAdmin) {
            _sandboxProducts.value = _sandboxProducts.value.filterNot { it.id == productId }
            _feedbackMessage.value = "Removed in preview mode"
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.deleteProduct(productId)
            _isBusy.value = false
            _feedbackMessage.value = if (res.isSuccess) "Product deleted" else res.exceptionOrNull()?.message
        }
    }

    fun saveCategory(category: MenuCategory, isNew: Boolean) {
        if (!isCloudAdmin) {
            val current = _sandboxCategories.value.toMutableList()
            if (isNew) {
                current.add(category.copy(id = "cat_${System.currentTimeMillis()}"))
            } else {
                val idx = current.indexOfFirst { it.id == category.id }
                if (idx >= 0) current[idx] = category
            }
            _sandboxCategories.value = current
            _feedbackMessage.value = "Category saved in preview mode"
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveCategory(category, isNew)
            _isBusy.value = false
            _feedbackMessage.value = if (res.isSuccess) "Category saved" else res.exceptionOrNull()?.message
        }
    }

    fun deleteCategory(categoryId: String) {
        if (!isCloudAdmin) {
            _sandboxCategories.value = _sandboxCategories.value.filterNot { it.id == categoryId }
            return
        }
        viewModelScope.launch {
            val res = repository.deleteCategory(categoryId)
            if (res.isFailure) _feedbackMessage.value = res.exceptionOrNull()?.message
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        if (!isCloudAdmin) {
            _sandboxOrders.value = _sandboxOrders.value.map {
                if (it.id == orderId) it.copy(status = newStatus.label) else it
            }
            _feedbackMessage.value = "Status updated to ${newStatus.label} (Preview)"
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateOrderStatus(orderId, newStatus)
            _isBusy.value = false
            _feedbackMessage.value = if (res.isSuccess) {
                "Order updated to ${newStatus.label}"
            } else {
                res.exceptionOrNull()?.message ?: "Failed to update status"
            }
        }
    }

    companion object {
        private const val TAG = "AdminVM"
        private const val STOP_TIMEOUT_MS = 5000L

        private fun demoPreviewOrders(): List<CafeOrder> {
            val nowSec = Timestamp.now().seconds
            return listOf(
                CafeOrder(
                    id = "demo_ord_1",
                    orderNumber = "CAL-1048",
                    userId = "demo",
                    customerName = "Sample Guest (Preview)",
                    contactNumber = "09XX-XXX-XXXX",
                    orderType = "Pickup",
                    paymentMethod = "Cash",
                    status = OrderStatus.PREPARING.label,
                    items = listOf(
                        OrderLineItem("antonio_latte", "Antonio Latte", 155.0, 2, 310.0, "Iced • Regular (12oz)", "sample_coffee_latte")
                    ),
                    subtotalPhp = 310.0,
                    serviceChargePhp = 0.0,
                    totalPhp = 310.0,
                    createdAt = Timestamp(nowSec - 1200, 0),
                    updatedAt = Timestamp(nowSec - 600, 0)
                ),
                CafeOrder(
                    id = "demo_ord_2",
                    orderNumber = "CAL-1047",
                    userId = "demo",
                    customerName = "Sample Guest (Preview)",
                    contactNumber = "09XX-XXX-XXXX",
                    orderType = "Dine-in",
                    paymentMethod = "GCash (Simulation)",
                    status = OrderStatus.COMPLETED.label,
                    items = listOf(
                        OrderLineItem("pista_latte", "Pista Latte", 200.0, 1, 200.0, "Iced • Large (16oz)", "sample_coffee_latte"),
                        OrderLineItem("butter_croissant", "Golden Butter Croissant", 115.0, 1, 115.0, "Warmed Freshly", "sample_pastry_croissant")
                    ),
                    subtotalPhp = 315.0,
                    serviceChargePhp = 0.0,
                    totalPhp = 315.0,
                    createdAt = Timestamp(nowSec - 7200, 0),
                    updatedAt = Timestamp(nowSec - 5400, 0)
                )
            )
        }
    }
}
