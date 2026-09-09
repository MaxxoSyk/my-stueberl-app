package de.landstueberl.mystueberlapp.repository

import androidx.room.Transaction
import de.landstueberl.mystueberlapp.data.Order
import de.landstueberl.mystueberlapp.data.OrderDao
import de.landstueberl.mystueberlapp.data.ProductDao

class OrderRepository(
    private val orderDao: OrderDao,
    private val productDao: ProductDao
) {

    @Transaction
    suspend fun upsertOrderFull(order: Order): Order {

        val orderDetail = order.orderDetail
        val products = order.productList

        // 1. Upsert order itself
        val generatedOrderId = orderDao.upsertOrderDetail(orderDetail).toInt()
        val orderId = if (orderDetail.id == 0) generatedOrderId else orderDetail.id

        // 2. Get existing products in DB for this order
        val existingProducts = orderDao.getProductsInOrder(orderId)
        val existingProductIds = existingProducts.map { it.id }.toSet()

        // 3. Upsert all incoming products
        val incomingProductIds = mutableSetOf<Int>()

        for (product in products) {

            // 3a. Upsert ProductDetail
            val detail = product.details.copy(orderId = orderId)
            val newDetailId = productDao.upsertProductDetail(detail).toInt()
            val finalProductId = if (detail.id == 0) newDetailId else detail.id

            incomingProductIds.add(finalProductId)

            // 3b. Sync images for each product
            val existingImages = productDao.getImagesByProductId(finalProductId)
            val existingImageIds = existingImages.map { it.id }.toSet()

            // Upsert incoming images
            val incomingImages = product.imageList.map { img ->
                img.copy(
                    id = img.id, // keep id if editing, 0 = insert
                    productId = finalProductId
                )
            }

            val incomingImageIds = incomingImages.map { it.id }.toSet()

            // Delete removed images
            val imagesToDelete =
                existingImageIds.minus(incomingImageIds).toList()

            if (imagesToDelete.isNotEmpty()) {
                productDao.deleteImagesByIds(imagesToDelete)
            }

            // Upsert new + updated images
            productDao.upsertImages(incomingImages)
        }

        // 4. Delete Products removed from the order
        val productsToDelete = existingProductIds.minus(incomingProductIds).toList()

        if (productsToDelete.isNotEmpty()) {
            orderDao.deleteProductsByIds(productsToDelete)
        }

        // 5. Return fresh full order
        return orderDao.getOrder(orderId)
    }
}