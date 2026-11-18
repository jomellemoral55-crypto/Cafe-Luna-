package com.example.data

import android.content.Context
import com.example.R
import com.example.model.CafeOrder
import com.example.model.CartItem
import com.example.model.MenuCategory
import com.example.model.MenuProduct
import com.example.model.OrderLineItem
import com.example.model.OrderStatus
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CafeRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- USER PROFILE ---
    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error -> handleStreamError(error, OperationType.GET, path, userId) }
        )
    }

    suspend fun ensureUserProfile(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not signed in"))
        val uid = user.uid
        val docPath = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val snapshot = docRef.get().await()
            val isAdmin = AuthManager.isBootstrappedAdmin(user)
            if (!snapshot.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to (user.displayName?.takeIf { it.isNotBlank() }?.take(80) ?: "Café Guest"),
                    "email" to (user.email ?: "").take(120),
                    "role" to if (isAdmin) "admin" else "customer",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(displayName: String, phone: String?): Result<Unit> {
        var docPath = "users/unknown"
        return try {
            val uid = requireUserId()
            val cleanName = displayName.trim().take(80)
            require(cleanName.isNotBlank()) { "Display name is required" }
            val cleanPhone = phone?.trim()?.takeIf { it.isNotBlank() }?.take(30)
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val updateMap = mutableMapOf<String, Any>(
                "displayName" to cleanName,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (cleanPhone != null) {
                updateMap["phone"] = cleanPhone
            }
            docRef.update(updateMap).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- CATEGORIES & PRODUCTS ---
    fun observeCategories(): Flow<List<MenuCategory>> = flow {
        val path = "categories"
        emitAll(
            db.collection("categories")
                .snapshots()
                .map { snap ->
                    snap.toObjects(MenuCategory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.sortOrder }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, auth.currentUser?.uid) }
        )
    }

    fun observeProducts(): Flow<List<MenuProduct>> = flow {
        val path = "products"
        emitAll(
            db.collection("products")
                .snapshots()
                .map { snap ->
                    snap.toObjects(MenuProduct::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.name }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, auth.currentUser?.uid) }
        )
    }

    suspend fun seedSampleMenuIfAdmin(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not signed in"))
        if (!AuthManager.isBootstrappedAdmin(user)) {
            return Result.failure(IllegalStateException("Admin permission required"))
        }
        return try {
            val batch = db.batch()
            SampleCatalog.defaultCategories.forEach { cat ->
                val ref = db.collection("categories").document(cat.id)
                batch.set(
                    ref,
                    mapOf(
                        "name" to cat.name,
                        "sortOrder" to cat.sortOrder,
                        "active" to cat.active,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            SampleCatalog.defaultProducts.forEach { prod ->
                val ref = db.collection("products").document(prod.id)
                batch.set(
                    ref,
                    productCreateMap(prod)
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "products")
            Result.failure(e)
        }
    }

    suspend fun saveCategory(category: MenuCategory, isNew: Boolean): Result<Unit> {
        val safeId = category.id.ifBlank { "cat_${UUID.randomUUID().toString().take(8)}" }
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val docPath = "categories/$safeId"
        return try {
            requireUserId()
            val cleanName = category.name.trim().take(40)
            require(cleanName.isNotBlank()) { "Category name required" }
            val ref = db.collection("categories").document(safeId)
            if (isNew) {
                ref.set(
                    mapOf(
                        "name" to cleanName,
                        "sortOrder" to category.sortOrder.coerceIn(0L, 1000L),
                        "active" to category.active,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.update(
                    mapOf(
                        "name" to cleanName,
                        "sortOrder" to category.sortOrder.coerceIn(0L, 1000L),
                        "active" to category.active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(categoryId: String): Result<Unit> {
        val docPath = "categories/$categoryId"
        return try {
            requireUserId()
            require(categoryId.isNotBlank())
            db.collection("categories").document(categoryId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveProduct(product: MenuProduct, isNew: Boolean): Result<Unit> {
        val safeId = product.id.ifBlank { "prod_${UUID.randomUUID().toString().take(8)}" }
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val docPath = "products/$safeId"
        return try {
            requireUserId()
            require(product.name.trim().isNotBlank()) { "Product name required" }
            require(product.pricePhp in 0.0..100000.0) { "Invalid price" }
            val ref = db.collection("products").document(safeId)
            if (isNew) {
                ref.set(productCreateMap(product)).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "name" to product.name.trim().take(80),
                    "description" to product.description.trim().take(400),
                    "categoryId" to product.categoryId.take(64),
                    "categoryName" to product.categoryName.trim().take(40),
                    "pricePhp" to product.pricePhp,
                    "imageKey" to product.imageKey.take(100),
                    "featured" to product.featured,
                    "bestSeller" to product.bestSeller,
                    "available" to product.available,
                    "sampleData" to product.sampleData,
                    "customizableDrink" to product.customizableDrink,
                    "allowedTemps" to product.allowedTemps.take(5),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                product.localImagePath?.takeIf { it.isNotBlank() }?.let {
                    updates["localImagePath"] = it.take(500)
                }
                ref.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        val docPath = "products/$productId"
        return try {
            requireUserId()
            require(productId.isNotBlank())
            db.collection("products").document(productId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private fun productCreateMap(product: MenuProduct): Map<String, Any> {
        val map = mutableMapOf<String, Any?>(
            "name" to product.name.trim().take(80),
            "description" to product.description.trim().take(400),
            "categoryId" to product.categoryId.take(64),
            "categoryName" to product.categoryName.trim().take(40),
            "pricePhp" to product.pricePhp.coerceIn(0.0, 100000.0),
            "imageKey" to product.imageKey.take(100),
            "localImagePath" to product.localImagePath?.takeIf { it.isNotBlank() }?.take(500),
            "featured" to product.featured,
            "bestSeller" to product.bestSeller,
            "available" to product.available,
            "sampleData" to product.sampleData,
            "customizableDrink" to product.customizableDrink,
            "allowedTemps" to product.allowedTemps.take(5),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return map.filterValues { it != null } as Map<String, Any>
    }

    // --- SHOPPING CART ---
    fun observeCart(userId: String): Flow<List<CartItem>> = flow {
        val path = "users/$userId/cart"
        emitAll(
            db.collection("users").document(userId).collection("cart")
                .snapshots()
                .map { snap ->
                    snap.toObjects(CartItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.seconds ?: 0L }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, userId) }
        )
    }

    suspend fun addToCart(item: CartItem): Result<Unit> {
        var docPath = "users/unknown/cart"
        return try {
            val uid = requireUserId()
            val cartId = "ci_${UUID.randomUUID().toString().take(12)}"
            val ref = db.collection("users").document(uid).collection("cart").document(cartId)
            docPath = ref.path
            val payload = mutableMapOf<String, Any?>(
                "productId" to item.productId.take(64),
                "productName" to item.productName.take(80),
                "categoryName" to item.categoryName.take(40),
                "imageKey" to item.imageKey.take(100),
                "localImagePath" to item.localImagePath?.takeIf { it.isNotBlank() }?.take(500),
                "basePricePhp" to item.basePricePhp.coerceIn(0.0, 100000.0),
                "unitPricePhp" to item.unitPricePhp.coerceIn(0.0, 100000.0),
                "quantity" to item.quantity.coerceIn(1L, 50L),
                "temperature" to item.temperature.take(20),
                "size" to item.size.take(20),
                "sugarLevel" to item.sugarLevel.take(20),
                "milkPreference" to item.milkPreference.take(30),
                "extraShots" to item.extraShots.coerceIn(0L, 5L),
                "addOns" to item.addOns.take(10),
                "customizationSummary" to item.customizationSummary.take(200),
                "customizableDrink" to item.customizableDrink,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            @Suppress("UNCHECKED_CAST")
            ref.set(payload.filterValues { it != null } as Map<String, Any>).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateCartQuantity(cartItemId: String, newQuantity: Long): Result<Unit> {
        var docPath = "users/unknown/cart/$cartItemId"
        return try {
            val uid = requireUserId()
            require(cartItemId.isNotBlank())
            val ref = db.collection("users").document(uid).collection("cart").document(cartItemId)
            docPath = ref.path
            if (newQuantity <= 0L) {
                ref.delete().await()
            } else {
                ref.update(
                    mapOf(
                        "quantity" to newQuantity.coerceIn(1L, 50L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun removeCartItem(cartItemId: String): Result<Unit> {
        var docPath = "users/unknown/cart/$cartItemId"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("cart").document(cartItemId)
            docPath = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- ORDERS ---
    fun observeUserOrders(userId: String): Flow<List<CafeOrder>> = flow {
        val path = "orders"
        emitAll(
            db.collection("orders")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CafeOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, userId) }
        )
    }

    fun observeAllOrdersForAdmin(): Flow<List<CafeOrder>> = flow {
        val path = "orders"
        emitAll(
            db.collection("orders")
                .snapshots()
                .map { snap ->
                    snap.toObjects(CafeOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, auth.currentUser?.uid) }
        )
    }

    suspend fun placeOrder(
        customerName: String,
        contactNumber: String,
        orderType: String,
        paymentMethod: String,
        notes: String?,
        cartItems: List<CartItem>,
        serviceChargePhp: Double
    ): Result<CafeOrder> {
        val orderId = "ord_${UUID.randomUUID().toString().take(12)}"
        val docPath = "orders/$orderId"
        return try {
            val uid = requireUserId()
            require(cartItems.isNotEmpty()) { "Cart cannot be empty" }
            val cleanName = customerName.trim().take(80)
            val cleanPhone = contactNumber.trim().take(25)
            require(cleanName.isNotBlank()) { "Customer name is required" }
            require(cleanPhone.length >= 4) { "Valid contact number is required" }

            val orderNumber = "CAL-${(1000..9999).random()}"
            val lineItems = cartItems.take(30).map { item ->
                OrderLineItem(
                    productId = item.productId,
                    productName = item.productName,
                    unitPrice = item.unitPricePhp,
                    quantity = item.quantity,
                    lineTotal = item.lineTotalPhp,
                    customizationSummary = item.customizationSummary,
                    imageKey = item.imageKey
                )
            }
            val subtotal = lineItems.sumOf { it.lineTotal }
            val total = subtotal + serviceChargePhp

            val itemMaps = lineItems.map { li ->
                mapOf(
                    "productId" to li.productId,
                    "productName" to li.productName,
                    "unitPrice" to li.unitPrice,
                    "quantity" to li.quantity,
                    "lineTotal" to li.lineTotal,
                    "customizationSummary" to li.customizationSummary,
                    "imageKey" to li.imageKey
                )
            }

            val orderPayload = mutableMapOf<String, Any?>(
                "orderNumber" to orderNumber,
                "userId" to uid,
                "customerName" to cleanName,
                "contactNumber" to cleanPhone,
                "orderType" to orderType,
                "paymentMethod" to paymentMethod,
                "status" to OrderStatus.RECEIVED.label,
                "items" to itemMaps,
                "subtotalPhp" to subtotal,
                "serviceChargePhp" to serviceChargePhp,
                "totalPhp" to total,
                "notes" to notes?.trim()?.takeIf { it.isNotBlank() }?.take(200),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            val orderRef = db.collection("orders").document(orderId)
            @Suppress("UNCHECKED_CAST")
            batch.set(orderRef, orderPayload.filterValues { it != null } as Map<String, Any>)

            // Clear user's cart atomically in the same batch
            cartItems.forEach { ci ->
                if (ci.id.isNotBlank()) {
                    val cartRef = db.collection("users").document(uid).collection("cart").document(ci.id)
                    batch.delete(cartRef)
                }
            }
            batch.commit().await()

            Result.success(
                CafeOrder(
                    id = orderId,
                    orderNumber = orderNumber,
                    userId = uid,
                    customerName = cleanName,
                    contactNumber = cleanPhone,
                    orderType = orderType,
                    paymentMethod = paymentMethod,
                    status = OrderStatus.RECEIVED.label,
                    items = lineItems,
                    subtotalPhp = subtotal,
                    serviceChargePhp = serviceChargePhp,
                    totalPhp = total,
                    notes = notes
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(orderId: String, newStatus: OrderStatus): Result<Unit> {
        val docPath = "orders/$orderId"
        return try {
            requireUserId()
            require(orderId.isNotBlank())
            db.collection("orders").document(orderId)
                .update(
                    mapOf(
                        "status" to newStatus.label,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    private fun handleStreamError(
        error: Throwable,
        op: OperationType,
        path: String,
        expectedUid: String?
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && (expectedUid == null || auth.currentUser?.uid == expectedUid)) {
            handleFirestoreError(exceptionToLog, op, path)
        }
        throw error
    }
}
